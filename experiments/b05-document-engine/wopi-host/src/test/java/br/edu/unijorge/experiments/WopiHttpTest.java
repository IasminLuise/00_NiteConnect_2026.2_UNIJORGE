package br.edu.unijorge.experiments;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.file.Path;
import java.time.*;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WopiHttpTest {
    @TempDir Path root;
    DocumentStore store;MockMvc mvc;UUID id;DocumentStore.Session session;
    @BeforeEach void setup()throws Exception {
        store=new DocumentStore(root,Clock.systemUTC());id=UUID.randomUUID();store.seed(id,"F01.docx",DocumentStoreTest.docx("one"));
        session=store.issue(id,true,Duration.ofMinutes(10));
        mvc=MockMvcBuilders.standaloneSetup(new WopiController(store),new LabController(store)).addFilters(new LabApplication().privacyHeaders()).setControllerAdvice(new WopiController.Errors()).build();
    }
    @Test void checkFileInfoReflectsTokenPermissionsAndNeverAdvertisesSaveAs()throws Exception {
        var ro=store.issue(id,false,Duration.ofMinutes(10));
        mvc.perform(get("/wopi/files/"+id).param("access_token",ro.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.UserCanWrite").value(false))
            .andExpect(jsonPath("$.UserCanNotWriteRelative").value(true))
            .andExpect(jsonPath("$.LastModifiedTime").isString());
    }
    @Test void putFileUsesPostOverrideAndReturnsSavedTimestampAndVersion()throws Exception {
        var before=store.info(id,session.token());
        mvc.perform(post("/wopi/files/"+id+"/contents").param("access_token",session.token())
            .header("X-WOPI-Override","PUT").header("X-COOL-WOPI-Timestamp",before.modified()).content(DocumentStoreTest.docx("two")))
            .andExpect(status().isOk()).andExpect(header().exists("X-WOPI-ItemVersion"))
            .andExpect(jsonPath("LastModifiedTime").isString());
        Assertions.assertEquals(2,store.versions(id).size());
    }
    @Test void stalePutHasCollaboraConflictCodeAndLockHeader()throws Exception {
        mvc.perform(post("/wopi/files/"+id+"/contents").param("access_token",session.token())
            .header("X-WOPI-Override","PUT").header("X-COOL-WOPI-Timestamp","2000-01-01T00:00:00Z").content(DocumentStoreTest.docx("two")))
            .andExpect(status().isConflict()).andExpect(header().string("X-WOPI-Lock",""))
            .andExpect(jsonPath("COOLStatusCode").value(1010));
    }
    @Test void legacyTimestampFromOnlyofficeStillEnforcesVersionCas()throws Exception {
        var before=store.info(id,session.token());
        mvc.perform(post("/wopi/files/"+id+"/contents").param("access_token",session.token())
            .header("X-WOPI-Override","PUT").header("X-LOOL-WOPI-Timestamp",before.modified()).content(DocumentStoreTest.docx("two")))
            .andExpect(status().isOk());
        mvc.perform(post("/wopi/files/"+id+"/contents").param("access_token",session.token())
            .header("X-WOPI-Override","PUT").header("X-LOOL-WOPI-Timestamp",before.modified()).content(DocumentStoreTest.docx("three")))
            .andExpect(status().isConflict());
    }
    @Test void unknownOperationAndUnauthenticatedDownloadAreDenied()throws Exception {
        mvc.perform(get("/wopi/files/"+id+"/contents")).andExpect(status().isUnauthorized());
        mvc.perform(post("/wopi/files/"+id).param("access_token",session.token()).header("X-WOPI-Override","DELETE"))
            .andExpect(status().isNotImplemented());
    }
    @Test void binaryPutWithoutOverrideIsRejected()throws Exception {
        mvc.perform(post("/wopi/files/"+id+"/contents").param("access_token",session.token()).content(DocumentStoreTest.docx("two")))
            .andExpect(status().isNotImplemented());
    }
    @Test void launchPagePreservesSameOriginFormIdentityButWopiNeverSendsReferrer()throws Exception {
        mvc.perform(get("/")).andExpect(header().string("Referrer-Policy","same-origin"));
        mvc.perform(get("/wopi/files/"+id).param("access_token",session.token())).andExpect(header().string("Referrer-Policy","no-referrer"));
    }
    @Test void foreignOriginCannotMintOrRevokeSessions()throws Exception {
        mvc.perform(post("/lab/editor").param("id",id.toString()).header("Origin","https://untrusted.invalid")).andExpect(status().isForbidden());
        mvc.perform(post("/lab/revoke/"+session.id()).header("Origin","https://untrusted.invalid")).andExpect(status().isForbidden());
        mvc.perform(get("/wopi/files/"+id).param("access_token",session.token())).andExpect(status().isOk());
    }
}
