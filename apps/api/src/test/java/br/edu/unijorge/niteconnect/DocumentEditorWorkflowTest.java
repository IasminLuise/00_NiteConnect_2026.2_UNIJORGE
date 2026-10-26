package br.edu.unijorge.niteconnect;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Synthetic local provider exercising the actual protocol adapter, HTTP security and persistence. */
class DocumentEditorWorkflowTest extends ActivityTestSupport {
    private static final String JWT_SECRET="synthetic-workflow-jwt-secret-at-least-32-bytes";
    private static final String CAPABILITY_SECRET="synthetic-workflow-capability-secret-at-least-32-bytes";
    private static final byte[] PDF="%PDF-1.7\nsynthetic local fixture".getBytes(StandardCharsets.US_ASCII);
    private static final HttpServer server;
    private static final URI origin;
    private static final Path directory;
    private static final AtomicReference<byte[]> candidate=new AtomicReference<>();
    private static final AtomicBoolean downloadFailure=new AtomicBoolean();
    private static final AtomicBoolean conversionFailure=new AtomicBoolean();
    private static final AtomicReference<URI> conversionSource=new AtomicReference<>();
    static {
        try {
            directory=Files.createTempDirectory("nite-editor-workflow-");
            server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            origin=URI.create("http://127.0.0.1:"+server.getAddress().getPort());
            server.createContext("/candidate",exchange->{
                if(downloadFailure.get()){exchange.sendResponseHeaders(503,-1);exchange.close();return;}
                byte[] data=candidate.get();exchange.sendResponseHeaders(200,data.length);exchange.getResponseBody().write(data);exchange.close();
            });
            server.createContext("/pdf",exchange->{exchange.sendResponseHeaders(200,PDF.length);exchange.getResponseBody().write(PDF);exchange.close();});
            server.createContext("/converter",exchange->{
                var json=JsonMapper.builder().build();var body=json.readTree(exchange.getRequestBody());
                conversionSource.set(URI.create(body.get("url").asString()));
                byte[] data=(conversionFailure.get()?"{\"error\":-3}":"{\"endConvert\":true,\"fileType\":\"pdf\",\"fileUrl\":\""+origin+"/pdf\"}").getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200,data.length);exchange.getResponseBody().write(data);exchange.close();
            });
            server.start();
        }catch(IOException error){throw new ExceptionInInitializerError(error);}
    }
    @DynamicPropertySource static void editorProperties(DynamicPropertyRegistry registry){
        registry.add("nite.documents.local-development",()->true);
        registry.add("nite.documents.storage-root",directory::toString);
        registry.add("nite.documents.editor.enabled",()->true);
        registry.add("nite.documents.editor.server-url",origin::toString);
        registry.add("nite.documents.editor.jwt-secret",()->JWT_SECRET);
        registry.add("nite.documents.editor.allow-insecure-loopback",()->true);
        registry.add("nite.documents.capability-secret",()->CAPABILITY_SECRET);
        registry.add("nite.documents.public-base-url",()->"http://127.0.0.1:18081");
    }
    @BeforeEach void resetProvider() throws Exception {
        candidate.set(DocumentCoreTest.fixture("Editado","24"));downloadFailure.set(false);conversionFailure.set(false);conversionSource.set(null);
    }
    @AfterAll static void stopProvider(){server.stop(0);}
    private final tools.jackson.databind.ObjectMapper json=JsonMapper.builder().build();
    private byte[] originalBytes;
    private JsonNode postAs(UUID actor,String path,Object body,int expected) throws Exception {
        String result=mvc.perform(post(path).with(token(actor)).contentType("application/json").content(json.writeValueAsString(body)))
            .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return result.isBlank()?json.createObjectNode():json.readTree(result);
    }
    private UUID id(JsonNode node){return UUID.fromString(node.get("id").asString());}
    private JsonNode document() throws Exception {
        originalBytes=DocumentCoreTest.fixture("Original","24");
        JsonNode template=postAs(ADMIN,"/api/v1/document-templates",Map.of("title","Modelo sintético","official",false,"reason","Fixture"),201);
        String version=mvc.perform(multipart("/api/v1/document-templates/"+id(template)+"/versions")
            .file(new MockMultipartFile("file","fixture.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",originalBytes))
            .param("expectedVersion","0").param("reason","Fixture").with(token(ADMIN))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return postAs(ADMIN,"/api/v1/documents",Map.of("title","Documento sintético","context","PERSONAL","ownerId",MEMBER,"templateVersionId",id(json.readTree(version)),"reason","Fixture"),201);
    }
    private long revision(UUID doc){return jdbc.queryForObject("select version from nite_connect.documents where id=?",Long.class,doc);}
    private UUID current(UUID doc){return jdbc.queryForObject("select current_version_id from nite_connect.documents where id=?",UUID.class,doc);}
    private int versions(UUID doc){return jdbc.queryForObject("select count(*) from nite_connect.document_versions where document_id=?",Integer.class,doc);}
    private JsonNode open(UUID actor,UUID doc) throws Exception {return postAs(actor,"/api/v1/documents/"+doc+"/editor-sessions",Map.of("expectedVersion",revision(doc),"readOnly",false),200);}
    private URI source(JsonNode session){return URI.create(session.path("editor").path("config").path("document").path("url").asString());}
    private Map<String,Object> callback(JsonNode session,int state){
        var body=new LinkedHashMap<String,Object>();body.put("key",session.path("editor").path("config").path("document").path("key").asString());body.put("status",state);
        if(Set.of(2,3,6,7).contains(state)){body.put("url",origin+"/candidate");body.put("filetype","docx");}
        body.put("users",List.of(MEMBER.toString()));return body;
    }
    private String signed(Map<String,Object> body) throws Exception {
        var jwt=new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),new JWTClaimsSet.Builder().claim("payload",body).expirationTime(Date.from(Instant.now().plusSeconds(300))).build());
        jwt.sign(new MACSigner(JWT_SECRET));return jwt.serialize();
    }
    private String callbackPath(JsonNode session){return "/api/v1/document-provider/cycles/"+id(session)+"/callback";}
    private void send(JsonNode session,Map<String,Object> body,int expected) throws Exception {
        mvc.perform(post(callbackPath(session)).header("Authorization","Bearer "+signed(body)).contentType("application/json").content(json.writeValueAsString(body)))
            .andExpect(status().is(expected));
    }
    @Test void collaboratorsShareCycleAndCapabilitiesRejectTamperingExpiryAndRevocation() throws Exception {
        UUID doc=id(document());var initial=open(MEMBER,doc);
        postAs(ADMIN,"/api/v1/documents/"+doc+"/grants",Map.of("memberId",OTHER,"canEdit",true,"expectedVersion",revision(doc),"reason","Colaboração"),200);
        mvc.perform(get(source(initial))).andExpect(status().isForbidden());
        var member=open(MEMBER,doc);var other=open(OTHER,doc);
        assertThat(id(member)).isEqualTo(id(other)).isNotEqualTo(id(initial));
        assertThat(member.path("editor").path("config").path("document").path("key").asString()).isEqualTo(other.path("editor").path("config").path("document").path("key").asString());
        mvc.perform(get(source(member))).andExpect(status().isOk()).andExpect(content().bytes(originalBytes));
        mvc.perform(get(URI.create(source(member)+"tampered"))).andExpect(status().isForbidden());
        UUID grant=UUID.fromString(source(member).getPath().substring(source(member).getPath().lastIndexOf('/')+1));
        jdbc.update("update nite_connect.document_source_grants set expires_at=now()-interval '1 minute' where id=?",grant);
        var row=jdbc.queryForMap("select version_id,actor_id,expires_at from nite_connect.document_source_grants where id=?",grant);
        Instant expires=((java.sql.Timestamp)row.get("expires_at")).toInstant();
        Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(CAPABILITY_SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        String expiredToken=Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(("source|"+grant+"|"+row.get("version_id")+"|"+row.get("actor_id")+"|"+expires.getEpochSecond()).getBytes(StandardCharsets.UTF_8)));
        mvc.perform(get(URI.create("/api/v1/document-provider/sources/"+grant+"?token="+expiredToken))).andExpect(status().isForbidden());
        postAs(MEMBER,"/api/v1/documents/"+doc+"/editor-sessions/"+id(member)+"/close",Map.of(),204);
        mvc.perform(get(source(other))).andExpect(status().isForbidden());
    }
    @Test void signedSavesPreserveVersionsAreIdempotentAndFinalStatusClosesCycle() throws Exception {
        UUID doc=id(document());UUID original=current(doc);var session=open(MEMBER,doc);var forceSave=callback(session,6);
        send(session,forceSave,200);UUID intermediate=current(doc);assertThat(intermediate).isNotEqualTo(original);assertThat(versions(doc)).isEqualTo(2);
        send(session,forceSave,200);assertThat(versions(doc)).isEqualTo(2);assertThat(current(doc)).isEqualTo(intermediate);
        candidate.set(DocumentCoreTest.fixture("Final","24"));send(session,callback(session,2),200);
        assertThat(versions(doc)).isEqualTo(3);UUID finalVersion=current(doc);assertThat(finalVersion).isNotEqualTo(intermediate);
        assertThat(jdbc.queryForObject("select status from nite_connect.document_cycles where id=?",String.class,id(session))).isEqualTo("CLOSED");
        send(session,callback(session,2),200);assertThat(versions(doc)).isEqualTo(3);assertThat(current(doc)).isEqualTo(finalVersion);
        mvc.perform(get("/api/v1/documents/"+doc+"/versions/"+original+"/download").with(token(MEMBER)))
            .andExpect(status().isOk()).andExpect(content().bytes(originalBytes));
    }
    @Test void protectedFontIsQuarantinedAndFailedDownloadCanRetryWithoutLosingCurrent() throws Exception {
        UUID doc=id(document());UUID original=current(doc);var session=open(MEMBER,doc);
        downloadFailure.set(true);send(session,callback(session,6),502);assertThat(versions(doc)).isEqualTo(1);assertThat(current(doc)).isEqualTo(original);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.document_callback_receipts where cycle_id=?",Integer.class,id(session))).isZero();
        downloadFailure.set(false);candidate.set(DocumentCoreTest.fixture("Editado","20"));send(session,callback(session,6),200);
        assertThat(versions(doc)).isEqualTo(2);assertThat(current(doc)).isEqualTo(original);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.document_versions where document_id=? and status='QUARANTINED'",Integer.class,doc)).isEqualTo(1);
    }
    @Test void callbackNeedsAuthenticSignatureAndCurrentActorAuthorization() throws Exception {
        UUID doc=id(document());var session=open(MEMBER,doc);var body=callback(session,6);
        mvc.perform(post(callbackPath(session)).contentType("application/json").content(json.writeValueAsString(body))).andExpect(status().isForbidden());
        for(String malformed:List.of("{","null"))
            mvc.perform(post(callbackPath(session)).header("Authorization","Bearer "+signed(body)).contentType("application/json").content(malformed)).andExpect(status().isBadRequest());
        String signature=signed(body);var tampered=new LinkedHashMap<>(body);tampered.put("users",List.of(OTHER.toString()));
        mvc.perform(post(callbackPath(session)).header("Authorization","Bearer "+signature).contentType("application/json").content(json.writeValueAsString(tampered))).andExpect(status().isForbidden());
        send(session,tampered,403);assertThat(versions(doc)).isEqualTo(1);
        jdbc.update("update nite_connect.members set status='AFASTADO' where id=?",MEMBER);send(session,body,403);assertThat(versions(doc)).isEqualTo(1);
    }
    @Test void informationalAndErrorStatusesNeverPublishCandidateBytes() throws Exception {
        for(int state:List.of(1,4,3,7)){
            UUID doc=id(document());UUID original=current(doc);var session=open(MEMBER,doc);send(session,callback(session,state),200);
            assertThat(versions(doc)).as("status %s",state).isEqualTo(1);assertThat(current(doc)).isEqualTo(original);
            String expected=state==1?"OPEN":state==4?"CLOSED":"FAILED";
            assertThat(jdbc.queryForObject("select status from nite_connect.document_cycles where id=?",String.class,id(session))).isEqualTo(expected);
        }
    }
    @Test void pdfPinsRequestedImmutableVersionAndConversionFailureCreatesNoArtifact() throws Exception {
        UUID doc=id(document());UUID original=current(doc);var session=open(MEMBER,doc);send(session,callback(session,6),200);UUID latest=current(doc);
        mvc.perform(post("/api/v1/documents/"+doc+"/versions/"+original+"/pdf").with(token(MEMBER)))
            .andExpect(status().isOk()).andExpect(header().string("X-Document-Version",original.toString())).andExpect(content().contentType("application/pdf")).andExpect(content().bytes(PDF));
        assertThat(conversionSource.get()).isNotNull();
        mvc.perform(get(conversionSource.get())).andExpect(status().isOk()).andExpect(content().bytes(originalBytes));
        conversionFailure.set(true);
        mvc.perform(post("/api/v1/documents/"+doc+"/versions/"+latest+"/pdf").with(token(MEMBER))).andExpect(status().isBadGateway());
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.document_pdfs where version_id=?",Integer.class,latest)).isZero();
        assertThat(current(doc)).isEqualTo(latest);assertThat(versions(doc)).isEqualTo(2);
    }
}
