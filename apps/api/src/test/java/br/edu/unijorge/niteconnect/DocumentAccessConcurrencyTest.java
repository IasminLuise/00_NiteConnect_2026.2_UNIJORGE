package br.edu.unijorge.niteconnect;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentAccessConcurrencyTest extends ActivityTestSupport {
    private static final Path DIRECTORY;
    static {
        try { DIRECTORY = Files.createTempDirectory("nite-doc-access-test-"); }
        catch (IOException e) { throw new ExceptionInInitializerError(e); }
    }
    @DynamicPropertySource static void documentStorage(DynamicPropertyRegistry registry) {
        registry.add("nite.documents.storage-root", DIRECTORY::toString);
        registry.add("nite.documents.local-development", () -> true);
    }
    private final tools.jackson.databind.ObjectMapper json = JsonMapper.builder().build();

    @Test void projectLeadershipAndExplicitGrantsStillRequireCurrentProjectMembership() throws Exception {
        UUID project = activeProject();
        JsonNode document = createDocument("PROJECT", project);
        UUID id = UUID.fromString(document.get("id").asString());
        String path = "/api/v1/documents/" + id;
        mvc.perform(get(path).with(token(LEADER))).andExpect(status().isOk());
        mvc.perform(get(path).with(token(MEMBER))).andExpect(status().isForbidden());
        assertListSize(MEMBER, 0);

        postData(ADMIN, path + "/grants", Map.of("memberId", MEMBER, "canEdit", false,
                "expectedVersion", 0, "reason", "Leitura contextual"), 200);
        mvc.perform(get(path).with(token(MEMBER))).andExpect(status().isOk());
        assertListSize(MEMBER, 1);
        assertThat(upload(MEMBER, id, 1, DocumentCoreTest.fixture("Sem permissão", "24")).getStatus()).isEqualTo(403);

        postData(ADMIN, path + "/grants", Map.of("memberId", MEMBER, "canEdit", true,
                "expectedVersion", 1, "reason", "Edição contextual"), 200);
        assertThat(upload(MEMBER, id, 2, DocumentCoreTest.fixture("Permitido", "24")).getStatus()).isEqualTo(201);
        assertThat(jdbc.update("update nite_connect.project_memberships set ended_at=clock_timestamp(), ended_by=?, end_reason='Fim do vínculo sintético' where project_id=? and member_id=? and ended_at is null",
                ADMIN, project, MEMBER)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.document_grants where document_id=? and member_id=? and revoked_at is null",
                Integer.class, id, MEMBER)).isEqualTo(1);
        mvc.perform(get(path).with(token(MEMBER))).andExpect(status().isForbidden());
        mvc.perform(get(path + "/versions/" + document.get("currentVersionId").asString() + "/download")
                .with(token(MEMBER))).andExpect(status().isForbidden());
        assertThat(upload(MEMBER, id, 3, DocumentCoreTest.fixture("Revogado", "24")).getStatus()).isEqualTo(403);
        assertListSize(MEMBER, 0);
    }

    @Test void simultaneousUploadsWithSameExpectedVersionPublishExactlyOneCandidate() throws Exception {
        UUID id = UUID.fromString(createDocument("PERSONAL", null).get("id").asString());
        byte[] first = DocumentCoreTest.fixture("Primeira candidata", "24");
        byte[] second = DocumentCoreTest.fixture("Segunda candidata", "24");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> uploadAfter(ready, start, id, first));
            var b = executor.submit(() -> uploadAfter(ready, start, id, second));
            try { assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); }
            finally { start.countDown(); }
            assertThat(List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.document_versions where document_id=?", Integer.class, id)).isEqualTo(2);
        assertThat(jdbc.queryForObject("select version from nite_connect.documents where id=?", Long.class, id)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("select v.status from nite_connect.documents d join nite_connect.document_versions v on v.id=d.current_version_id where d.id=?",
                String.class, id)).isEqualTo("AVAILABLE");
        mvc.perform(get("/api/v1/documents/" + id + "/versions").with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2));
    }

    @Test void restorationRejectsCrossDocumentVersionAndQuarantinedCandidate() throws Exception {
        JsonNode first = createDocument("PERSONAL", null);
        JsonNode second = createDocument("PERSONAL", null);
        UUID id = UUID.fromString(first.get("id").asString());
        String path = "/api/v1/documents/" + id;
        postData(ADMIN, path + "/restore", Map.of("versionId", second.get("currentVersionId").asString(),
                "expectedVersion", 0, "confirmed", true, "reason", "Versão de outro documento"), 404);
        var response = upload(MEMBER, id, 0, DocumentCoreTest.fixture("Formato alterado", "18"));
        assertThat(response.getStatus()).isEqualTo(201);
        JsonNode candidate = json.readTree(response.getContentAsString());
        assertThat(candidate.get("status").asString()).isEqualTo("QUARANTINED");
        postData(ADMIN, path + "/restore", Map.of("versionId", candidate.get("id").asString(),
                "expectedVersion", 1, "confirmed", true, "reason", "Quarentena não publicável"), 409);
        assertThat(jdbc.queryForObject("select current_version_id from nite_connect.documents where id=?", UUID.class, id))
                .isEqualTo(UUID.fromString(first.get("currentVersionId").asString()));
    }

    @Test void archivingRequiresAdministratorConfirmationAndCurrentExpectedVersion() throws Exception {
        UUID id = UUID.fromString(createDocument("PERSONAL", null).get("id").asString());
        String path = "/api/v1/documents/" + id + "/archive";
        postData(MEMBER, path, Map.of("expectedVersion", 0, "confirmed", true, "reason", "Tentativa do titular"), 403);
        postData(ADMIN, path, Map.of("expectedVersion", 0, "confirmed", false, "reason", "Sem confirmação"), 400);
        postData(ADMIN, path, Map.of("expectedVersion", 1, "confirmed", true, "reason", "Revisão divergente"), 409);
        assertThat(jdbc.queryForObject("select archived_at is null from nite_connect.documents where id=?", Boolean.class, id)).isTrue();
        JsonNode archived = postData(ADMIN, path, Map.of("expectedVersion", 0, "confirmed", true, "reason", "Arquivamento confirmado"), 200);
        assertThat(archived.get("archivedAt").isNull()).isFalse();
        postData(ADMIN, path, Map.of("expectedVersion", 0, "confirmed", true, "reason", "Comando repetido desatualizado"), 409);
        assertThat(jdbc.queryForObject("select version from nite_connect.documents where id=?", Long.class, id)).isEqualTo(1L);
        assertThat(upload(MEMBER, id, 1, DocumentCoreTest.fixture("Após arquivamento", "24")).getStatus()).isEqualTo(409);
    }

    private int uploadAfter(CountDownLatch ready, CountDownLatch start, UUID id, byte[] bytes) throws Exception {
        ready.countDown();
        if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent test start timed out");
        return upload(MEMBER, id, 0, bytes).getStatus();
    }

    private org.springframework.mock.web.MockHttpServletResponse upload(UUID actor, UUID id, long expected, byte[] bytes) throws Exception {
        return mvc.perform(multipart("/api/v1/documents/" + id + "/versions")
                .file(new MockMultipartFile("file", "synthetic.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bytes))
                .param("expectedVersion", Long.toString(expected)).param("reason", "Candidata sintética").with(token(actor)))
                .andReturn().getResponse();
    }

    private void assertListSize(UUID actor, int expected) throws Exception {
        mvc.perform(get("/api/v1/documents").with(token(actor))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(expected)).andExpect(jsonPath("$.items.length()").value(expected));
    }

    private JsonNode createDocument(String context, UUID project) throws Exception {
        JsonNode template = postData(ADMIN, "/api/v1/document-templates",
                Map.of("title", "Modelo sintético", "official", false, "reason", "Fixture isolada"), 201);
        var uploaded = mvc.perform(multipart("/api/v1/document-templates/" + template.get("id").asString() + "/versions")
                .file(new MockMultipartFile("file", "synthetic.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", DocumentCoreTest.fixture("Original", "24")))
                .param("expectedVersion", "0").param("reason", "Fixture isolada").with(token(ADMIN)))
                .andExpect(status().isCreated()).andReturn().getResponse();
        String version = json.readTree(uploaded.getContentAsString()).get("id").asString();
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("title", "Documento sintético"); body.put("context", context);
        body.put("templateVersionId", version); body.put("reason", "Fixture isolada");
        if (project == null) body.put("ownerId", MEMBER); else body.put("projectId", project);
        return postData(ADMIN, "/api/v1/documents", body, 201);
    }

    private JsonNode postData(UUID actor, String path, Object body, int expectedStatus) throws Exception {
        var response = mvc.perform(post(path).with(token(actor)).contentType("application/json")
                .content(json.writeValueAsString(body))).andExpect(status().is(expectedStatus)).andReturn().getResponse();
        return response.getContentAsString().isEmpty() ? json.createObjectNode() : json.readTree(response.getContentAsString());
    }
}
