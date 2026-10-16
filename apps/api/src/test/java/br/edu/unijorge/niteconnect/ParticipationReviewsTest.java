package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ParticipationReviewsTest extends AttendanceTestSupport {
    @Test void approvedMissingAttendanceRequestCreatesPendingPresenceAndKeepsOriginalRequest() throws Exception {
        UUID event=event();qr(event);
        mvc.perform(post("/api/v1/agenda/events/"+event+"/close").with(token(ADMIN)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion",ev(event),"reason","Conferência")))).andExpect(status().isOk());
        String response=mvc.perform(post("/api/v1/me/attendance/corrections").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId",event,"startsAt",java.time.Instant.now().minusSeconds(200).toString(),
                        "endsAt",java.time.Instant.now().minusSeconds(100).toString(),"reason","Esqueci check-in"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id=json.readTree(response).get("id").asString();
        mvc.perform(post("/api/v1/attendance/corrections/"+id+"/decisions").with(token(ADMIN)).contentType("application/json")
                .content("{\"expectedVersion\":0,\"decision\":\"ACEITA\",\"reason\":\"Conferido presencialmente\",\"confirmed\":true}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/me/attendance").with(token(MEMBER))).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].periods[0].reviewStatus").value("PENDENTE"));
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.participation_events where entity_type='CORRECTION'",Long.class)).isEqualTo(2);
    }
    @Test void administratorsCannotApproveOwnHoursAndCorrectionRequiresExplicitConfirmation() throws Exception {
        String response=mvc.perform(post("/api/v1/me/work-logs").with(token(ADMIN)).contentType("application/json")
                .content("{\"startsAt\":\"2026-10-10T10:00:00Z\",\"endsAt\":\"2026-10-10T11:00:00Z\",\"description\":\"Trabalho\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id=json.readTree(response).get("id").asString();
        mvc.perform(post("/api/v1/attendance/records/"+id+"/reviews").with(token(ADMIN)).contentType("application/json")
                .content("{\"expectedVersion\":0,\"status\":\"APROVADA\",\"reason\":\"Autoaprovar\"}")).andExpect(status().isForbidden());
        UUID member=work("2026-10-10T12:00:00Z","2026-10-10T13:00:00Z");
        mvc.perform(post("/api/v1/attendance/records/"+member+"/corrections").with(token(ADMIN)).contentType("application/json")
                .content("{\"expectedVersion\":0,\"startsAt\":\"2026-10-10T12:00:00Z\",\"endsAt\":\"2026-10-10T14:00:00Z\",\"reason\":\"Corrigir\"}"))
                .andExpect(status().isBadRequest());
    }
    UUID work(String start,String end) throws Exception {
        String response=mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("startsAt",start,"endsAt",end,"description","Contribuição remota"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).get("id").asString());
    }
    @Test void remoteDeclarationNeedsReviewAndDoesNotAuthorizeProjectLeader() throws Exception {
        UUID id=work("2026-10-10T02:00:00Z","2026-10-10T04:00:00Z");
        mvc.perform(get("/api/v1/me/participation-summary?from=2026-10-09&to=2026-10-09").with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.registeredSeconds").value(3600)).andExpect(jsonPath("$.validatedSeconds").value(0));
        String body="{\"expectedVersion\":0,\"status\":\"APROVADA\",\"reason\":\"Conferido\"}";
        mvc.perform(post("/api/v1/attendance/records/"+id+"/reviews").with(token(LEADER)).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/attendance/records/"+id+"/reviews").with(token(ADMIN)).contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/me/participation-summary?from=2026-10-10&to=2026-10-10").with(token(MEMBER)))
                .andExpect(jsonPath("$.validatedSeconds").value(3600));
        mvc.perform(post("/api/v1/attendance/records/"+id+"/reviews").with(token(ADMIN)).contentType("application/json").content(body)).andExpect(status().isConflict());
    }
    @Test void correctionPreservesOriginalAndResetsValidation() throws Exception {
        UUID id=work("2026-10-10T12:00:00Z","2026-10-10T13:00:00Z");
        mvc.perform(post("/api/v1/attendance/records/"+id+"/reviews").with(token(ADMIN)).contentType("application/json")
                .content("{\"expectedVersion\":0,\"status\":\"APROVADA\",\"reason\":\"Conferência\"}")).andExpect(status().isOk());
        String response=mvc.perform(post("/api/v1/me/attendance/corrections").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("periodId",id,"startsAt","2026-10-10T12:00:00Z","endsAt","2026-10-10T14:00:00Z","reason","Saída correta"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String request=json.readTree(response).get("id").asString();
        mvc.perform(post("/api/v1/attendance/corrections/"+request+"/decisions").with(token(ADMIN)).contentType("application/json")
                .content("{\"expectedVersion\":0,\"expectedPeriodVersion\":1,\"decision\":\"ACEITA\",\"reason\":\"Confirmado\",\"confirmed\":true}"))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select review_status from nite_connect.participation_periods where id=?",String.class,id)).isEqualTo("PENDENTE");
        String before=jdbc.queryForObject("select before_value from nite_connect.participation_events where entity_id=? and action='CORRECTED'",String.class,id);
        assertThat(before).contains("2026-10-10T13:00:00Z");
        mvc.perform(get("/api/v1/me/attendance/corrections").with(token(OTHER))).andExpect(jsonPath("$.total").value(0));
    }
    @Test void justificationCanBeComplementedAndAcceptanceNeverCreatesHours() throws Exception {
        UUID event=event();qr(event);
        mvc.perform(post("/api/v1/agenda/events/"+event+"/close").with(token(ADMIN)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion",ev(event),"reason","Conferência final")))).andExpect(status().isOk());
        String response=mvc.perform(post("/api/v1/me/absence-justifications").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId",event,"reason","Impedimento pessoal"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id=json.readTree(response).get("id").asString();
        mvc.perform(post("/api/v1/attendance/justifications/"+id+"/decisions").with(token(ADMIN)).contentType("application/json")
                .content("{\"expectedVersion\":0,\"decision\":\"COMPLEMENTACAO_SOLICITADA\",\"reason\":\"Esclarecer contexto\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/me/absence-justifications/"+id+"/complements").with(token(MEMBER)).contentType("application/json")
                .content("{\"expectedVersion\":1,\"reason\":\"Esclarecimento pessoal\"}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/attendance/justifications/"+id+"/decisions").with(token(ADMIN)).contentType("application/json")
                .content("{\"expectedVersion\":2,\"decision\":\"ACEITA\",\"reason\":\"Analisado\"}")).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.participation_periods",Long.class)).isZero();
        mvc.perform(get("/api/v1/me/absence-justifications").with(token(OTHER))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/me/participation-history").with(token(MEMBER))).andExpect(jsonPath("$.total").value(4));
    }
}
