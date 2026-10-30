package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ParticipationGovernanceTest extends ProjectTestSupport {
    private static final String WEEK_START = "2026-10-26";
    private static final String WEEK_END = "2026-11-01";

    @Test
    void officialCalendarExceptionChangesOnlyTheAffectedWeeklyObligation() throws Exception {
        mvc.perform(post("/api/v1/attendance/calendar-exceptions").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"expectedVersion":0,"weekStart":"%s","weekEnd":"%s","type":"SUSPENSAO_PARCIAL",
                                 "presentialHoursRelieved":2.5,"remoteHoursRelieved":1.25,
                                 "reason":"Suspensão parcial registrada"}
                                """.formatted(WEEK_START, WEEK_END)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(1));

        mvc.perform(get("/api/v1/me/participation-weekly-summary").with(token(MEMBER)).param("from", WEEK_START).param("to", WEEK_END))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modalities.presential.requiredSeconds").value(9000))
                .andExpect(jsonPath("$.modalities.remote.requiredSeconds").value(9900))
                .andExpect(jsonPath("$.calendarException.version").value(1));

        mvc.perform(post("/api/v1/attendance/calendar-exceptions").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":0,"weekStart":"2026-10-27","weekEnd":"2026-11-02","type":"RECESSO_INTEGRAL",
                                 "presentialHoursRelieved":5,"remoteHoursRelieved":4,"reason":"Sobreposição"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void cancellationRecessAndReschedulingHaveDistinctWeeklyEffects() throws Exception {
        String canceled=calendarEvent("2026-10-29T18:00:00Z","Encontro cancelado");
        UUID canceledId=UUID.fromString(com.jayway.jsonpath.JsonPath.read(canceled,"$.id"));
        mvc.perform(post("/api/v1/agenda/events/"+canceledId+"/cancel").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"reason\":\"Cancelamento institucional\",\"confirmed\":true}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/attendance/calendar-exceptions").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"expectedVersion\":0,\"weekStart\":\"2026-10-26\",\"weekEnd\":\"2026-11-01\",\"type\":\"CANCELAMENTO_PRESENCIAL\",\"presentialHoursRelieved\":5,\"remoteHoursRelieved\":0,\"affectedEventId\":\"%s\",\"reason\":\"Encontro cancelado\"}").formatted(canceledId)))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/me/participation-weekly-summary").with(token(MEMBER)).param("from","2026-10-26").param("to","2026-11-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.modalities.presential.requiredSeconds").value(0))
                .andExpect(jsonPath("$.modalities.remote.requiredSeconds").value(14400));

        mvc.perform(post("/api/v1/attendance/calendar-exceptions").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"weekStart\":\"2026-11-02\",\"weekEnd\":\"2026-11-08\",\"type\":\"RECESSO_INTEGRAL\",\"presentialHoursRelieved\":5,\"remoteHoursRelieved\":4,\"reason\":\"Recesso integral\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/me/participation-weekly-summary").with(token(MEMBER)).param("from","2026-11-02").param("to","2026-11-08"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.modalities.presential.requiredSeconds").value(0))
                .andExpect(jsonPath("$.modalities.remote.requiredSeconds").value(0));

        mvc.perform(post("/api/v1/attendance/calendar-exceptions").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"weekStart\":\"2026-11-09\",\"weekEnd\":\"2026-11-15\",\"type\":\"SUSPENSAO_PRESENCIAL\",\"presentialHoursRelieved\":5,\"remoteHoursRelieved\":0,\"reason\":\"Suspensão presencial\"}"))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/me/participation-weekly-summary").with(token(MEMBER)).param("from","2026-11-09").param("to","2026-11-15"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.modalities.presential.requiredSeconds").value(0))
                .andExpect(jsonPath("$.modalities.remote.requiredSeconds").value(14400));

        String original=calendarEvent("2026-11-19T18:00:00Z","Encontro original");
        UUID originalId=UUID.fromString(com.jayway.jsonpath.JsonPath.read(original,"$.id"));
        mvc.perform(post("/api/v1/agenda/events/"+originalId+"/cancel").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"reason\":\"Remarcação oficial\",\"confirmed\":true}"))
                .andExpect(status().isOk());
        String replacement=calendarEvent("2026-11-20T18:00:00Z","Encontro remarcado");
        UUID replacementId=UUID.fromString(com.jayway.jsonpath.JsonPath.read(replacement,"$.id"));
        mvc.perform(post("/api/v1/attendance/calendar-exceptions").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"expectedVersion\":0,\"weekStart\":\"2026-11-16\",\"weekEnd\":\"2026-11-22\",\"type\":\"REMARCACAO\",\"presentialHoursRelieved\":0,\"remoteHoursRelieved\":0,\"affectedEventId\":\"%s\",\"replacementEventId\":\"%s\",\"reason\":\"Nova data registrada\"}").formatted(originalId,replacementId)))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/me/participation-weekly-summary").with(token(MEMBER)).param("from","2026-11-16").param("to","2026-11-22"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.modalities.presential.requiredSeconds").value(18000))
                .andExpect(jsonPath("$.calendarException.replacementEventId").value(replacementId.toString()));
    }

    @Test
    void calendarExceptionCannotUseCanceledMeetingFromAnotherWeek() throws Exception {
        String canceled=calendarEvent("2026-11-09T18:00:00Z","Encontro da semana anterior");
        UUID canceledId=UUID.fromString(com.jayway.jsonpath.JsonPath.read(canceled,"$.id"));
        mvc.perform(post("/api/v1/agenda/events/"+canceledId+"/cancel").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"reason\":\"Cancelamento institucional\",\"confirmed\":true}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/attendance/calendar-exceptions").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"expectedVersion\":0,\"weekStart\":\"2026-11-16\",\"weekEnd\":\"2026-11-22\",\"type\":\"CANCELAMENTO_PRESENCIAL\",\"presentialHoursRelieved\":5,\"remoteHoursRelieved\":0,\"affectedEventId\":\"%s\",\"reason\":\"Encontro cancelado\"}").formatted(canceledId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void calendarExceptionAcceptsMeetingOnTheLastDayOfItsInstitutionalWeek() throws Exception {
        String canceled=calendarEvent("2026-11-22T15:00:00Z","Encontro no limite da semana");
        UUID canceledId=UUID.fromString(com.jayway.jsonpath.JsonPath.read(canceled,"$.id"));
        mvc.perform(post("/api/v1/agenda/events/"+canceledId+"/cancel").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"reason\":\"Cancelamento institucional\",\"confirmed\":true}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/attendance/calendar-exceptions").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"expectedVersion\":0,\"weekStart\":\"2026-11-16\",\"weekEnd\":\"2026-11-22\",\"type\":\"CANCELAMENTO_PRESENCIAL\",\"presentialHoursRelieved\":5,\"remoteHoursRelieved\":0,\"affectedEventId\":\"%s\",\"reason\":\"Encontro cancelado\"}").formatted(canceledId)))
                .andExpect(status().isCreated());
    }

    @Test
    void dispensationDoesNotCreateWorkHoursAndMemberCannotReadOthersAdjustments() throws Exception {
        mvc.perform(post("/api/v1/attendance/participation-dispensations").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"memberId":"%s","weekStart":"%s","weekEnd":"%s","modality":"REMOTA",
                                 "hours":2,"reason":"Dispensa autorizada"}
                                """.formatted(MEMBER, WEEK_START, WEEK_END)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("AUTORIZADA"));

        mvc.perform(get("/api/v1/me/participation-weekly-summary").with(token(MEMBER)).param("from", WEEK_START).param("to", WEEK_END))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modalities.remote.requiredSeconds").value(7200))
                .andExpect(jsonPath("$.modalities.remote.validatedSeconds").value(0));

        mvc.perform(get("/api/v1/me/participation-adjustments").with(token(OTHER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void replacementRequiresAuthorizationAndValidatedPeriodBeforeAllocation() throws Exception {
        String response = mvc.perform(post("/api/v1/me/participation-replacements").with(token(MEMBER))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"weekStart":"2026-10-19","weekEnd":"2026-10-25","modality":"REMOTA","hours":2,"reason":"Reposição solicitada"}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn().getResponse().getContentAsString();
        UUID adjustmentId = java.util.UUID.fromString(com.jayway.jsonpath.JsonPath.read(response, "$.id"));

        mvc.perform(post("/api/v1/attendance/participation-replacements/" + adjustmentId + "/decisions").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"expectedVersion":0,"decision":"ACEITAR","authorizedModality":"REMOTA",
                                 "authorizedHours":2,"dueOn":"2026-11-15","reason":"Reposição autorizada"}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AUTORIZADA"));

        Instant start = Instant.parse("2026-10-26T15:00:00Z");
        Instant end = Instant.parse("2026-10-26T16:00:00Z");
        String period = mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startsAt":"%s","endsAt":"%s","description":"Atividade de reposição"}
                                """.formatted(start, end)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID periodId = java.util.UUID.fromString(com.jayway.jsonpath.JsonPath.read(period, "$.id"));
        mvc.perform(post("/api/v1/attendance/records/" + periodId + "/reviews").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"status\":\"APROVADA\",\"reason\":\"Validação registrada\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/attendance/participation-replacements/" + adjustmentId + "/periods/" + periodId)
                        .with(token(ADMIN)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.periodId").value(periodId.toString()));
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.participation_replacement_allocations where period_id=?", Integer.class, periodId)).isEqualTo(1);
    }

    @Test
    void weeklyReviewBatchApprovesSelectedLogsAndRecordsCorrectionRequests() throws Exception {
        Instant start = Instant.parse("2026-10-26T15:00:00Z");
        Instant end = Instant.parse("2026-10-26T16:00:00Z");
        String approved = workLog(start, end, "Aprovar");
        String correction = workLog(start.plusSeconds(7200), end.plusSeconds(7200), "Corrigir");
        UUID approvedId = java.util.UUID.fromString(com.jayway.jsonpath.JsonPath.read(approved, "$.id"));
        UUID correctionId = java.util.UUID.fromString(com.jayway.jsonpath.JsonPath.read(correction, "$.id"));

        mvc.perform(post("/api/v1/attendance/work-log-review-batches").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"weekStart":"%s","weekEnd":"%s","reason":"Validação semanal",
                                 "items":[
                                  {"periodId":"%s","expectedVersion":0,"action":"APROVAR","reason":"Período validado"},
                                  {"periodId":"%s","expectedVersion":0,"action":"CORRECAO_SOLICITADA","reason":"Descreva melhor a atividade"}
                                 ]}
                                """.formatted(WEEK_START, WEEK_END, approvedId, correctionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items[0].resultingVersion").value(1))
                .andExpect(jsonPath("$.items[1].resultingVersion").value(0));

        assertThat(jdbc.queryForObject("select review_status from nite_connect.participation_periods where id=?", String.class, approvedId)).isEqualTo("APROVADA");
        assertThat(jdbc.queryForObject("select review_status from nite_connect.participation_periods where id=?", String.class, correctionId)).isEqualTo("PENDENTE");
        mvc.perform(get("/api/v1/me/work-logs/review-requests").with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
    }

    private String workLog(Instant start, Instant end, String description) throws Exception {
        return mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startsAt":"%s","endsAt":"%s","description":"%s"}
                                """.formatted(start, end, description)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    private String calendarEvent(String startsAt,String title) throws Exception {
        Instant start=Instant.parse(startsAt);
        return mvc.perform(post("/api/v1/agenda/events").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"%s","description":"Reunião institucional","kind":"ENCONTRO","modality":"PRESENCIAL",
                                 "startsAt":"%s","endsAt":"%s","expectedMemberIds":[],"reason":"Evento de teste"}
                                """.formatted(title,start,start.plusSeconds(3600))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }
}
