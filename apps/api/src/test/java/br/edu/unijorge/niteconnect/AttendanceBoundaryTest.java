package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AttendanceBoundaryTest extends AttendanceTestSupport {
    @Test void agendaRequiresAdministrationAndDoesNotExposeParticipantsOrQrToMembers() throws Exception {
        UUID event=event();qr(event);
        mvc.perform(get("/api/v1/agenda/events/"+event).with(token(MEMBER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").doesNotExist()).andExpect(jsonPath("$.expectedMemberIds").doesNotExist());
        mvc.perform(post("/api/v1/agenda/events/"+event+"/session").with(token(LEADER)).contentType("application/json")
                .content("{\"expectedVersion\":1,\"reason\":\"Abrir\"}")).andExpect(status().isForbidden());
    }
    @Test void qrCheckInIsUniqueAndRenewalInvalidatesPreviousPayload() throws Exception {
        UUID event=event();String old=qr(event);String current=qr(event);
        mvc.perform(post("/api/v1/me/attendance/check-in").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId",event,"payload",old)))).andExpect(status().isConflict());
        checkIn(event,current);
        mvc.perform(post("/api/v1/me/attendance/check-in").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId",event,"payload",current)))).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/me/attendance").with(token(OTHER))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/attendance/records").with(token(LEADER))).andExpect(status().isForbidden());
    }
    @Test void checkOutWithoutEntryAndInactiveMemberAreDenied() throws Exception {
        mvc.perform(post("/api/v1/me/attendance/check-out").with(token(MEMBER)).contentType("application/json")
                .content("{\"expectedVersion\":0}")).andExpect(status().isConflict());
        UUID event=event();String qr=qr(event);
        jdbc.update("update nite_connect.members set status='AFASTADO' where id=?",MEMBER);
        mvc.perform(post("/api/v1/me/attendance/check-in").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId",event,"payload",qr)))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/me/attendance").with(token(MEMBER))).andExpect(status().isForbidden());
    }
}
