package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AttendanceReviewRegressionTest extends AttendanceTestSupport {
    @Test void cancellationStillAllowsAuditedCorrectionOfExistingOpenPeriod() throws Exception {
        UUID event=event(),attendance=checkIn(event,qr(event));
        UUID period=jdbc.queryForObject("select id from nite_connect.participation_periods where attendance_id=?",UUID.class,attendance);
        mvc.perform(post("/api/v1/agenda/events/"+event+"/cancel").with(token(ADMIN)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion",ev(event),"reason","Cancelado após entrada","confirmed",true)))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/attendance/records/"+period+"/corrections").with(token(ADMIN)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion",0,"startsAt",Instant.now().minusSeconds(200).toString(),
                        "endsAt",Instant.now().minusSeconds(100).toString(),"reason","Corrigir saída esquecida","confirmed",true))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.endsAt").isNotEmpty());
        mvc.perform(post("/api/v1/attendance/records/"+period+"/reviews").with(token(ADMIN)).contentType("application/json")
                .content("{\"expectedVersion\":1,\"status\":\"APROVADA\",\"reason\":\"Tentar validar\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("startsAt",Instant.now().minusSeconds(90).toString(),"endsAt",Instant.now().minusSeconds(20).toString(),"description","Trabalho após saída corrigida"))))
                .andExpect(status().isCreated());
    }
    @Test void administrationCanClosePastMeetingWithoutQrAndThenAnalyzeMissingAttendance() throws Exception {
        String response=mvc.perform(post("/api/v1/agenda/events").with(token(ADMIN)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("title","Encontro passado","description","Sem janela aberta","kind","ENCONTRO","modality","PRESENCIAL",
                        "startsAt","2026-10-10T10:00:00Z","endsAt","2026-10-10T11:00:00Z","expectedMemberIds",List.of(MEMBER),"reason","Importar programação"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID event=UUID.fromString(json.readTree(response).get("id").asString());
        mvc.perform(post("/api/v1/agenda/events/"+event+"/close").with(token(ADMIN)).contentType("application/json")
                .content("{\"expectedVersion\":0,\"reason\":\"Sem sessão, conferir ausências\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/me/participation-summary?from=2026-10-10&to=2026-10-10").with(token(MEMBER)))
                .andExpect(jsonPath("$.absentMeetings").value(1));
    }
    @Test void missingAttendanceCannotBeCreatedByCorrectionBeforeSessionIsOpened() throws Exception {
        UUID event=event();
        mvc.perform(post("/api/v1/me/attendance/corrections").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId",event,"startsAt",Instant.now().minusSeconds(200).toString(),
                        "endsAt",Instant.now().minusSeconds(100).toString(),"reason","Não registrei entrada"))))
                .andExpect(status().isConflict());
    }
    @Test void finalConferenceIncludesUnplannedAttendeeWithoutRevealingRosterToMembers() throws Exception {
        UUID event=event();String code=qr(event);
        mvc.perform(post("/api/v1/me/attendance/check-in").with(token(OTHER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId",event,"payload",code)))).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/agenda/events/"+event+"/participants").with(token(ADMIN))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[?(@.memberId == '"+OTHER+"')].expected").value(false))
                .andExpect(jsonPath("$.items[?(@.memberId == '"+OTHER+"')].checkedIn").value(true));
        mvc.perform(get("/api/v1/agenda/events/"+event+"/participants").with(token(MEMBER))).andExpect(status().isForbidden());
    }
}
