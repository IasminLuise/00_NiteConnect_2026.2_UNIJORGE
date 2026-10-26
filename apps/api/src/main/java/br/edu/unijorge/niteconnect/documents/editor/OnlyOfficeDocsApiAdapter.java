package br.edu.unijorge.niteconnect.documents.editor;

import br.edu.unijorge.niteconnect.documents.application.DocumentEditorProvider;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.*;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public final class OnlyOfficeDocsApiAdapter implements DocumentEditorProvider {
    private static final int MAX_DOCUMENT_BYTES=8*1024*1024;
    private static final int MAX_RESPONSE_BYTES=64*1024;
    private final boolean enabled;
    private final URI server;
    private final byte[] secret;
    private final boolean insecureLoopback;
    private final Clock clock;
    private final ObjectMapper json;
    private final HttpClient http;

    public OnlyOfficeDocsApiAdapter(@Value("${nite.documents.editor.enabled:false}") boolean enabled,
            @Value("${nite.documents.editor.server-url:}") String serverUrl,
            @Value("${nite.documents.editor.jwt-secret:}") String jwtSecret,
            @Value("${nite.documents.editor.allow-insecure-loopback:false}") boolean insecureLoopback,
            Clock clock,ObjectMapper json) {
        this.enabled=enabled;this.insecureLoopback=insecureLoopback;this.clock=clock;this.json=json;
        this.secret=jwtSecret.getBytes(StandardCharsets.UTF_8);
        http=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(5)).build();
        if(enabled){
            try {
                server=URI.create(serverUrl);validateWebUri(server);
                if(secret.length<32 || server.getRawQuery()!=null || !(server.getPath().isEmpty()||server.getPath().equals("/")))throw unavailable();
            }catch(RuntimeException error){throw unavailable();}
        }else server=null;
    }

    @Override public boolean enabled(){return enabled;}
    @Override public EditorConfiguration open(String editorKey,String title,URI source,URI callback,boolean readOnly,UUID actor){
        return configuration(editorKey,title,source,callback,readOnly,false,actor);
    }
    @Override public EditorConfiguration openInspection(String editorKey,String title,URI source,URI callback,UUID actor){
        requireEnabled();
        if(!loopback(server.getHost()))throw unavailable();
        return configuration(editorKey,title,source,callback,true,true,actor);
    }
    private EditorConfiguration configuration(String editorKey,String title,URI source,URI callback,boolean readOnly,boolean inspectionExport,UUID actor){
        requireEnabled();validateWebUri(source);validateWebUri(callback);validateKey(editorKey);
        if(title==null||title.isBlank()||title.length()>255)throw ApiException.invalid("Título inválido");
        var config=new LinkedHashMap<String,Object>();
        config.put("documentType","word");
        config.put("document",Map.of("key",editorKey,"fileType","docx","title",title,"url",source.toString(),
            "permissions",Map.of("edit",!readOnly,"download",inspectionExport,"print",false,"copy",false,"review",false,"comment",false)));
        config.put("editorConfig",Map.of("callbackUrl",callback.toString(),"mode",readOnly?"view":"edit","user",Map.of("id",Objects.requireNonNull(actor).toString())));
        config.put("token",sign(config));
        return new EditorConfiguration(server.resolve("/web-apps/apps/api/documents/api.js").toString(),config);
    }

    @Override public Callback verifyCallback(String token,Map<String,Object> body){
        requireEnabled();
        try{
            if(token==null||token.length()>64*1024||body==null)throw ApiException.forbidden();
            var jwt=SignedJWT.parse(token);
            if(!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm())||!jwt.verify(new MACVerifier(secret)))throw ApiException.forbidden();
            var claims=jwt.getJWTClaimsSet();
            // Vendor callbacks may omit exp. Active key lifecycle/revocation and idempotency belong to the application.
            if(claims.getExpirationTime()!=null&&!claims.getExpirationTime().toInstant().isAfter(clock.instant()))throw ApiException.forbidden();
            if(claims.getNotBeforeTime()!=null&&claims.getNotBeforeTime().toInstant().isAfter(clock.instant()))throw ApiException.forbidden();
            if(claims.getClaim("operation")!=null&&!"callback".equals(claims.getClaim("operation")))throw ApiException.forbidden();
            Map<String,Object> signed=claims.getClaim("payload")==null?claims.getClaims():claims.getJSONObjectClaim("payload");
            for(String name:List.of("key","status","url","filetype","users"))if(!sameValue(signed.get(name),body.get(name)))throw ApiException.forbidden();
            if(!(signed.get("key") instanceof String key)||key.isBlank()||key.length()>128)throw ApiException.forbidden();
            if(!(signed.get("status") instanceof Number number))throw ApiException.forbidden();
            int status=number.intValue();
            if(number.doubleValue()!=status||!Set.of(1,2,3,4,6,7).contains(status))throw ApiException.forbidden();
            URI url=null;String filetype=null;
            if(signed.get("url")!=null){if(!(signed.get("url") instanceof String text))throw ApiException.forbidden();url=URI.create(text);validateRemote(url);}
            if(signed.get("filetype")!=null){if(!(signed.get("filetype") instanceof String text)||!"docx".equals(text))throw ApiException.forbidden();filetype=text;}
            if(Set.of(2,3,6,7).contains(status)&&url==null)throw ApiException.forbidden();
            var users=new ArrayList<UUID>();
            if(signed.get("users")!=null){
                if(!(signed.get("users") instanceof List<?> values)||values.size()>100)throw ApiException.forbidden();
                for(Object value:values){if(!(value instanceof String id))throw ApiException.forbidden();users.add(UUID.fromString(id));}
            }
            return new Callback(key,status,url,filetype,users);
        }catch(Exception error){throw ApiException.forbidden();}
    }
    private static boolean sameValue(Object left,Object right){
        if(left instanceof Number a&&right instanceof Number b)return new java.math.BigDecimal(a.toString()).compareTo(new java.math.BigDecimal(b.toString()))==0;
        return Objects.equals(left,right);
    }

    @Override public byte[] download(URI uri,int maxBytes){
        requireEnabled();validateRemote(uri);
        if(maxBytes<1||maxBytes>MAX_DOCUMENT_BYTES)throw ApiException.invalid("Limite de arquivo inválido");
        return exchange(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(20)).GET().build(),maxBytes);
    }
    @Override public byte[] convertPdf(String key,URI source){
        requireEnabled();validateKey(key);validateWebUri(source);
        var request=new LinkedHashMap<String,Object>();
        request.put("async",false);request.put("filetype","docx");request.put("key",key);request.put("outputtype","pdf");request.put("title","document.pdf");request.put("url",source.toString());
        request.put("token",sign(request));
        try{
            byte[] response=exchange(HttpRequest.newBuilder(server.resolve("/converter")).timeout(Duration.ofSeconds(30))
                .header("Content-Type","application/json").header("Accept","application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(request))).build(),MAX_RESPONSE_BYTES);
            var result=json.readTree(response);
            if(result.has("error")||!result.path("endConvert").asBoolean(false)||!result.path("fileUrl").isString())throw upstream();
            byte[] pdf=download(URI.create(result.get("fileUrl").asString()),MAX_DOCUMENT_BYTES);
            byte[] magic="%PDF-".getBytes(StandardCharsets.US_ASCII);
            if(pdf.length<magic.length||!Arrays.equals(Arrays.copyOf(pdf,magic.length),magic))throw upstream();
            return pdf;
        }catch(Exception error){throw upstream();}
    }
    private String sign(Map<String,Object> payload){
        try{
            var claims=new JWTClaimsSet.Builder().expirationTime(Date.from(clock.instant().plusSeconds(300)));
            payload.forEach(claims::claim);
            var jwt=new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),claims.build());jwt.sign(new MACSigner(secret));return jwt.serialize();
        }catch(Exception error){throw unavailable();}
    }
    private byte[] exchange(HttpRequest request,int limit){
        var responseFuture=http.sendAsync(request,info->new BoundedBody(limit));
        try{
            // A separate deadline also bounds body consumption on clients whose request timeout ends at headers.
            var response=responseFuture.get(35,TimeUnit.SECONDS);
            if(response.statusCode()!=200)throw upstream();return response.body();
        }catch(InterruptedException error){responseFuture.cancel(true);Thread.currentThread().interrupt();throw upstream();}
        catch(Exception error){responseFuture.cancel(true);throw upstream();}
    }
    private void validateRemote(URI uri){
        validateWebUri(uri);
        if(!server.getScheme().equalsIgnoreCase(uri.getScheme())||!server.getHost().equalsIgnoreCase(uri.getHost())||port(server)!=port(uri))throw ApiException.forbidden();
    }
    private void validateWebUri(URI uri){
        if(uri==null||!uri.isAbsolute()||uri.getHost()==null||uri.getRawUserInfo()!=null||uri.getRawFragment()!=null)throw ApiException.forbidden();
        if("https".equalsIgnoreCase(uri.getScheme()))return;
        if("http".equalsIgnoreCase(uri.getScheme())&&insecureLoopback&&loopback(uri.getHost()))return;
        throw ApiException.forbidden();
    }
    private static boolean loopback(String host){return "localhost".equalsIgnoreCase(host)||"127.0.0.1".equals(host)||"[::1]".equals(host)||"::1".equals(host);}
    private static int port(URI uri){return uri.getPort()!=-1?uri.getPort():"https".equalsIgnoreCase(uri.getScheme())?443:80;}
    private static void validateKey(String key){if(key==null||!key.matches("[A-Za-z0-9_.=-]{1,128}"))throw ApiException.invalid("Chave de documento inválida");}
    private void requireEnabled(){if(!enabled)throw unavailable();}
    private static ApiException unavailable(){return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Editor de documentos indisponível");}
    private static ApiException upstream(){return new ApiException(HttpStatus.BAD_GATEWAY,"Falha no provedor de documentos");}

    /** Bounds allocation while consuming the response. */
    private static final class BoundedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final int limit;
        private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> result=new CompletableFuture<>();
        private Flow.Subscription subscription;
        private BoundedBody(int limit){this.limit=limit;}
        @Override public CompletionStage<byte[]> getBody(){return result;}
        @Override public void onSubscribe(Flow.Subscription value){subscription=value;value.request(1);}
        @Override public void onNext(List<ByteBuffer> buffers){
            for(ByteBuffer buffer:buffers){
                if(buffer.remaining()>limit-bytes.size()){subscription.cancel();result.completeExceptionally(upstream());return;}
                byte[] chunk=new byte[buffer.remaining()];buffer.get(chunk);bytes.writeBytes(chunk);
            }
            subscription.request(1);
        }
        @Override public void onError(Throwable error){result.completeExceptionally(upstream());}
        @Override public void onComplete(){result.complete(bytes.toByteArray());}
    }
}
