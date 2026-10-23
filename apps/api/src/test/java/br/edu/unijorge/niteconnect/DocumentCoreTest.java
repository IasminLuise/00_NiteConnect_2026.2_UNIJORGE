package br.edu.unijorge.niteconnect;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

class DocumentCoreTest extends ActivityTestSupport {
    static final Path directory;
    static { try { directory=Files.createTempDirectory("nite-doc-test-"); } catch(IOException e){throw new ExceptionInInitializerError(e);} }
    @DynamicPropertySource static void documents(DynamicPropertyRegistry r) {
        r.add("nite.documents.storage-root",directory::toString);r.add("nite.documents.local-development",()->true);
    }
    final tools.jackson.databind.ObjectMapper json=JsonMapper.builder().build();
    JsonNode postData(UUID actor,String path,Object body,int code) throws Exception {
        var result=mvc.perform(post(path).with(token(actor)).contentType("application/json").content(json.writeValueAsString(body)))
            .andExpect(status().is(code)).andReturn().getResponse().getContentAsString();
        return result.isEmpty()?json.createObjectNode():json.readTree(result);
    }
    static byte[] fixture(String text,String size) throws IOException {
        var out=new ByteArrayOutputStream();
        try(var zip=new ZipOutputStream(out)){
            var parts=Map.of("[Content_Types].xml","<Types xmlns='http://schemas.openxmlformats.org/package/2006/content-types'><Default Extension='xml' ContentType='application/xml'/><Override PartName='/word/document.xml' ContentType='application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml'/></Types>",
                "_rels/.rels","<Relationships xmlns='http://schemas.openxmlformats.org/package/2006/relationships'><Relationship Id='rId1' Type='http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument' Target='word/document.xml'/></Relationships>",
                "word/document.xml","<w:document xmlns:w='http://schemas.openxmlformats.org/wordprocessingml/2006/main'><w:body><w:p><w:r><w:rPr><w:sz w:val='"+size+"'/></w:rPr><w:t>"+text+"</w:t></w:r></w:p></w:body></w:document>");
            for(var e:new TreeMap<>(parts).entrySet()){zip.putNextEntry(new ZipEntry(e.getKey()));zip.write(e.getValue().getBytes(StandardCharsets.UTF_8));zip.closeEntry();}
        }return out.toByteArray();
    }
    UUID template(boolean official) throws Exception {
        var t=postData(ADMIN,"/api/v1/document-templates",Map.of("title","Sintético","official",official,"reason","Fixture"),201);
        var result=mvc.perform(multipart("/api/v1/document-templates/"+t.get("id").asString()+"/versions")
            .file(new MockMultipartFile("file","fixture.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",fixture("Original","24")))
            .param("expectedVersion","0").param("reason","Fixture").with(token(ADMIN))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(result).get("id").asString());
    }
    JsonNode document(boolean official) throws Exception {
        return postData(ADMIN,"/api/v1/documents",Map.of("title","Documento sintético","context","PERSONAL","ownerId",MEMBER,"templateVersionId",template(official),"reason","Atribuição"),201);
    }
    JsonNode upload(UUID actor,UUID doc,long version,byte[] bytes) throws Exception {
        var body=mvc.perform(multipart("/api/v1/documents/"+doc+"/versions")
            .file(new MockMultipartFile("file","candidate.docx","application/vnd.openxmlformats-officedocument.wordprocessingml.document",bytes))
            .param("expectedVersion",Long.toString(version)).param("reason","Fixture").with(token(actor)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }
    @Test void assignedPersonalDocumentDeniesColleagueAndPreservesVersions() throws Exception {
        var d=document(false);var id=UUID.fromString(d.get("id").asString());var v=d.get("currentVersionId").asString();
        mvc.perform(get("/api/v1/documents/"+id).with(token(MEMBER))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/documents/"+id).with(token(OTHER))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/documents/"+id+"/versions/"+v+"/download").with(token(OTHER))).andExpect(status().isForbidden());
        var candidate=upload(MEMBER,id,0,fixture("Editado","24"));assertThat(candidate.get("status").asString()).isEqualTo("AVAILABLE");
        var original=mvc.perform(get("/api/v1/documents/"+id+"/versions/"+v+"/download").with(token(MEMBER))).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(original,StandardCharsets.ISO_8859_1)).isNotEmpty();
        postData(ADMIN,"/api/v1/documents/"+id+"/restore",Map.of("expectedVersion",1,"versionId",v,"reason","Recuperação","confirmed",true),200);
        assertThat(jdbc.queryForObject("select current_version_id from nite_connect.documents where id=?",UUID.class,id)).isEqualTo(UUID.fromString(v));
    }
    @Test void formatRegressionIsQuarantinedAndCurrentBytesRemain() throws Exception {
        var d=document(false);var id=UUID.fromString(d.get("id").asString());var v=d.get("currentVersionId").asString();
        var rejected=upload(MEMBER,id,0,fixture("Original","20"));assertThat(rejected.get("status").asString()).isEqualTo("QUARANTINED");
        assertThat(jdbc.queryForObject("select current_version_id from nite_connect.documents where id=?",UUID.class,id)).isEqualTo(UUID.fromString(v));
        assertThatThrownBy(()->jdbc.update("update nite_connect.document_versions set size=1 where id=?",UUID.fromString(v))).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
    @Test void inactiveMemberAndStaleRevisionCannotWrite() throws Exception {
        var d=document(false);var id=UUID.fromString(d.get("id").asString());
        upload(MEMBER,id,0,fixture("Editado","24"));
        mvc.perform(multipart("/api/v1/documents/"+id+"/versions").file(new MockMultipartFile("file",fixture("Antigo","24"))).param("expectedVersion","0").param("reason","Fixture").with(token(MEMBER))).andExpect(status().isConflict());
        jdbc.update("update nite_connect.members set status='AFASTADO' where id=?",MEMBER);
        mvc.perform(get("/api/v1/documents/"+id).with(token(MEMBER))).andExpect(status().isForbidden());
    }
    @Test void projectLeadershipDoesNotOpenPersonalDocumentAndOnlyAdminCreatesModels() throws Exception {
        activeProject();var d=document(false);
        mvc.perform(get("/api/v1/documents/"+d.get("id").asString()).with(token(LEADER))).andExpect(status().isForbidden());
        postData(MEMBER,"/api/v1/document-templates",Map.of("title","Indevido","official",false,"reason","Fixture"),403);
    }
    @Test void officialTemplateEditingIsAlwaysBlocked() throws Exception {
        var d=document(true);String path="/api/v1/documents/"+d.get("id").asString();
        postData(MEMBER,path+"/editor-sessions",Map.of("expectedVersion",0,"readOnly",false),409);
        mvc.perform(multipart(path+"/versions").file(new MockMultipartFile("file",fixture("Proibido","24"))).param("expectedVersion","0").param("reason","Fixture").with(token(MEMBER))).andExpect(status().isConflict());
    }
    @Test void unknownAndAnonymousRoutesRemainClosed() throws Exception {
        mvc.perform(get("/api/v1/documents")).andExpect(status().is4xxClientError());
        mvc.perform(post("/api/v1/documents/provider/unlisted").contentType("application/json").content("{}")).andExpect(status().is4xxClientError());
    }
}
