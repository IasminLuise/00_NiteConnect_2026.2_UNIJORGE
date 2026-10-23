package br.edu.unijorge.niteconnect;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DocumentTemplateCatalogTest extends ActivityTestSupport {
    static final Path directory;
    static {
        try { directory = Files.createTempDirectory("nite-template-catalog-test-"); }
        catch (IOException error) { throw new ExceptionInInitializerError(error); }
    }
    @DynamicPropertySource static void documents(DynamicPropertyRegistry registry) {
        registry.add("nite.documents.storage-root", directory::toString);
        registry.add("nite.documents.local-development", () -> true);
    }
    private final tools.jackson.databind.ObjectMapper json = JsonMapper.builder().build();

    private UUID template() throws Exception {
        String body = mvc.perform(post("/api/v1/document-templates").with(token(ADMIN))
                .contentType("application/json").content(json.writeValueAsString(Map.of(
                        "title", "Modelo sintético", "official", false, "reason", "Fixture"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(body).get("id").asString());
    }

    private void upload(UUID template, int expectedVersion) throws Exception {
        mvc.perform(multipart("/api/v1/document-templates/" + template + "/versions")
                .file(new MockMultipartFile("file", "synthetic.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        DocumentCoreTest.fixture("Revisão " + expectedVersion, "24")))
                .param("expectedVersion", Integer.toString(expectedVersion)).param("reason", "Fixture")
                .with(token(ADMIN))).andExpect(status().isCreated());
    }

    @Test void administratorCanDiscoverPaginatedVersionsAndCreateDocumentWithoutUploadResponse() throws Exception {
        UUID template = template();
        upload(template, 0);
        upload(template, 1);
        UUID unrelated = template();
        upload(unrelated, 0);

        mvc.perform(get("/api/v1/document-templates").with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2));
        String first = mvc.perform(get("/api/v1/document-templates/" + template + "/versions")
                .param("size", "1").with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].revision").value(2))
                .andExpect(jsonPath("$.items[0].objectId").doesNotExist())
                .andExpect(jsonPath("$.items[0].sha256").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        mvc.perform(get("/api/v1/document-templates/" + template + "/versions")
                .param("page", "1").param("size", "1").with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].revision").value(1));
        JsonNode selected = json.readTree(first).get("items").get(0);
        String created = mvc.perform(post("/api/v1/documents").with(token(ADMIN))
                .contentType("application/json").content(json.writeValueAsString(Map.of(
                        "title", "Atribuição pelo catálogo", "context", "PERSONAL", "ownerId", MEMBER,
                        "templateVersionId", selected.get("id").asString(), "reason", "Fixture"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        assertThat(json.readTree(created).get("templateVersionId").asString())
                .isEqualTo(selected.get("id").asString());
    }

    @Test void versionCatalogRequiresAdministratorAndValidPagination() throws Exception {
        UUID template = template();
        String path = "/api/v1/document-templates/" + template + "/versions";
        mvc.perform(get(path).with(token(ADMIN))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.items.length()").value(0));
        mvc.perform(get(path).with(token(MEMBER))).andExpect(status().isForbidden());
        mvc.perform(get(path)).andExpect(status().is4xxClientError());
        mvc.perform(get(path).param("page", "-1").with(token(ADMIN))).andExpect(status().isBadRequest());
        mvc.perform(get(path).param("size", "101").with(token(ADMIN))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/document-templates/" + UUID.randomUUID() + "/versions").with(token(ADMIN)))
                .andExpect(status().isNotFound());
    }
}
