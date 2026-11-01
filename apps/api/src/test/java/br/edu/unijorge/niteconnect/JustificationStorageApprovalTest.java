package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JustificationStorageApprovalTest extends AttendanceTestSupport {
    private static final Path PRIVATE_STORAGE = createPrivateStorage();

    @DynamicPropertySource
    static void configureUnapprovedStorage(DynamicPropertyRegistry properties) {
        properties.add("nite.justification.local-root", () -> PRIVATE_STORAGE.toString());
        properties.add("nite.justification.storage.approved", () -> false);
        properties.add("nite.justification.retention.authorized-validator-ids", MEMBER::toString);
    }

    @AfterAll
    static void removePrivateStorage() throws Exception {
        Files.deleteIfExists(PRIVATE_STORAGE);
    }

    @Test
    void configuredLocalDirectoryDoesNotEnableUploadWithoutExplicitStorageApproval() throws Exception {
        validateRetentionPolicy();
        UUID justificationId = createJustification();

        mvc.perform(get("/api/v1/me/absence-justifications/" + justificationId + "/attachments").with(token(MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadAvailability").value("ARMAZENAMENTO_AGUARDANDO_APROVACAO"));

        mvc.perform(multipart("/api/v1/me/absence-justifications/" + justificationId + "/attachments")
                        .file(new MockMultipartFile("file", "comprovante.pdf", "application/pdf", pdf()))
                        .param("context", "JUSTIFICATIVA").with(token(MEMBER)))
                .andExpect(status().isServiceUnavailable());

        try (var files = Files.list(PRIVATE_STORAGE)) {
            org.assertj.core.api.Assertions.assertThat(files).isEmpty();
        }
    }

    private void validateRetentionPolicy() throws Exception {
        String proposal = mvc.perform(post("/api/v1/attendance/justification-attachment-retention-policies").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"retentionMonths\":24,\"deletionGraceDays\":30,\"reason\":\"Proposta pendente de aprovação do armazenamento\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID policyId = UUID.fromString(json.readTree(proposal).get("id").asString());
        mvc.perform(post("/api/v1/attendance/justification-attachment-retention-policies/" + policyId + "/validations").with(token(MEMBER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"sourceReference\":\"Aprovação de teste\","
                                + "\"backupHandlingReference\":\"Backup de teste\","
                                + "\"sensitiveDataHandlingReference\":\"Dados sensíveis de teste\","
                                + "\"reason\":\"Validação de privacidade de teste\"}"))
                .andExpect(status().isOk());
    }

    private UUID createJustification() throws Exception {
        String eventJson = json.writeValueAsString(Map.of("title", "Sessão encerrada", "description", "Evento concluído",
                "kind", "ENCONTRO", "modality", "PRESENCIAL", "startsAt", Instant.now().minusSeconds(7200).toString(),
                "endsAt", Instant.now().minusSeconds(3600).toString(), "expectedMemberIds", List.of(MEMBER), "reason", "Teste"));
        String event = mvc.perform(post("/api/v1/agenda/events").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON).content(eventJson))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID eventId = UUID.fromString(json.readTree(event).get("id").asString());
        String request = json.writeValueAsString(Map.of("eventId", eventId, "reason", "Justificativa de teste"));
        String body = mvc.perform(post("/api/v1/me/absence-justifications").with(token(MEMBER))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(body).get("id").asString());
    }

    private static byte[] pdf() {
        return "%PDF-1.4\n1 0 obj <</Type/Catalog>> endobj\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    private static Path createPrivateStorage() {
        try { return Files.createTempDirectory("nite-unapproved-justification-storage-"); }
        catch (java.io.IOException e) { throw new ExceptionInInitializerError(e); }
    }
}
