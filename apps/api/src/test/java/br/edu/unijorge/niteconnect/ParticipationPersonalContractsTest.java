package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ParticipationPersonalContractsTest extends AttendanceTestSupport {
    @Test
    void absenceJustificationWorksWithoutAnAttachmentAndAttachmentRouteRemainsUnavailable() throws Exception {
        String eventBody = json.writeValueAsString(Map.of("title", "Encontro encerrado", "description", "Sessão encerrada",
                "kind", "ENCONTRO", "modality", "PRESENCIAL", "startsAt", Instant.now().minusSeconds(7200).toString(),
                "endsAt", Instant.now().minusSeconds(3600).toString(), "expectedMemberIds", java.util.List.of(MEMBER), "reason", "Agenda"));
        String eventJson = mvc.perform(post("/api/v1/agenda/events").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON).content(eventBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID eventId = UUID.fromString(json.readTree(eventJson).get("id").asString());
        String justification = mvc.perform(post("/api/v1/me/absence-justifications").with(token(MEMBER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("eventId", eventId, "reason", "Justificativa sem arquivo"))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.attachments").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        UUID justificationId = UUID.fromString(json.readTree(justification).get("id").asString());

        mvc.perform(get("/api/v1/me/absence-justifications/" + justificationId + "/attachments").with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.uploadAvailability").value("ARMAZENAMENTO_PRIVADO_NAO_CONFIGURADO"));
        mvc.perform(multipart("/api/v1/me/absence-justifications/" + justificationId + "/attachments")
                        .file(new org.springframework.mock.web.MockMultipartFile("file", "proof.pdf", "application/pdf", "%PDF-1.4 %%EOF".getBytes()))
                        .with(token(MEMBER)).param("context", "JUSTIFICATIVA"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void expectedEventsAndTimelineArePersonalPaginatedAndPreserveUnexpectedCheckIn() throws Exception {
        var event = event();
        var payload = qr(event);
        checkIn(event, payload);

        mvc.perform(get("/api/v1/me/participation/expected-events").with(token(MEMBER)).param("page", "0").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].expected").value(true))
                .andExpect(jsonPath("$.items[0].modality").value("PRESENCIAL"))
                .andExpect(jsonPath("$.items[0].checkedIn").value(true));
        mvc.perform(get("/api/v1/me/participation/expected-events").with(token(OTHER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));

        mvc.perform(get("/api/v1/me/participation/timeline").with(token(MEMBER)).param("page", "0").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].eventId").value(event.toString()))
                .andExpect(jsonPath("$.items[0].periodStatus").value("ABERTO"))
                .andExpect(jsonPath("$.items[0].durationSeconds").doesNotExist());
        mvc.perform(get("/api/v1/me/participation/timeline").with(token(OTHER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));

        mvc.perform(get("/api/v1/analytics/reports/FREQUENCIA_PARTICIPACAO").with(token(ADMIN))
                        .param("from",LocalDate.now(ZoneId.of("America/Bahia")).minusDays(1).toString())
                        .param("to",LocalDate.now(ZoneId.of("America/Bahia")).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns").value(org.hamcrest.Matchers.hasItem("periodStatus")))
                .andExpect(jsonPath("$.rows[0][4]").value("PENDENTE"))
                .andExpect(jsonPath("$.rows[0][5]").value("ABERTO"))
                .andExpect(jsonPath("$.rows[0][8]").value("0"));

        var unexpected = event();
        var unexpectedPayload = qr(unexpected);
        mvc.perform(post("/api/v1/me/attendance/check-in").with(token(OTHER)).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("eventId", unexpected, "payload", unexpectedPayload))))
                .andExpect(status().isCreated());
    }

    @Test
    void openPeriodsHaveNoDashboardDurationAndCancelledEventsStayVisibleButDoNotAggregate() throws Exception {
        var event = event();
        checkIn(event, qr(event));
        var today = LocalDate.now(ZoneId.of("America/Bahia")).toString();

        mvc.perform(get("/api/v1/me/dashboard").with(token(MEMBER)).param("from", today).param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicators['participation.presencial.seconds']").value(0))
                .andExpect(jsonPath("$.indicators['participation.open']").value(1));

        mvc.perform(post("/api/v1/agenda/events/" + event + "/cancel").with(token(ADMIN)).contentType("application/json")
                        .content("{\"expectedVersion\":1,\"reason\":\"Cancelamento sintético\",\"confirmed\":true}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/me/dashboard").with(token(MEMBER)).param("from", today).param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicators['participation.presencial.seconds']").value(0))
                .andExpect(jsonPath("$.indicators['participation.pendente']").value(0));
        mvc.perform(get("/api/v1/analytics/reports/FREQUENCIA_PARTICIPACAO").with(token(ADMIN))
                        .param("from", today).param("to", today))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/me/participation/timeline").with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].periodStatus").value("CANCELADO"));
    }

    @Test
    void weeklySummaryMarksLoadReferenceProvisionalAndPersonalCalendarHistoryIsSanitized() throws Exception {
        var start=LocalDate.now(ZoneId.of("America/Bahia"));
        var weekStart=start.minusDays((start.getDayOfWeek().getValue()+6)%7);
        var weekEnd=weekStart.plusDays(6);
        mvc.perform(get("/api/v1/me/participation-weekly-summary").with(token(MEMBER))
                        .param("from",weekStart.toString()).param("to",weekEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadPolicy.status").value("SEM_POLITICA_APLICAVEL"))
                .andExpect(jsonPath("$.loadPolicy.institutionalConfirmation.status").value("CONFIRMADA"))
                .andExpect(jsonPath("$.loadPolicy.institutionalConfirmation.presentialHours").value(5))
                .andExpect(jsonPath("$.loadPolicy.institutionalConfirmation.remoteHours").value(4))
                .andExpect(jsonPath("$.modalities.presential.requiredSeconds").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.modalities.remote.remainingSeconds").value(org.hamcrest.Matchers.nullValue()));

        mvc.perform(post("/api/v1/attendance/calendar-exceptions").with(token(ADMIN)).contentType("application/json")
                        .content("""
                                {"expectedVersion":0,"weekStart":"%s","weekEnd":"%s","type":"SUSPENSAO_PARCIAL",
                                 "presentialHoursRelieved":1,"remoteHoursRelieved":0.5,"reason":"Detalhe gerencial reservado"}
                                """.formatted(weekStart,weekEnd)))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/me/participation/calendar-exceptions").with(token(MEMBER))
                        .param("from",weekStart.toString()).param("to",weekEnd.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].type").value("SUSPENSAO_PARCIAL"))
                .andExpect(jsonPath("$.items[0].reason").doesNotExist())
                .andExpect(jsonPath("$.items[0].createdBy").doesNotExist());
        mvc.perform(get("/api/v1/me/participation/calendar-exceptions").with(token(OTHER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].reason").doesNotExist());
    }

    @Test
    void weeklySummaryUsesOnlyAFullWeekHomologatedPolicyAndKeepsTheProvisionalReferenceDistinct() throws Exception {
        LocalDate weekStart=LocalDate.now(ZoneId.of("America/Bahia"));
        weekStart=weekStart.minusDays((weekStart.getDayOfWeek().getValue()+6)%7);
        LocalDate weekEnd=weekStart.plusDays(6);
        insertLoadPolicy(1,weekStart,weekEnd.plusDays(1),"3.00","2.00");

        mvc.perform(get("/api/v1/me/participation-weekly-summary").with(token(MEMBER))
                        .param("from",weekStart.toString()).param("to",weekEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadPolicy.status").value("HOMOLOGADA_NO_PERIODO"))
                .andExpect(jsonPath("$.loadPolicy.currentConfiguration.version").value(1))
                .andExpect(jsonPath("$.modalities.presential.requiredSeconds").value(10800))
                .andExpect(jsonPath("$.modalities.remote.requiredSeconds").value(7200))
                .andExpect(jsonPath("$.loadPolicy.institutionalConfirmation.presentialHours").value(5));
    }

    @Test
    void weeklySummaryDoesNotLabelAdjacentPolicyVersionsAsOverlapping() throws Exception {
        LocalDate weekStart=LocalDate.now(ZoneId.of("America/Bahia"));
        weekStart=weekStart.minusDays((weekStart.getDayOfWeek().getValue()+6)%7);
        LocalDate weekEnd=weekStart.plusDays(6);
        insertLoadPolicy(1,weekStart,weekStart.plusDays(3),"3.00","2.00");
        insertLoadPolicy(2,weekStart.plusDays(3),weekEnd.plusDays(1),"3.00","2.00");

        mvc.perform(get("/api/v1/me/participation-weekly-summary").with(token(MEMBER))
                        .param("from",weekStart.toString()).param("to",weekEnd.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadPolicy.status").value("MULTIPLAS_CONFIGURACOES_NO_PERIODO"))
                .andExpect(jsonPath("$.modalities.presential.requiredSeconds").value(org.hamcrest.Matchers.nullValue()));
    }

    private void insertLoadPolicy(int version,LocalDate from,LocalDate until,String presential,String remote) {
        jdbc.update("""
                insert into nite_connect.participation_load_policy_versions
                    (id,version,effective_from,effective_until,presential_hours,remote_hours,source_reference,approved_by,approved_at)
                values (gen_random_uuid(),?,?,?,?,?,'Fonte sintética de teste',?,clock_timestamp())
                """,version,from,until,new java.math.BigDecimal(presential),new java.math.BigDecimal(remote),MEMBER);
    }
}
