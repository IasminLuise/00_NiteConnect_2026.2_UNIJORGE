package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import br.edu.unijorge.niteconnect.attendance.infrastructure.JustificationAttachmentOperationLocks;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class JustificationAttachmentPurgeTest extends AttendanceTestSupport {
    private static final ZoneId INSTITUTIONAL_ZONE = ZoneId.of("America/Bahia");
    private static final Path PRIVATE_STORAGE = createPrivateStorage();
    @Autowired JustificationAttachmentOperationLocks operationLocks;

    @DynamicPropertySource
    static void configureApprovedTestStorage(DynamicPropertyRegistry properties) {
        properties.add("nite.justification.local-root", () -> PRIVATE_STORAGE.toString());
        properties.add("nite.justification.storage.approved", () -> true);
        properties.add("nite.justification.retention.authorized-validator-ids", MEMBER::toString);
        properties.add("nite.justification.retention.purge.execution-enabled", () -> true);
        properties.add("nite.justification.retention.purge.enabled", () -> false);
    }

    @AfterAll
    static void removePrivateTestStorage() throws IOException {
        clearPrivateStorage();
        Files.deleteIfExists(PRIVATE_STORAGE);
    }

    @AfterEach
    void clearTestFiles() throws IOException {
        clearPrivateStorage();
    }

    @Test
    void dryRunFindsOnlyExpiredFinalClassifiedUnheldAttachmentsWithoutDeletingFiles() throws Exception {
        UUID policyId = validatedRetentionPolicy();
        AttachmentFixture eligible = availableExpiredAttachment(policyId, "Elegível para descarte");
        AttachmentFixture held = availableExpiredAttachment(policyId, "Conservação excepcional");
        mvc.perform(post("/api/v1/attendance/justification-attachments/" + held.id() + "/retention-holds").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"basis\":\"Apuração em curso\",\"reason\":\"Conservar até decisão\"}"))
                .andExpect(status().isOk());
        AttachmentFixture pending = pendingAttachment("Solicitação ainda pendente");

        String response = mvc.perform(post("/api/v1/attendance/justification-attachment-retention-purges").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"DRY_RUN\",\"confirmed\":false,\"reason\":\"Conferência antes da execução\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIMULACAO"))
                .andExpect(jsonPath("$.candidateCount").value(1))
                .andExpect(jsonPath("$.deletedCount").value(0))
                .andExpect(jsonPath("$.failedCount").value(0))
                .andExpect(jsonPath("$.items[0].attachmentId").value(eligible.id().toString()))
                .andExpect(jsonPath("$.items[0].outcome").value("ELEGIVEL"))
                .andExpect(jsonPath("$.items[0].objectKey").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        UUID runId = UUID.fromString(json.readTree(response).get("id").asString());

        mvc.perform(get("/api/v1/attendance/justification-attachment-retention-purges").with(token(MEMBER)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/attendance/justification-attachment-retention-purges").with(token(MEMBER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"DRY_RUN\",\"confirmed\":false,\"reason\":\"Sem permissão\"}"))
                .andExpect(status().isForbidden());

        assertThat(Files.exists(eligible.objectPath())).isTrue();
        assertThat(Files.exists(held.objectPath())).isTrue();
        assertThat(Files.exists(pending.objectPath())).isTrue();
        assertThat(attachmentState(eligible.id())).isEqualTo("DISPONIVEL");
        assertThat(attachmentState(held.id())).isEqualTo("DISPONIVEL");
        assertThat(attachmentState(pending.id())).isEqualTo("DISPONIVEL");
        mvc.perform(get("/api/v1/attendance/justification-attachment-retention-purges/" + runId).with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void deletionLockCannotBeAcquiredByAnotherSessionWhileHeld() {
        Boolean acquiredInside = operationLocks.tryWithKey(JustificationAttachmentOperationLocks.RETENTION_PURGE_KEY,
                () -> operationLocks.tryWithKey(JustificationAttachmentOperationLocks.RETENTION_PURGE_KEY,
                        () -> Boolean.TRUE).isPresent()).orElseThrow();

        assertThat(acquiredInside).isFalse();
    }

    @Test
    void dryRunWaitsUntilTheRetentionDeadlineIncludingThirtyDayMinimumGrace() throws Exception {
        UUID policyId = validatedRetentionPolicy();
        AttachmentFixture deadlineToday = availableExpiredAttachment(policyId, "Carência concluída hoje");
        AttachmentFixture deadlineTomorrow = availableExpiredAttachment(policyId, "Carência termina amanhã");
        LocalDate today = LocalDate.now(INSTITUTIONAL_ZONE);
        setDeletionDeadline(deadlineToday, today);
        setDeletionDeadline(deadlineTomorrow, today.plusDays(1));

        mvc.perform(post("/api/v1/attendance/justification-attachment-retention-purges").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"DRY_RUN\",\"confirmed\":false,\"reason\":\"Validar carência mínima\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateCount").value(1))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].attachmentId").value(deadlineToday.id().toString()));
        assertThat(Files.exists(deadlineToday.objectPath())).isTrue();
        assertThat(Files.exists(deadlineTomorrow.objectPath())).isTrue();
    }

    @Test
    void failedObjectDeletionIsAuditedAndARepeatedRunCompletesSafely() throws Exception {
        UUID policyId = validatedRetentionPolicy();
        AttachmentFixture attachment = availableExpiredAttachment(policyId, "Repetição segura após falha");
        Path object = attachment.objectPath();
        Path backup = PRIVATE_STORAGE.resolve(attachment.id() + ".backup");
        Files.move(object, backup);
        Files.createDirectory(object);
        Files.writeString(object.resolve("block-delete"), "test");

        String failed = executePurge("Falha simulada no objeto");
        UUID failedRunId = UUID.fromString(json.readTree(failed).get("id").asString());
        assertThat(json.readTree(failed).get("status").asString()).isEqualTo("CONCLUIDA_COM_FALHAS");
        assertThat(json.readTree(failed).get("failedCount").asInt()).isEqualTo(1);
        assertThat(attachmentState(attachment.id())).isEqualTo("DISPONIVEL");
        mvc.perform(get("/api/v1/attendance/justification-attachment-retention-purges/" + failedRunId).with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].outcome").value("FALHA"));

        Files.delete(object.resolve("block-delete"));
        Files.delete(object);
        Files.move(backup, object);
        String succeeded = executePurge("Repetir descarte após corrigir falha");
        assertThat(json.readTree(succeeded).get("status").asString()).isEqualTo("CONCLUIDA");
        assertThat(json.readTree(succeeded).get("deletedCount").asInt()).isEqualTo(1);
        assertThat(attachmentState(attachment.id())).isEqualTo("REMOVIDO");
        assertThat(Files.exists(object)).isFalse();

        String repeated = executePurge("Repetir após sucesso");
        assertThat(json.readTree(repeated).get("candidateCount").asInt()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.absence_justification_attachment_events where attachment_id=? and event_type='EXCLUIDO'",
                Integer.class, attachment.id())).isEqualTo(1);
    }

    @Test
    void executionWithoutValidatedPolicyIsBlockedAndDoesNotTouchFiles() throws Exception {
        UUID proposalId = retentionProposal();
        mvc.perform(post("/api/v1/attendance/justification-attachment-retention-purges").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"EXECUTE\",\"confirmed\":false,\"reason\":\"Sem confirmação\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/attendance/justification-attachment-retention-purges").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"EXECUTE\",\"confirmed\":true,\"reason\":\"Validar bloqueio\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOQUEADA"))
                .andExpect(jsonPath("$.resultCode").value("POLITICA_NAO_VALIDADA"));
        assertThat(jdbc.queryForObject("select status from nite_connect.justification_attachment_retention_policies p "
                + "join lateral (select event_type as status from nite_connect.justification_attachment_retention_events e "
                + "where e.policy_id=p.id order by sequence desc limit 1) s on true where p.id=?", String.class, proposalId))
                .isEqualTo("CRIADA");
    }

    private String executePurge(String reason) throws Exception {
        return mvc.perform(post("/api/v1/attendance/justification-attachment-retention-purges").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("mode", "EXECUTE", "confirmed", true, "reason", reason))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }

    private UUID retentionProposal() throws Exception {
        String body = mvc.perform(post("/api/v1/attendance/justification-attachment-retention-policies").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"retentionMonths\":24,\"deletionGraceDays\":30,\"reason\":\"Proposta\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(body).get("id").asString());
    }

    private UUID validatedRetentionPolicy() throws Exception {
        UUID policyId = retentionProposal();
        mvc.perform(post("/api/v1/attendance/justification-attachment-retention-policies/" + policyId + "/validations").with(token(MEMBER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"sourceReference\":\"Validação institucional de teste\","
                                + "\"backupHandlingReference\":\"Backup de teste\","
                                + "\"sensitiveDataHandlingReference\":\"Dados sensíveis de teste\","
                                + "\"reason\":\"Validada para teste isolado\"}"))
                .andExpect(status().isOk());
        return policyId;
    }

    private AttachmentFixture availableExpiredAttachment(UUID policyId, String reason) throws Exception {
        UUID justificationId = createJustification(reason);
        mvc.perform(multipart("/api/v1/me/absence-justifications/" + justificationId + "/attachments")
                        .file(new MockMultipartFile("file", "comprovante.pdf", "application/pdf", pdf()))
                        .param("context", "JUSTIFICATIVA").with(token(MEMBER)))
                .andExpect(status().isCreated());
        UUID attachmentId = jdbc.queryForObject("select id from nite_connect.absence_justification_attachments "
                + "where justification_id=?", UUID.class, justificationId);
        mvc.perform(post("/api/v1/attendance/justifications/" + justificationId + "/decisions").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"decision\":\"ACEITA\",\"reason\":\"Análise finalizada\"}"))
                .andExpect(status().isOk());
        jdbc.update("update nite_connect.absence_justifications set decided_at=? where id=?",
                java.sql.Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")), justificationId);
        mvc.perform(post("/api/v1/attendance/justifications/" + justificationId + "/retention-classifications").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semesterEndOn\":\"2020-01-31\",\"sourceReference\":\"Calendário teste\","
                                + "\"reason\":\"Prazo de retenção vencido\"}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select retention_policy_id from nite_connect.absence_justification_attachments where id=?",
                UUID.class, attachmentId)).isEqualTo(policyId);
        return new AttachmentFixture(attachmentId, justificationId, PRIVATE_STORAGE.resolve(attachmentId + ".object"));
    }

    private void setDeletionDeadline(AttachmentFixture attachment, LocalDate deadline) {
        LocalDate decisionDate = decisionDateForDeletionDeadline(deadline);
        jdbc.update("update nite_connect.absence_justifications set decided_at=? where id=?",
                java.sql.Timestamp.from(decisionDate.atStartOfDay(INSTITUTIONAL_ZONE).toInstant()), attachment.justificationId());
    }

    private static LocalDate decisionDateForDeletionDeadline(LocalDate deadline) {
        LocalDate candidate = deadline.minusDays(30).minusMonths(24);
        for (int offset = -5; offset <= 5; offset++) {
            LocalDate date = candidate.plusDays(offset);
            if (date.plusMonths(24).plusDays(30).equals(deadline)) return date;
        }
        throw new IllegalStateException("Não foi possível calcular a data-limite de descarte");
    }

    private AttachmentFixture pendingAttachment(String reason) throws Exception {
        UUID justificationId = createJustification(reason);
        mvc.perform(multipart("/api/v1/me/absence-justifications/" + justificationId + "/attachments")
                        .file(new MockMultipartFile("file", "pendente.pdf", "application/pdf", pdf()))
                        .param("context", "JUSTIFICATIVA").with(token(MEMBER)))
                .andExpect(status().isCreated());
        UUID attachmentId = jdbc.queryForObject("select id from nite_connect.absence_justification_attachments "
                + "where justification_id=?", UUID.class, justificationId);
        return new AttachmentFixture(attachmentId, justificationId, PRIVATE_STORAGE.resolve(attachmentId + ".object"));
    }

    private UUID createJustification(String reason) throws Exception {
        String eventJson = json.writeValueAsString(Map.of("title", "Sessão encerrada", "description", "Evento concluído",
                "kind", "ENCONTRO", "modality", "PRESENCIAL", "startsAt", Instant.now().minusSeconds(7200).toString(),
                "endsAt", Instant.now().minusSeconds(3600).toString(), "expectedMemberIds", List.of(MEMBER), "reason", "Teste"));
        String event = mvc.perform(post("/api/v1/agenda/events").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON).content(eventJson))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID eventId = UUID.fromString(json.readTree(event).get("id").asString());
        String request = json.writeValueAsString(Map.of("eventId", eventId, "reason", reason));
        String body = mvc.perform(post("/api/v1/me/absence-justifications").with(token(MEMBER))
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(body).get("id").asString());
    }

    private String attachmentState(UUID id) {
        return jdbc.queryForObject("select state from nite_connect.absence_justification_attachments where id=?", String.class, id);
    }

    private static byte[] pdf() {
        return "%PDF-1.4\n1 0 obj <</Type/Catalog>> endobj\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    }

    private static Path createPrivateStorage() {
        try { return Files.createTempDirectory("nite-retention-purge-test-"); }
        catch (IOException e) { throw new ExceptionInInitializerError(e); }
    }

    private static void clearPrivateStorage() throws IOException {
        if (!Files.exists(PRIVATE_STORAGE)) return;
        try (var files = Files.walk(PRIVATE_STORAGE)) {
            for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
        Files.createDirectories(PRIVATE_STORAGE);
    }

    private record AttachmentFixture(UUID id, UUID justificationId, Path objectPath) {}
}
