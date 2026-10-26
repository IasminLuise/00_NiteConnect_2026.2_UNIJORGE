package br.edu.unijorge.niteconnect;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import java.net.URI;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Local-only authorized inspection; the normal editor and quarantine remain closed. */
class DocumentInspectionWorkflowTest extends ActivityTestSupport {
    private static final String SECRET="synthetic-inspection-provider-jwt-at-least-32-bytes";
    private static final Path STORAGE;
    static {try { STORAGE=Files.createTempDirectory("nite-inspection-test-"); }
        catch(java.io.IOException error){throw new ExceptionInInitializerError(error);} }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r){
        r.add("nite.documents.local-development",()->true);
        r.add("nite.documents.inspection.enabled",()->true);
        r.add("server.address",()->"127.0.0.1");
        r.add("nite.documents.storage-root",STORAGE::toString);
        r.add("nite.documents.public-base-url",()->"http://127.0.0.1:18083");
        r.add("nite.documents.capability-secret",()->"synthetic-inspection-capability-at-least-32-bytes");
        r.add("nite.documents.editor.enabled",()->true);
        r.add("nite.documents.editor.server-url",()->"http://127.0.0.1:18089");
        r.add("nite.documents.editor.jwt-secret",()->SECRET);
        r.add("nite.documents.editor.allow-insecure-loopback",()->true);
    }
    private final tools.jackson.databind.ObjectMapper json=JsonMapper.builder().build();
    private byte[] quarantinedBytes;
    private JsonNode postAs(UUID actor,String path,Object body,int code)throws Exception{
        var response=mvc.perform(post(path).with(token(actor)).contentType("application/json")
                .content(json.writeValueAsString(body))).andExpect(status().is(code)).andReturn().getResponse();
        return response.getContentAsString().isBlank()?json.createObjectNode():json.readTree(response.getContentAsString());
    }
    private UUID id(JsonNode n){return UUID.fromString(n.get("id").asString());}
    private JsonNode document(boolean official)throws Exception{
        var template=postAs(ADMIN,"/api/v1/document-templates",Map.of("title","Fixture privada","official",official,"reason","Teste"),201);
        String uploaded=mvc.perform(multipart("/api/v1/document-templates/"+id(template)+"/versions")
                .file(new MockMultipartFile("file","fixture.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",DocumentCoreTest.fixture("Original","24")))
                .param("expectedVersion","0").param("reason","Teste").with(token(ADMIN)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return postAs(ADMIN,"/api/v1/documents",Map.of("title","Cópia privada","context","PERSONAL","ownerId",MEMBER,"templateVersionId",id(json.readTree(uploaded)),"reason","Teste"),201);
    }
    private UUID quarantined(UUID document)throws Exception{
        quarantinedBytes=DocumentCoreTest.fixture("Original","36");
        String result=mvc.perform(multipart("/api/v1/documents/"+document+"/versions")
                .file(new MockMultipartFile("file","candidate.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",quarantinedBytes))
                .param("expectedVersion","0").param("reason","Controle negativo").with(token(ADMIN)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var value=json.readTree(result);assertThat(value.get("status").asString()).isEqualTo("QUARANTINED");return id(value);
    }
    private JsonNode open(UUID actor,UUID document,UUID version,int code)throws Exception{
        return postAs(actor,"/api/v1/documents/"+document+"/inspection-sessions",
                Map.of("versionId",version,"reason","Inspeção privada autorizada"),code);
    }
    private URI source(JsonNode n){return URI.create(n.path("editor").path("config").path("document").path("url").asString());}
    private String signed(Map<String,Object> body)throws Exception{
        var jwt=new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),new JWTClaimsSet.Builder()
                .claim("payload",body).expirationTime(Date.from(Instant.now().plusSeconds(300))).build());
        jwt.sign(new MACSigner(SECRET));return jwt.serialize();
    }
    @Test void administratorInspectsQuarantineReadOnlyWithoutChangingNormalPermissionsOrHead()throws Exception{
        var doc=document(false);UUID document=id(doc),head=UUID.fromString(doc.get("currentVersionId").asString());
        UUID version=quarantined(document);long revision=jdbc.queryForObject("select version from nite_connect.documents where id=?",Long.class,document);
        var inspection=open(ADMIN,document,version,200);
        var config=inspection.path("editor").path("config");
        assertThat(config.path("editorConfig").path("mode").asString()).isEqualTo("view");
        assertThat(config.path("document").path("permissions").path("edit").asBoolean()).isFalse();
        assertThat(config.path("document").path("permissions").path("download").asBoolean()).isTrue();
        assertThat(config.path("document").path("permissions").path("copy").asBoolean()).isFalse();
        mvc.perform(get(source(inspection))).andExpect(status().isOk())
                .andExpect(content().bytes(quarantinedBytes));
        var normal=postAs(ADMIN,"/api/v1/documents/"+document+"/editor-sessions",Map.of("expectedVersion",revision,"readOnly",false),200);
        assertThat(normal.path("editor").path("config").path("document").path("permissions").path("download").asBoolean()).isFalse();
        assertThat(jdbc.queryForObject("select current_version_id from nite_connect.documents where id=?",UUID.class,document)).isEqualTo(head);
        assertThat(jdbc.queryForObject("select status from nite_connect.document_versions where id=?",String.class,version)).isEqualTo("QUARANTINED");
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.document_events where document_id=? and action='INSPECTION_OPENED'",Integer.class,document)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.audit_events where entity_id=? and action='INSPECTION_SOURCE_READ'",Integer.class,document)).isEqualTo(1);
    }
    @Test void memberAndAnonymousCannotOpenAnInspection()throws Exception{
        var d=document(false);UUID document=id(d),version=UUID.fromString(d.get("currentVersionId").asString());
        open(MEMBER,document,version,403);
        mvc.perform(post("/api/v1/documents/"+document+"/inspection-sessions").contentType("application/json")
                .content(json.writeValueAsString(Map.of("versionId",version,"reason","Teste")))).andExpect(status().isUnauthorized());
    }
    @Test void versionFromAnotherDocumentAndOfficialProductTemplatesRemainDenied()throws Exception{
        var a=document(false);var b=document(false);
        open(ADMIN,id(a),UUID.fromString(b.get("currentVersionId").asString()),404);
        var official=document(true);open(ADMIN,id(official),UUID.fromString(official.get("currentVersionId").asString()),409);
    }
    @Test void capabilityExpiresRevokesAndRechecksAdministratorRole()throws Exception{
        var d=document(false);UUID document=id(d),version=UUID.fromString(d.get("currentVersionId").asString());
        var revoked=open(ADMIN,document,version,200);URI url=source(revoked);
        mvc.perform(get(URI.create(url.toString()+"x"))).andExpect(status().isForbidden());
        postAs(ADMIN,"/api/v1/documents/"+document+"/inspection-sessions/"+id(revoked)+"/close",Map.of(),204);
        mvc.perform(get(url)).andExpect(status().isForbidden());
        var expired=open(ADMIN,document,version,200);
        jdbc.update("update nite_connect.document_inspection_sessions set expires_at=clock_timestamp()-interval '1 second' where id=?",id(expired));
        mvc.perform(get(source(expired))).andExpect(status().isForbidden());
        var role=open(ADMIN,document,version,200);
        jdbc.update("delete from nite_connect.role_assignments where member_id=?",ADMIN);
        mvc.perform(get(source(role))).andExpect(status().isForbidden());
    }
    @Test void inspectionCallbackIsSignedReadOnlyAndIdempotent()throws Exception{
        var d=document(false);UUID document=id(d),version=UUID.fromString(d.get("currentVersionId").asString());
        var session=open(ADMIN,document,version,200);String url="/api/v1/document-provider/inspection-cycles/"+id(session)+"/callback";
        String key=session.path("editor").path("config").path("document").path("key").asString();
        Map<String,Object> closed=Map.of("key",key,"status",4,"users",List.of(ADMIN.toString()));
        mvc.perform(post(url).contentType("application/json").content(json.writeValueAsString(closed))).andExpect(status().isForbidden());
        Map<String,Object> write=Map.of("key",key,"status",2,"url","http://127.0.0.1:18089/candidate","filetype","docx","users",List.of(ADMIN.toString()));
        mvc.perform(post(url).header("Authorization","Bearer "+signed(write)).contentType("application/json").content(json.writeValueAsString(write))).andExpect(status().isForbidden());
        for(int i=0;i<2;i++)mvc.perform(post(url).header("Authorization","Bearer "+signed(closed)).contentType("application/json").content(json.writeValueAsString(closed))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.document_versions where document_id=?",Integer.class,document)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.document_events where document_id=? and action='INSPECTION_STATUS_4'",Integer.class,document)).isEqualTo(1);
        mvc.perform(get(source(session))).andExpect(status().isForbidden());
    }
    @Test void importedSnapshotIsAlwaysQuarantinedAndDoesNotMoveHead()throws Exception{
        var d=document(false);UUID document=id(d),head=UUID.fromString(d.get("currentVersionId").asString());
        byte[] bytes=DocumentCoreTest.fixture("Original","24");
        String response=mvc.perform(multipart("/api/v1/documents/"+document+"/inspection-samples")
                .file(new MockMultipartFile("file","snapshot.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",bytes))
                .param("basedOnVersionId",head.toString()).param("expectedVersion","0").param("reason","Snapshot do motor para reabertura privada")
                .with(token(ADMIN))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var candidate=json.readTree(response);assertThat(candidate.get("status").asString()).isEqualTo("QUARANTINED");
        assertThat(candidate.get("rejectionCode").asString()).isEqualTo("INSPECTION_ONLY");
        var session=open(ADMIN,document,id(candidate),200);
        mvc.perform(get(source(session))).andExpect(status().isOk()).andExpect(content().bytes(bytes));
        assertThat(jdbc.queryForObject("select current_version_id from nite_connect.documents where id=?",UUID.class,document)).isEqualTo(head);
    }
    @Test void unsafeQuarantinedBytesCannotBeSentToTheInspectionEngine()throws Exception{
        var d=document(false);UUID document=id(d);
        String response=mvc.perform(multipart("/api/v1/documents/"+document+"/versions")
                .file(new MockMultipartFile("file","invalid.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",new byte[]{1,2,3}))
                .param("expectedVersion","0").param("reason","Controle de segurança").with(token(ADMIN)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        open(ADMIN,document,id(json.readTree(response)),400);
    }

}
