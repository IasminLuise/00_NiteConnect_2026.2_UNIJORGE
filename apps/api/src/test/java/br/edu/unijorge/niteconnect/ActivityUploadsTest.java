package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import br.edu.unijorge.niteconnect.activities.application.*;
import br.edu.unijorge.niteconnect.activities.api.EvidenceContracts.Upload;
import br.edu.unijorge.niteconnect.activities.infrastructure.LocalEvidenceObjectStore;
import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.members.application.MemberAdministration;
import br.edu.unijorge.niteconnect.members.domain.MemberStatus;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.time.*;
import java.nio.file.*;
import java.io.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ActivityUploadsTest extends ActivityTestSupport {
    @Autowired EvidenceService evidenceService;
    @Autowired EvidenceObjectStore store;
    @Autowired AccessService access;
    @Autowired MemberAdministration members;
    @Autowired EvidenceUploads uploads;
    static final Path root;
    static { try { root=Files.createTempDirectory("nite-evidence-test-"); } catch(IOException e) { throw new ExceptionInInitializerError(e); } }
    @DynamicPropertySource static void storage(DynamicPropertyRegistry r) { r.add("nite.evidence.local-root",()->root.toString()); }
    MockMultipartFile metadata(UUID id) { return new MockMultipartFile("metadata","","application/json",command(id,"").getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    @Test void privateTextUploadDownloadsOnlyThroughAuthorizedContext() throws Exception {
        UUID id=activity(MEMBER,null,MEMBER);
        mvc.perform(multipart("/api/v1/activities/"+id+"/evidence/uploads").file(new MockMultipartFile("file","report.txt","text/plain","Contribution".getBytes())).file(metadata(id)).with(token(MEMBER)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.object.size").value(12)).andExpect(jsonPath("$.object.objectKey").doesNotExist());
        UUID evidence=jdbc.queryForObject("select id from nite_connect.activity_evidence where activity_id=?",UUID.class,id);
        mvc.perform(get("/api/v1/activities/"+id+"/evidence/"+evidence+"/download").with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(content().string("Contribution"))
                .andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("X-Content-Type-Options","nosniff"));
        mvc.perform(get("/api/v1/activities/"+id+"/evidence/"+evidence+"/download").with(token(OTHER))).andExpect(status().isNotFound());
        UUID other=activity(MEMBER,null,MEMBER);
        mvc.perform(get("/api/v1/activities/"+other+"/evidence/"+evidence+"/download").with(token(MEMBER))).andExpect(status().isNotFound());
    }
    @Test void invalidFilesNeverBecomeAvailable() throws Exception {
        UUID id=activity(MEMBER,null,MEMBER);
        for(MockMultipartFile file:java.util.List.of(
                new MockMultipartFile("file","image.png","image/png","not an image".getBytes()),
                new MockMultipartFile("file","file.pdf","application/pdf","%PDF".getBytes()),
                new MockMultipartFile("file","binary.txt","text/plain",new byte[]{0,1,2}),
                new MockMultipartFile("file","bad.txt","text/plain",new byte[]{(byte)0xc3,(byte)0x28}))) {
            mvc.perform(multipart("/api/v1/activities/"+id+"/evidence/uploads").file(file).file(metadata(id)).with(token(MEMBER)))
                    .andExpect(status().isUnsupportedMediaType());
        }
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.activity_evidence",Integer.class)).isZero();
        assertThat(av(id)).isZero();
    }
    @Test void imageIsDecodedAndWithdrawalStopsOrdinaryDownload() throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
        UUID id=activity(MEMBER,null,MEMBER);
        mvc.perform(multipart("/api/v1/activities/"+id+"/evidence/uploads").file(new MockMultipartFile("file","image.png","image/png",bytes.toByteArray())).file(metadata(id)).with(token(MEMBER)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.type").value("IMAGE"));
        UUID evidence=jdbc.queryForObject("select id from nite_connect.activity_evidence where activity_id=?",UUID.class,id);
        mvc.perform(post("/api/v1/activities/"+id+"/evidence/"+evidence+"/withdraw").with(token(MEMBER)).contentType("application/json")
                .content(command(id,",\"confirmed\":true"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/activities/"+id+"/evidence/"+evidence+"/download").with(token(MEMBER))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/activities/"+id+"/evidence/"+evidence+"/download").with(token(ADMIN))).andExpect(status().isOk());
    }
    @Test void forbiddenUploadDoesNotCreateReceiptAndOversizedUploadDoesNotCreateEvidence() throws Exception {
        UUID id=activity(MEMBER,null,MEMBER);
        mvc.perform(multipart("/api/v1/activities/"+id+"/evidence/uploads").file(new MockMultipartFile("file","report.txt","text/plain",new byte[]{1})).file(metadata(id)).with(token(OTHER)))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.evidence_objects",Integer.class)).isZero();
        mvc.perform(multipart("/api/v1/activities/"+id+"/evidence/uploads").file(new MockMultipartFile("file","large.txt","text/plain",new byte[10*1024*1024+1])).file(metadata(id)).with(token(MEMBER)))
                .andExpect(status().isPayloadTooLarge());
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.activity_evidence",Integer.class)).isZero();
    }
    @Test void deactivationBetweenByteStorageAndCommitDoesNotPublishEvidence() throws Exception {
        UUID id=activity(MEMBER,null,MEMBER);
        EvidenceObjectStore revoking=new DelegatingStore() {
            @Override public Stored write(UUID key,String filename,String mime,InputStream input) {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
                Stored result=super.write(key,filename,mime,input);
                members.changeStatus(ADMIN,MEMBER,MemberStatus.AFASTADO,0,"Revogação durante upload");
                return result;
            }
        };
        assertThatThrownBy(()->new EvidenceUploads(evidenceService,revoking,access).upload(MEMBER,id,new Upload(0L,"Teste",null),"report.txt","text/plain",new ByteArrayInputStream("Teste".getBytes())))
                .isInstanceOf(ApiException.class).extracting(e->((ApiException)e).status().value()).isEqualTo(403);
        assertThat(av(id)).isZero();assertThat(jdbc.queryForObject("select count(*) from nite_connect.activity_evidence",Integer.class)).isZero();
        UUID key=jdbc.queryForObject("select object_key from nite_connect.evidence_objects where activity_id=?",UUID.class,id);
        assertThat(Files.exists(root.resolve(key+".object"))).isFalse();
        assertThat(jdbc.queryForObject("select state from nite_connect.evidence_objects where activity_id=?",String.class,id)).isEqualTo("FAILED");
    }
    @Test void storageFailureLeavesOnlyFailedPrivateReceipt() throws Exception {
        UUID id=activity(MEMBER,null,MEMBER);
        EvidenceObjectStore failing=new DelegatingStore() {
            @Override public Stored write(UUID key,String filename,String mime,InputStream input) { throw LocalEvidenceObjectStore.unavailable(); }
        };
        assertThatThrownBy(()->new EvidenceUploads(evidenceService,failing,access).upload(MEMBER,id,new Upload(0L,"Teste",null),"report.txt","text/plain",new ByteArrayInputStream("Teste".getBytes())))
                .isInstanceOf(ApiException.class);
        assertThat(jdbc.queryForObject("select state from nite_connect.evidence_objects where activity_id=?",String.class,id)).isEqualTo("FAILED");
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.activity_evidence",Integer.class)).isZero();
    }
    @Test void auditFailureAfterObjectWriteRollsBackEvidenceAndRemovesPrivateObject() throws Exception {
        UUID id=activity(MEMBER,null,MEMBER);
        jdbc.execute("create function nite_connect.test_fail_evidence_audit() returns trigger language plpgsql as $$ begin if NEW.entity_type='EVIDENCE' then raise exception 'Evidence audit failure'; end if; return NEW; end; $$");
        jdbc.execute("create trigger test_fail_evidence_audit before insert on nite_connect.audit_events for each row execute function nite_connect.test_fail_evidence_audit()");
        try {
            assertThatThrownBy(()->uploads.upload(MEMBER,id,new Upload(0L,"Teste",null),"report.txt","text/plain",new ByteArrayInputStream("Teste".getBytes())))
                    .hasStackTraceContaining("Evidence audit failure");
            assertThat(av(id)).isZero();assertThat(jdbc.queryForObject("select count(*) from nite_connect.activity_evidence",Integer.class)).isZero();
            UUID key=jdbc.queryForObject("select object_key from nite_connect.evidence_objects where activity_id=?",UUID.class,id);
            assertThat(Files.exists(root.resolve(key+".object"))).isFalse();
        } finally {
            jdbc.execute("drop trigger test_fail_evidence_audit on nite_connect.audit_events");
            jdbc.execute("drop function nite_connect.test_fail_evidence_audit()");
        }
    }
    @Test void crashOrphanRecoveryHasDryRunAndPreservesAvailableObjects() throws Exception {
        UUID id=activity(MEMBER,null,MEMBER);
        uploads.upload(MEMBER,id,new Upload(0L,"Teste",null),"report.txt","text/plain",new ByteArrayInputStream("Teste".getBytes()));
        UUID available=jdbc.queryForObject("select object_key from nite_connect.evidence_objects where activity_id=?",UUID.class,id);
        var pending=evidenceService.prepare(MEMBER,id,new Upload(av(id),"Preparar",null),"orphan.txt",true);
        Path orphan=root.resolve(pending.objectKey()+".object");Files.writeString(orphan,"Private orphan");
        Instant old=Instant.now().minus(Duration.ofHours(2));
        Files.setLastModifiedTime(orphan,java.nio.file.attribute.FileTime.from(old));
        Files.setLastModifiedTime(root.resolve(available+".object"),java.nio.file.attribute.FileTime.from(old));
        jdbc.update("update nite_connect.evidence_objects set updated_at=? where id=?",java.sql.Timestamp.from(old),pending.id());
        assertThat(uploads.reconcile(ADMIN,Duration.ofHours(1),true)).contains(pending.objectKey()).doesNotContain(available);
        assertThat(Files.exists(orphan)).isTrue();
        assertThat(uploads.reconcile(ADMIN,Duration.ofHours(1),false)).contains(pending.objectKey()).doesNotContain(available);
        assertThat(Files.exists(orphan)).isFalse();assertThat(Files.exists(root.resolve(available+".object"))).isTrue();
        assertThat(jdbc.queryForObject("select state from nite_connect.evidence_objects where id=?",String.class,pending.id())).isEqualTo("FAILED");
    }
    @Test void damagedObjectIsNotReturnedAsValidContent() throws Exception {
        UUID id=activity(MEMBER,null,MEMBER);
        var evidence=uploads.upload(MEMBER,id,new Upload(0L,"Teste",null),"report.txt","text/plain",new ByteArrayInputStream("Teste".getBytes()));
        UUID key=jdbc.queryForObject("select object_key from nite_connect.evidence_objects where activity_id=?",UUID.class,id);
        Files.writeString(root.resolve(key+".object"),"Alterado");
        mvc.perform(get("/api/v1/activities/"+id+"/evidence/"+evidence.id()+"/download").with(token(MEMBER))).andExpect(status().isServiceUnavailable());
    }
    @Test void recoveryAlsoClosesStaleReceiptsWhenProcessDiedBeforeWritingAnyFile() throws Exception {
        UUID id=activity(MEMBER,null,MEMBER);
        var pending=evidenceService.prepare(MEMBER,id,new Upload(0L,"Preparar",null),"missing.txt",true);
        Instant old=Instant.now().minus(Duration.ofHours(2));
        jdbc.update("update nite_connect.evidence_objects set updated_at=? where id=?",java.sql.Timestamp.from(old),pending.id());
        assertThat(uploads.reconcile(ADMIN,Duration.ofHours(1),true)).contains(pending.objectKey());
        assertThat(jdbc.queryForObject("select state from nite_connect.evidence_objects where id=?",String.class,pending.id())).isEqualTo("PENDING");
        assertThat(uploads.reconcile(ADMIN,Duration.ofHours(1),false)).contains(pending.objectKey());
        assertThat(jdbc.queryForObject("select state from nite_connect.evidence_objects where id=?",String.class,pending.id())).isEqualTo("FAILED");
    }
    class DelegatingStore implements EvidenceObjectStore {
        public boolean enabled() { return store.enabled(); }
        public Stored write(UUID key,String filename,String mime,InputStream input) { return store.write(key,filename,mime,input); }
        public byte[] read(UUID key,long size,String hash) { return store.read(key,size,hash); }
        public void delete(UUID key) { store.delete(key); }
        public java.util.List<StoredKey> olderThan(Instant cutoff) { return store.olderThan(cutoff); }
    }
}
