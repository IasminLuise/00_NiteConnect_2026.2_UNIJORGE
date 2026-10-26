package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.documents.editor.OnlyOfficeDocsApiAdapter;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import tools.jackson.databind.json.JsonMapper;

class OnlyOfficeDocsApiAdapterTest {
    private static final String SECRET="synthetic-test-secret-for-hs256-and-hs384-at-least-64-bytes-long-value";
    private final Clock clock=Clock.fixed(Instant.parse("2026-10-23T12:00:00Z"),ZoneOffset.UTC);
    private HttpServer server;
    private URI origin;
    private OnlyOfficeDocsApiAdapter adapter;
    @BeforeEach void start() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        origin=URI.create("http://127.0.0.1:"+server.getAddress().getPort());
        adapter=new OnlyOfficeDocsApiAdapter(true,origin.toString(),SECRET,true,clock,JsonMapper.builder().build());
        server.start();
    }
    @AfterEach void stop(){server.stop(0);}
    private String token(Map<String,Object> body,boolean wrapped,Instant expires,JWSAlgorithm algorithm) throws Exception {
        var builder=new JWTClaimsSet.Builder();
        if(wrapped) builder.claim("payload",body); else body.forEach(builder::claim);
        if(expires!=null)builder.expirationTime(Date.from(expires));
        var jwt=new SignedJWT(new JWSHeader(algorithm),builder.build());
        jwt.sign(new MACSigner(SECRET));return jwt.serialize();
    }
    private Map<String,Object> callback(){return Map.of("key","revision-1","status",2,"url",origin+"/docx","filetype","docx");}
    @Test void acceptsRootAndWrappedVendorTokensWithoutExpiration() throws Exception {
        var body=callback();
        for(boolean wrapped:List.of(true,false))assertThat(adapter.verifyCallback(token(body,wrapped,null,JWSAlgorithm.HS256),body).key()).isEqualTo("revision-1");
    }
    @Test void rejectsTamperingExpiredWrongAlgorithmAndMismatchedBody() throws Exception {
        var body=callback();var signed=token(body,true,null,JWSAlgorithm.HS256);
        assertThatThrownBy(()->adapter.verifyCallback(signed.substring(0,signed.length()-8)+"tampered",body)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->adapter.verifyCallback(token(body,true,clock.instant().minusSeconds(1),JWSAlgorithm.HS256),body)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->adapter.verifyCallback(token(body,true,null,JWSAlgorithm.HS384),body)).isInstanceOf(RuntimeException.class);
        for(String field:List.of("key","status","url","filetype","users")){
            var changed=new HashMap<>(body);changed.put(field,field.equals("status")?6:"different");
            assertThatThrownBy(()->adapter.verifyCallback(signed,changed)).isInstanceOf(RuntimeException.class);
        }
        for(int status:List.of(0,5,8)){
            var changed=new HashMap<>(body);changed.put("status",status);
            assertThatThrownBy(()->adapter.verifyCallback(token(changed,false,null,JWSAlgorithm.HS256),changed)).isInstanceOf(RuntimeException.class);
        }
    }
    @Test void rejectsMissingStatusAndConfigurationTokenAsCallback() throws Exception {
        var missing=Map.<String,Object>of("key","revision-1");
        assertThatThrownBy(()->adapter.verifyCallback(token(missing,false,null,JWSAlgorithm.HS256),missing)).isInstanceOf(RuntimeException.class);
        var config=adapter.open("revision-1","Documento.docx",URI.create("https://api.example.test/source"),URI.create("https://api.example.test/callback"),false,UUID.randomUUID());
        assertThatThrownBy(()->adapter.verifyCallback((String)config.config().get("token"),callback())).isInstanceOf(RuntimeException.class);
    }
    @Test void extractsOnlySignedUsersAndRejectsTampering() throws Exception {
        UUID author=UUID.randomUUID();var body=new HashMap<>(callback());body.put("users",List.of(author.toString()));
        String signed=token(body,true,null,JWSAlgorithm.HS256);
        assertThat(adapter.verifyCallback(signed,body).users()).containsExactly(author);
        body.put("users",List.of(UUID.randomUUID().toString()));
        assertThatThrownBy(()->adapter.verifyCallback(signed,body)).isInstanceOf(RuntimeException.class);
    }
    @Test void configUsesStableProvidedKeySignedShortLivedAndReadOnly() throws Exception {
        UUID actor=UUID.randomUUID();
        var config=adapter.open("revision-1","Documento.docx",URI.create("https://api.example.test/source"),URI.create("https://api.example.test/callback"),true,actor);
        var jwt=SignedJWT.parse((String)config.config().get("token"));
        assertThat(jwt.verify(new com.nimbusds.jose.crypto.MACVerifier(SECRET))).isTrue();
        assertThat(jwt.getJWTClaimsSet().getExpirationTime().toInstant()).isEqualTo(clock.instant().plusSeconds(300));
        assertThat(jwt.getJWTClaimsSet().getJSONObjectClaim("document")).containsEntry("key","revision-1");
        assertThat(jwt.getJWTClaimsSet().getJSONObjectClaim("editorConfig")).containsEntry("mode","view");
        assertThat(jwt.getJWTClaimsSet().getJSONObjectClaim("editorConfig")).containsEntry("user",Map.of("id",actor.toString()));
        assertThat(config.toString()).doesNotContain((String)config.config().get("token"));
        assertThat(adapter.verifyCallback(token(callback(),true,null,JWSAlgorithm.HS256),callback()).toString()).doesNotContain(origin+"/docx");
    }
    @Test void downloadIsBoundedAndRejectsRedirectsCredentialsOtherOrigins() {
        server.createContext("/ok",exchange->{byte[] data="12345678".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,data.length);exchange.getResponseBody().write(data);exchange.close();});
        server.createContext("/redirect",exchange->{exchange.getResponseHeaders().set("Location",origin+"/ok");exchange.sendResponseHeaders(302,-1);exchange.close();});
        assertThat(adapter.download(origin.resolve("/ok"),8)).hasSize(8);
        assertThatThrownBy(()->adapter.download(origin.resolve("/ok"),7)).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->adapter.download(origin.resolve("/redirect"),20)).isInstanceOf(RuntimeException.class);
        for(String url:List.of("http://127.0.0.1:1/docx","https://example.test/docx","file:///tmp/docx","http://user@127.0.0.1:"+origin.getPort()+"/ok"))
            assertThatThrownBy(()->adapter.download(URI.create(url),20)).isInstanceOf(RuntimeException.class);
    }
    @Test void convertsUsingSignedRequestAndValidatesPdf() {
        server.createContext("/pdf",exchange->{byte[] data="%PDF-1.7\nsynthetic".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,data.length);exchange.getResponseBody().write(data);exchange.close();});
        server.createContext("/converter",exchange->{
            var json=JsonMapper.builder().build();var body=json.readTree(exchange.getRequestBody());
            assertThat(body.get("outputtype").asString()).isEqualTo("pdf");
            try {var jwt=SignedJWT.parse(body.get("token").asString());assertThat(jwt.verify(new com.nimbusds.jose.crypto.MACVerifier(SECRET))).isTrue();}
            catch(Exception error){throw new IllegalStateException(error);}
            byte[] data=("{\"endConvert\":true,\"fileUrl\":\""+origin+"/pdf\",\"fileType\":\"pdf\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,data.length);exchange.getResponseBody().write(data);exchange.close();});
        assertThat(adapter.convertPdf("revision-1",URI.create("https://api.example.test/source"))).startsWith("%PDF-".getBytes(StandardCharsets.US_ASCII));
    }
    @Test void conversionRejectsUpstreamErrorAndNonPdf() {
        server.createContext("/converter",exchange->{byte[] data="{\"error\":-3}".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,data.length);exchange.getResponseBody().write(data);exchange.close();});
        assertThatThrownBy(()->adapter.convertPdf("revision-1",URI.create("https://api.example.test/source"))).isInstanceOf(RuntimeException.class);
        server.removeContext("/converter");
        server.createContext("/converter",exchange->{byte[] data=("{\"endConvert\":true,\"fileUrl\":\""+origin+"/bad\"}").getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,data.length);exchange.getResponseBody().write(data);exchange.close();});
        server.createContext("/bad",exchange->{byte[] data="not pdf".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,data.length);exchange.getResponseBody().write(data);exchange.close();});
        assertThatThrownBy(()->adapter.convertPdf("revision-1",URI.create("https://api.example.test/source"))).isInstanceOf(RuntimeException.class);
    }
    @Test void conversionRejectsOversizedResponsesAndForeignDownloadOrigins() {
        server.createContext("/converter",exchange->{byte[] data=("{\"padding\":\""+"x".repeat(65536)+"\"}").getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,data.length);exchange.getResponseBody().write(data);exchange.close();});
        assertThatThrownBy(()->adapter.convertPdf("revision-1",URI.create("https://api.example.test/source"))).isInstanceOf(RuntimeException.class);
        server.removeContext("/converter");
        server.createContext("/converter",exchange->{byte[] data="{\"endConvert\":true,\"fileUrl\":\"https://example.test/secret.pdf\"}".getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,data.length);exchange.getResponseBody().write(data);exchange.close();});
        assertThatThrownBy(()->adapter.convertPdf("revision-1",URI.create("https://api.example.test/source"))).isInstanceOf(RuntimeException.class);
    }
    @Test void oversizedConvertedPdfIsAnUpstreamFailureBeforeStorage() {
        server.createContext("/oversized-pdf",exchange->{
            byte[] data=new byte[8*1024*1024+1];
            byte[] magic="%PDF-".getBytes(StandardCharsets.US_ASCII);System.arraycopy(magic,0,data,0,magic.length);
            exchange.sendResponseHeaders(200,data.length);
            try(var stream=exchange.getResponseBody()){stream.write(data);}finally{exchange.close();}
        });
        server.createContext("/converter",exchange->{
            byte[] data=("{\"endConvert\":true,\"fileUrl\":\""+origin+"/oversized-pdf\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,data.length);exchange.getResponseBody().write(data);exchange.close();
        });
        assertThatThrownBy(()->adapter.convertPdf("revision-1",URI.create("https://api.example.test/source")))
            .isInstanceOfSatisfying(br.edu.unijorge.niteconnect.shared.api.ApiException.class,
                error->assertThat(error.status()).isEqualTo(org.springframework.http.HttpStatus.BAD_GATEWAY));
    }
    @Test void disabledAndInsecureConfigurationFailClosed() {
        assertThat(new OnlyOfficeDocsApiAdapter(false,"","",false,clock,JsonMapper.builder().build()).enabled()).isFalse();
        assertThatThrownBy(()->new OnlyOfficeDocsApiAdapter(true,origin.toString(),SECRET,false,clock,JsonMapper.builder().build())).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(()->new OnlyOfficeDocsApiAdapter(true,"http://example.test",SECRET,true,clock,JsonMapper.builder().build())).isInstanceOf(RuntimeException.class);
    }
}
