package br.edu.unijorge.niteconnect;

import java.nio.file.Files;
import java.nio.file.Path;
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

class DiaryWorkflowTest extends ActivityTestSupport {
    static final Path directory;
    static { try { directory = Files.createTempDirectory("nite-diary-test-"); } catch (Exception e) { throw new ExceptionInInitializerError(e); } }
    @DynamicPropertySource static void documents(DynamicPropertyRegistry registry) {
        registry.add("nite.documents.storage-root", directory::toString);
        registry.add("nite.documents.local-development", () -> true);
    }

    private final tools.jackson.databind.ObjectMapper json = JsonMapper.builder().build();

    private JsonNode importDiary(UUID target, boolean includeSourceMetadata) throws Exception {
        return importDiary(target, includeSourceMetadata, DocumentCoreTest.fixture("Resumo semanal importado", "24"));
    }

    private JsonNode importDiary(UUID target, boolean includeSourceMetadata, byte[] sourceBytes) throws Exception {
        var request = multipart("/api/v1/diaries/" + target + "/import")
                .file(new MockMultipartFile("file", "sharepoint-export.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        sourceBytes))
                .param("title", "Diário contínuo")
                .param("reason", "Importação inicial do SharePoint")
                .with(token(ADMIN));
        if (includeSourceMetadata) {
            request.param("sourceCreatedAt", "2026-02-01T10:00:00Z");
            request.param("sourceModifiedAt", "2026-10-01T16:30:00Z");
        }
        String response = mvc.perform(request).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    @Test void importsOneInitialSharePointVersionAndKeepsOptionalSourceDatesSeparate() throws Exception {
        byte[] imported = DocumentCoreTest.fixture("Resumo semanal importado", "24");
        JsonNode created = importDiary(MEMBER, true, imported);
        assertThat(created.get("memberId").asString()).isEqualTo(MEMBER.toString());
        assertThat(created.get("currentRevision").asInt()).isEqualTo(1);
        assertThat(created.get("sourceMetadataOrigin").asString()).isEqualTo("SHAREPOINT");
        assertThat(created.get("sourceCreatedAt").asString()).isEqualTo("2026-02-01T10:00:00Z");
        assertThat(created.get("sourceModifiedAt").asString()).isEqualTo("2026-10-01T16:30:00Z");
        assertThat(created.get("importedAt").asString()).isNotBlank();
        assertThat(created.get("createdAt").asString()).isNotBlank();
        assertThat(created.get("updatedAt").asString()).isEqualTo(created.get("createdAt").asString());

        mvc.perform(get("/api/v1/diaries/me").with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.documentId").value(created.get("documentId").asString()));
        mvc.perform(get("/api/v1/diaries/" + MEMBER).with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentRevision").value(1));

        String documentId = created.get("documentId").asString();
        String versionId = created.get("currentVersionId").asString();
        byte[] downloaded = mvc.perform(get("/api/v1/documents/" + documentId + "/versions/" + versionId + "/download").with(token(MEMBER)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(downloaded).containsExactly(imported);

        mvc.perform(multipart("/api/v1/documents/" + documentId + "/versions")
                .file(new MockMultipartFile("file", "weekly-update.docx",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        DocumentCoreTest.fixture("Resumo semanal editado", "24")))
                .param("expectedVersion", "0").param("reason", "Resumo semanal redigido")
                .with(token(MEMBER))).andExpect(status().isCreated()).andExpect(jsonPath("$.revision").value(2));

        mvc.perform(get("/api/v1/diaries/me").with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.currentRevision").value(2))
                .andExpect(jsonPath("$.documentVersion").value(1));
    }

    @Test void rejectsDuplicateImportAndKeepsAdministrativeDiaryAccessReadOnly() throws Exception {
        JsonNode created = importDiary(MEMBER, false);
        UUID document = UUID.fromString(created.get("documentId").asString());
        assertThat(created.get("sourceMetadataOrigin").isNull()).isTrue();
        assertThat(created.get("sourceCreatedAt").isNull()).isTrue();

        mvc.perform(multipart("/api/v1/diaries/" + MEMBER + "/import")
                .file(new MockMultipartFile("file", DocumentCoreTest.fixture("duplicado", "24")))
                .param("title", "Diário duplicado").param("reason", "Tentativa")
                .with(token(ADMIN))).andExpect(status().isConflict());

        mvc.perform(multipart("/api/v1/documents/" + document + "/versions")
                .file(new MockMultipartFile("file", DocumentCoreTest.fixture("alteração administrativa", "24")))
                .param("expectedVersion", "0").param("reason", "Não permitido")
                .with(token(ADMIN))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/documents/" + document + "/editor-sessions")
                .contentType("application/json").content("{\"expectedVersion\":0,\"readOnly\":false}")
                .with(token(ADMIN))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/diaries/" + MEMBER).with(token(OTHER))).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/diaries/" + MEMBER + "/import")
                .file(new MockMultipartFile("file", DocumentCoreTest.fixture("sem permissão", "24")))
                .param("title", "Diário").param("reason", "Tentativa")
                .with(token(MEMBER))).andExpect(status().isForbidden());
    }

    @Test void activityPanelRequiresAValidWeekAndReturnsOnlyAuthorizedPersonalData() throws Exception {
        var weekStart = java.time.LocalDate.now(java.time.ZoneId.of("America/Bahia"))
                .with(java.time.DayOfWeek.MONDAY);
        var weekEnd = weekStart.plusDays(6);
        activity(ADMIN, null, MEMBER);
        String activityBody = mvc.perform(post("/api/v1/activities").with(token(ADMIN)).contentType("application/json")
                        .content(json.writeValueAsString(java.util.Map.of("title", "Atividade semanal", "description", "Fixture",
                                "dueOn", weekStart.plusDays(1).toString(), "primaryExecutorId", MEMBER, "reason", "Fixture"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String activityId = json.readTree(activityBody).get("id").asString();
        mvc.perform(post("/api/v1/activities/" + activityId + "/evidence").with(token(MEMBER)).contentType("application/json")
                        .content(json.writeValueAsString(java.util.Map.of("type", "LINK", "url", "https://evidence.example.org/weekly",
                                "expectedVersion", 0, "reason", "Evidência da semana"))))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/diaries/me/activities").param("from", weekStart.toString()).param("to", weekEnd.toString()).with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activities.items.length()").value(1))
                .andExpect(jsonPath("$.evidence.items.length()").value(1));
        mvc.perform(get("/api/v1/diaries/me/activities").param("from", weekStart.minusDays(7).toString()).param("to", weekEnd.toString()).with(token(MEMBER)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/diaries/me/activities").param("from", weekStart.toString()).param("to", weekEnd.toString()).with(token(OTHER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activities.items.length()").value(0));
    }

    @Test void activityPanelRejectsDateBoundsThatOverflowWeekArithmetic() throws Exception {
        var maxDate = java.time.LocalDate.MAX;
        mvc.perform(get("/api/v1/diaries/me/activities").param("from", maxDate.toString()).param("to", maxDate.toString()).with(token(MEMBER)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/diaries/me/activities").param("from", maxDate.minusDays(6).toString()).param("to", maxDate.toString()).with(token(MEMBER)))
                .andExpect(status().isBadRequest());
    }
}
