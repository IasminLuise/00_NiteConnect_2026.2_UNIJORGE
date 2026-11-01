package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JustificationAttachmentsTest extends AttendanceTestSupport {
    private static final Path PRIVATE_STORAGE = createPrivateStorage();

    @DynamicPropertySource
    static void configureStorage(DynamicPropertyRegistry properties) {
        properties.add("nite.justification.local-root", () -> PRIVATE_STORAGE.toString());
        properties.add("nite.justification.storage.approved", () -> true);
        properties.add("nite.justification.retention.authorized-validator-ids", MEMBER::toString);
    }

    @AfterAll
    static void removePrivateTestStorage() throws IOException {
        if (Files.exists(PRIVATE_STORAGE)) {
            try (var paths = Files.walk(PRIVATE_STORAGE)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    @Test @Order(1)
    void storageConfiguredButUnvalidatedRetentionKeepsUploadsUnavailable() throws Exception {
        mvc.perform(post("/api/v1/attendance/justification-attachment-retention-policies").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"retentionMonths\":24,\"deletionGraceDays\":31,\"reason\":\"Fora da janela máxima\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/attendance/justification-attachment-retention-policies").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"retentionMonths\":24,\"deletionGraceDays\":29,\"reason\":\"Abaixo da carência mínima\"}"))
                .andExpect(status().isBadRequest());
        UUID justification = justification("Sem anexo ainda");
        mvc.perform(get("/api/v1/me/absence-justifications/" + justification + "/attachments").with(token(MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadAvailability").value("RETENCAO_AGUARDANDO_VALIDACAO"))
                .andExpect(jsonPath("$.items").isEmpty());
        upload(justification, "JUSTIFICATIVA", "proof.pdf", "application/pdf", pdf())
                .andExpect(status().isServiceUnavailable());
    }

    @Test @Order(2)
    void optionalPrivatePdfAndImagesAreValidatedAndOnlyOwnerOrAuthorizedReviewersCanDownload() throws Exception {
        UUID justification = justification("Comprovantes opcionais");
        UUID policy = validatedRetentionPolicy();
        String pdfResult = upload(justification, "JUSTIFICATIVA", "comprovante.pdf", "application/pdf", pdf())
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DISPONIVEL"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.downloadable").value(true)).andReturn().getResponse().getContentAsString();
        UUID pdfId = UUID.fromString(json.readTree(pdfResult).get("id").asString());
        UUID pngId = uploadImage(justification, "image.png", "image/png", true);
        UUID jpegId = uploadImage(justification, "image.jpg", "image/jpeg", false);

        mvc.perform(get("/api/v1/me/absence-justifications/" + justification + "/attachments").with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.uploadAvailability").value("DISPONIVEL"))
                .andExpect(jsonPath("$.items.length()").value(3));
        mvc.perform(get("/api/v1/me/absence-justifications/" + justification + "/attachments/" + pdfId + "/download").with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "private, no-store"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().contentType("application/pdf"));
        mvc.perform(get("/api/v1/attendance/justifications/" + justification + "/attachments/" + pngId + "/download").with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"));
        mvc.perform(get("/api/v1/attendance/justifications/" + justification + "/attachments/" + jpegId + "/download").with(token(LEADER)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/me/absence-justifications/" + justification + "/attachments/" + pdfId + "/download").with(token(OTHER)))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select retention_policy_id from nite_connect.absence_justification_attachments where id=?",
                UUID.class, pdfId)).isEqualTo(policy);
        assertThat(Files.exists(PRIVATE_STORAGE.resolve(pdfId + ".object"))).isTrue();
    }

    @Test @Order(3)
    void invalidContentAndMoreThanFiveMillionBytesAreRejected() throws Exception {
        UUID justification = justification("Validação de arquivo");
        validatedRetentionPolicy();
        upload(justification, "JUSTIFICATIVA", "imagem.png", "image/png", "não é PNG".getBytes())
                .andExpect(status().isUnsupportedMediaType());
        upload(justification, "JUSTIFICATIVA", "arquivo.txt", "text/plain", "texto".getBytes())
                .andExpect(status().isUnsupportedMediaType());
        byte[] tooLarge = new byte[5_000_001];
        System.arraycopy(pdf(), 0, tooLarge, 0, pdf().length);
        upload(justification, "JUSTIFICATIVA", "grande.pdf", "application/pdf", tooLarge)
                .andExpect(status().isPayloadTooLarge());
        mvc.perform(get("/api/v1/me/absence-justifications/" + justification + "/attachments").with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
        byte[] exactLimit = new byte[5_000_000];
        java.util.Arrays.fill(exactLimit, (byte) ' ');
        System.arraycopy(pdf(), 0, exactLimit, 0, pdf().length);
        upload(justification, "JUSTIFICATIVA", "limite.pdf", "application/pdf", exactLimit)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sizeBytes").value(5_000_000));
    }

    @Test @Order(4)
    void complementAttachmentsUseAnExplicitContextAndCannotBeReadAcrossMembers() throws Exception {
        UUID justification = justification("Complementação");
        validatedRetentionPolicy();
        mvc.perform(post("/api/v1/attendance/justifications/" + justification + "/decisions").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"decision\":\"COMPLEMENTACAO_SOLICITADA\",\"reason\":\"Contexto adicional\"}"))
                .andExpect(status().isOk());
        upload(justification, "COMPLEMENTACAO", "complemento.pdf", "application/pdf", pdf())
                .andExpect(status().isCreated()).andExpect(jsonPath("$.context").value("COMPLEMENTACAO"));
        mvc.perform(get("/api/v1/me/absence-justifications/" + justification + "/attachments").with(token(OTHER)))
                .andExpect(status().isNotFound());
    }

    @Test @Order(5)
    void retentionClassificationHoldAndConfirmedDeletionAreAuditableAndControlled() throws Exception {
        UUID justification = justification("Retenção e descarte controlado");
        validatedRetentionPolicy();
        String uploaded = upload(justification, "JUSTIFICATIVA", "retencao.pdf", "application/pdf", pdf())
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID attachmentId = UUID.fromString(json.readTree(uploaded).get("id").asString());
        mvc.perform(post("/api/v1/attendance/justifications/" + justification + "/decisions").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"decision\":\"ACEITA\",\"reason\":\"Análise encerrada\"}"))
                .andExpect(status().isOk());
        jdbc.update("update nite_connect.absence_justifications set decided_at=? where id=?",
                java.sql.Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")), justification);
        mvc.perform(post("/api/v1/attendance/justifications/" + justification + "/retention-classifications").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semesterEndOn\":\"2020-01-31\",\"sourceReference\":\"Calendário oficial arquivado\",\"reason\":\"Classificação de retenção\"}"))
                .andExpect(status().isOk());
        String hold = mvc.perform(post("/api/v1/attendance/justification-attachments/" + attachmentId + "/retention-holds").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"basis\":\"Apuração formal em curso\",\"reason\":\"Conservação excepcional\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        UUID holdId = UUID.fromString(json.readTree(hold).get("id").asString());
        mvc.perform(get("/api/v1/attendance/justification-attachments/" + attachmentId + "/retention-holds").with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].type").value("APLICADO"))
                .andExpect(jsonPath("$[0].holdId").value(holdId.toString()))
                .andExpect(jsonPath("$[0].actorId").value(ADMIN.toString()))
                .andExpect(jsonPath("$[0].basis").value("Apuração formal em curso"));
        mvc.perform(get("/api/v1/attendance/justification-attachments/" + attachmentId + "/retention-holds").with(token(MEMBER)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/attendance/justification-attachments/" + attachmentId + "/deletions").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"confirmed\":true,\"reason\":\"Tentativa protegida\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/attendance/justification-attachments/" + attachmentId + "/retention-holds/" + holdId + "/releases").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Apuração encerrada\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/attendance/justification-attachments/" + attachmentId + "/retention-holds").with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].type").value("LIBERADO"))
                .andExpect(jsonPath("$[1].reason").value("Apuração encerrada"));
        mvc.perform(post("/api/v1/attendance/justification-attachments/" + attachmentId + "/deletions").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"confirmed\":true,\"reason\":\"Prazo vencido e descarte autorizado\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REMOVIDO"))
                .andExpect(jsonPath("$.filename").doesNotExist()).andExpect(jsonPath("$.downloadable").value(false));
        assertThat(Files.exists(PRIVATE_STORAGE.resolve(attachmentId + ".object"))).isFalse();
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.absence_justification_attachment_events where attachment_id=? and event_type='EXCLUIDO'",
                Integer.class, attachmentId)).isEqualTo(1);
    }

    @Test @Order(6)
    void manualDeletionWaitsUntilThirtyDaysAfterRetentionExpires() throws Exception {
        UUID justification = justification("Carência mínima antes do descarte");
        validatedRetentionPolicy();
        String uploaded = upload(justification, "JUSTIFICATIVA", "carencia.pdf", "application/pdf", pdf())
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID attachmentId = UUID.fromString(json.readTree(uploaded).get("id").asString());
        mvc.perform(post("/api/v1/attendance/justifications/" + justification + "/decisions").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"decision\":\"ACEITA\",\"reason\":\"Análise encerrada\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/attendance/justifications/" + justification + "/retention-classifications").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semesterEndOn\":\"2020-01-31\",\"sourceReference\":\"Calendário oficial arquivado\","
                                + "\"reason\":\"Classificação de retenção\"}"))
                .andExpect(status().isOk());

        LocalDate today = LocalDate.now(ZoneId.of("America/Bahia"));
        LocalDate nextDay = today.plusDays(1);
        LocalDate decisionDate = decisionDateForDeletionDeadline(nextDay);
        jdbc.update("update nite_connect.absence_justifications set decided_at=? where id=?",
                java.sql.Timestamp.from(decisionDate.atStartOfDay(ZoneId.of("America/Bahia")).toInstant()), justification);

        mvc.perform(get("/api/v1/me/absence-justifications/" + justification + "/attachments").with(token(MEMBER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].deletionDeadlineOn").value(nextDay.toString()));
        mvc.perform(post("/api/v1/attendance/justification-attachments/" + attachmentId + "/deletions").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"confirmed\":true,\"reason\":\"Carência ainda vigente\"}"))
                .andExpect(status().isConflict());

        decisionDate = decisionDateForDeletionDeadline(today);
        jdbc.update("update nite_connect.absence_justifications set decided_at=? where id=?",
                java.sql.Timestamp.from(decisionDate.atStartOfDay(ZoneId.of("America/Bahia")).toInstant()), justification);
        mvc.perform(post("/api/v1/attendance/justification-attachments/" + attachmentId + "/deletions").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"confirmed\":true,\"reason\":\"Carência mínima concluída\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REMOVIDO"));
    }

    @Test @Order(7)
    void limitsAttachmentsToFivePerJustificationAndTenPerMember() throws Exception {
        validatedRetentionPolicy();
        UUID first = justification("Cinco comprovantes na primeira justificativa");
        for (int index = 0; index < 5; index++) {
            upload(first, "JUSTIFICATIVA", "primeiro-" + index + ".pdf", "application/pdf", pdf())
                    .andExpect(status().isCreated());
        }
        upload(first, "JUSTIFICATIVA", "sexto.pdf", "application/pdf", pdf()).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/me/absence-justifications/" + first + "/attachments").with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(5));

        UUID second = justification("Cinco comprovantes na segunda justificativa");
        for (int index = 0; index < 5; index++) {
            upload(second, "JUSTIFICATIVA", "segundo-" + index + ".pdf", "application/pdf", pdf())
                    .andExpect(status().isCreated());
        }
        UUID third = justification("Limite total do integrante");
        upload(third, "JUSTIFICATIVA", "decimo-primeiro.pdf", "application/pdf", pdf())
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/me/absence-justifications/" + second + "/attachments").with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(5));
    }

    @Test @Order(8)
    void failedUploadReleasesQuotaAndIsExcludedFromTheActiveAttachmentList() throws Exception {
        UUID justification = justification("Tentativas falhas não consomem vaga");
        validatedRetentionPolicy();
        for (int attempt = 0; attempt < 6; attempt++) {
            upload(justification, "JUSTIFICATIVA", "falha-" + attempt + ".txt", "text/plain", "inválido".getBytes())
                    .andExpect(status().isUnsupportedMediaType());
        }
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.absence_justification_attachments "
                + "where justification_id=? and state='FALHA'", Integer.class, justification)).isEqualTo(6);
        upload(justification, "JUSTIFICATIVA", "válido.pdf", "application/pdf", pdf()).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/me/absence-justifications/" + justification + "/attachments").with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test @Order(9)
    void retentionClassificationCannotChangeWhileAttachmentDeletionIsPending() throws Exception {
        UUID justification = justification("Classificação bloqueada durante descarte");
        validatedRetentionPolicy();
        String uploaded = upload(justification, "JUSTIFICATIVA", "classificacao.pdf", "application/pdf", pdf())
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID attachmentId = UUID.fromString(json.readTree(uploaded).get("id").asString());
        mvc.perform(post("/api/v1/attendance/justifications/" + justification + "/decisions").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"decision\":\"ACEITA\",\"reason\":\"Análise encerrada\"}"))
                .andExpect(status().isOk());
        jdbc.update("update nite_connect.absence_justifications set decided_at=? where id=?",
                java.sql.Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")), justification);
        jdbc.update("update nite_connect.absence_justification_attachments set state='EXCLUSAO_PENDENTE',version=version+1 where id=?",
                attachmentId);

        mvc.perform(post("/api/v1/attendance/justifications/" + justification + "/retention-classifications").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"semesterEndOn\":\"2020-01-31\",\"sourceReference\":\"Calendário oficial\","
                                + "\"reason\":\"Atualização durante descarte\"}"))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.absence_justification_retention_classifications where justification_id=?",
                Integer.class, justification)).isZero();
    }

    @Test @Order(10)
    void interruptedUploadReservationIsCleanedAndReleasedBeforeTheNextUpload() throws Exception {
        UUID justification = justification("Recuperação de tentativa interrompida");
        UUID policy = validatedRetentionPolicy();
        UUID abandonedId = UUID.randomUUID();
        jdbc.update("""
                insert into nite_connect.absence_justification_attachments(
                  id,justification_id,member_id,retention_policy_id,object_key,context,filename,size_bytes,state,uploaded_by,created_at)
                values (?,?,?,?,?,'JUSTIFICATIVA','interrompido.pdf',32,'PENDENTE',?,?)
                """, abandonedId, justification, MEMBER, policy, abandonedId, MEMBER,
                java.sql.Timestamp.from(Instant.now()));
        jdbc.update("""
                insert into nite_connect.absence_justification_attachment_events(id,attachment_id,event_type,actor_id,occurred_at)
                values (?,?, 'UPLOAD_INICIADO', ?, ?)
                """, UUID.randomUUID(), abandonedId, MEMBER, java.sql.Timestamp.from(Instant.now()));
        Path abandonedObject = PRIVATE_STORAGE.resolve(abandonedId + ".object");
        Files.write(abandonedObject, "partial upload".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        upload(justification, "JUSTIFICATIVA", "novo.pdf", "application/pdf", pdf()).andExpect(status().isCreated());

        assertThat(Files.exists(abandonedObject)).isFalse();
        assertThat(jdbc.queryForObject("select state from nite_connect.absence_justification_attachments where id=?",
                String.class, abandonedId)).isEqualTo("FALHA");
        assertThat(jdbc.queryForObject("select size_bytes from nite_connect.absence_justification_attachments where id=?",
                Long.class, abandonedId)).isNull();
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.absence_justification_attachments "
                + "where justification_id=? and state='DISPONIVEL'", Integer.class, justification)).isEqualTo(1);
    }

    private org.springframework.test.web.servlet.ResultActions upload(UUID justification, String context,
            String filename, String contentType, byte[] bytes) throws Exception {
        return mvc.perform(multipart("/api/v1/me/absence-justifications/" + justification + "/attachments")
                .file(new MockMultipartFile("file", filename, contentType, bytes))
                .param("context", context).with(token(MEMBER)));
    }

    private UUID uploadImage(UUID justification, String filename, String contentType, boolean png) throws Exception {
        String body = upload(justification, "JUSTIFICATIVA", filename, contentType, image(png))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(body).get("id").asString());
    }

    private UUID justification(String reason) throws Exception {
        String eventBody = json.writeValueAsString(Map.of("title", "Encontro encerrado", "description", "Sessão encerrada",
                "kind", "ENCONTRO", "modality", "PRESENCIAL", "startsAt", Instant.now().minusSeconds(7200).toString(),
                "endsAt", Instant.now().minusSeconds(3600).toString(), "expectedMemberIds", List.of(MEMBER), "reason", "Agenda"));
        String event = mvc.perform(post("/api/v1/agenda/events").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON).content(eventBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID eventId = UUID.fromString(json.readTree(event).get("id").asString());
        String body = mvc.perform(post("/api/v1/me/absence-justifications").with(token(MEMBER))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("eventId", eventId, "reason", reason))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(body).get("id").asString());
    }

    private UUID validatedRetentionPolicy() throws Exception {
        String proposal = mvc.perform(post("/api/v1/attendance/justification-attachment-retention-policies").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"retentionMonths\":24,\"deletionGraceDays\":30,\"reason\":\"Proposta institucional sujeita à validação de privacidade\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("CRIADA"))
                .andReturn().getResponse().getContentAsString();
        UUID policyId = UUID.fromString(json.readTree(proposal).get("id").asString());
        mvc.perform(post("/api/v1/attendance/justification-attachment-retention-policies").with(token(MEMBER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"retentionMonths\":24,\"deletionGraceDays\":30,\"reason\":\"Não autorizado\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/attendance/justification-attachment-retention-policies/" + policyId + "/validations").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"sourceReference\":\"Validação formal\","
                                + "\"backupHandlingReference\":\"Tratamento de backup validado\","
                                + "\"sensitiveDataHandlingReference\":\"Tratamento de dados sensíveis validado\","
                                + "\"reason\":\"Política validada em teste\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/attendance/justification-attachment-retention-policies/" + policyId + "/validations").with(token(MEMBER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"sourceReference\":\"Validação formal\","
                                + "\"backupHandlingReference\":\"Tratamento de backup validado\","
                                + "\"sensitiveDataHandlingReference\":\"Tratamento de dados sensíveis validado\","
                                + "\"reason\":\"Política validada em teste\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("VALIDADA"));
        return policyId;
    }

    private static LocalDate decisionDateForDeletionDeadline(LocalDate deadline) {
        LocalDate candidate = deadline.minusDays(30).minusMonths(24);
        for (int offset = -5; offset <= 5; offset++) {
            LocalDate date = candidate.plusDays(offset);
            if (date.plusMonths(24).plusDays(30).equals(deadline)) return date;
        }
        throw new IllegalStateException("Não foi possível calcular a data-limite de descarte");
    }

    private static byte[] pdf() { return "%PDF-1.4\n1 0 obj <</Type/Catalog>> endobj\n%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII); }
    private static byte[] image(boolean png) throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, png ? "png" : "jpeg", output);
        return output.toByteArray();
    }
    private static Path createPrivateStorage() {
        try { return Files.createTempDirectory("nite-justification-test-"); }
        catch (IOException e) { throw new ExceptionInInitializerError(e); }
    }
}
