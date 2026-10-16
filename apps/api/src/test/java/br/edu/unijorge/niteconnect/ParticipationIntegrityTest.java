package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ParticipationIntegrityTest extends AttendanceTestSupport {
    String workBody(String start,String end) { return "{\"startsAt\":\""+start+"\",\"endsAt\":\""+end+"\",\"description\":\"Contribuição\"}"; }
    @Test void rejectsOverlappingAndFutureWorkButAllowsAdjacentIntervals() throws Exception {
        mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType("application/json").content(workBody("2026-10-10T10:00:00Z","2026-10-10T11:00:00Z"))).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType("application/json").content(workBody("2026-10-10T10:30:00Z","2026-10-10T11:30:00Z"))).andExpect(status().isConflict());
        mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType("application/json").content(workBody("2026-10-10T11:00:00Z","2026-10-10T12:00:00Z"))).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType("application/json").content(workBody("2030-01-01T10:00:00Z","2030-01-01T11:00:00Z"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType("application/json").content(workBody("2026-10-10T12:00:00Z","2026-10-10T11:00:00Z"))).andExpect(status().isBadRequest());
    }
    @Test void departureAndReturnPreserveOneAttendanceAndTwoPeriods() throws Exception {
        UUID event=event(),attendance=checkIn(event,qr(event));
        mvc.perform(post("/api/v1/me/attendance/check-out").with(token(MEMBER)).contentType("application/json").content("{\"expectedVersion\":0}")).andExpect(status().isOk());
        mvc.perform(post("/api/v1/me/attendance/"+attendance+"/return").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion",1,"payload",qr(event))))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_records",Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.participation_periods",Long.class)).isEqualTo(2);
        mvc.perform(post("/api/v1/me/attendance/"+attendance+"/return").with(token(OTHER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion",2,"payload",qr(event))))).andExpect(status().isNotFound());
    }
    @Test void simultaneousCheckInsCreateOnlyOneRecord() throws Exception {
        UUID event=event();String payload=qr(event),body=json.writeValueAsString(Map.of("eventId",event,"payload",payload));
        var latch=new CountDownLatch(1);
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<Integer> task=()->{latch.await();return mvc.perform(post("/api/v1/me/attendance/check-in").with(token(MEMBER)).contentType("application/json").content(body)).andReturn().getResponse().getStatus();};
            var a=pool.submit(task);var b=pool.submit(task);latch.countDown();
            assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
        }
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_records",Long.class)).isEqualTo(1);
    }
    @Test void cannotOpenTwoMeetingsAtOnceForSameMember() throws Exception {
        UUID first=event();checkIn(first,qr(first));UUID second=event();String code=qr(second);
        mvc.perform(post("/api/v1/me/attendance/check-in").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId",second,"payload",code)))).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_records",Long.class)).isEqualTo(1);
    }
    @Test void closedSessionAndExpiredQrCannotRegisterPresence() throws Exception {
        UUID event=event();String payload=qr(event);
        jdbc.update("update nite_connect.calendar_events set qr_expires_at=current_timestamp-interval '1 second' where id=?",event);
        mvc.perform(post("/api/v1/me/attendance/check-in").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId",event,"payload",payload)))).andExpect(status().isConflict());
        payload=qr(event);
        mvc.perform(post("/api/v1/agenda/events/"+event+"/close").with(token(ADMIN)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion",ev(event),"reason","Encerrar")))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/me/attendance/check-in").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId",event,"payload",payload)))).andExpect(status().isConflict());
    }
    @Test void openPresenceHasNoConsolidatedDuration() throws Exception {
        UUID event=event();checkIn(event,qr(event));String date=LocalDate.now(ZoneId.of("America/Bahia")).toString();
        mvc.perform(get("/api/v1/me/participation-summary?from="+date+"&to="+date).with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.registeredSeconds").value(0)).andExpect(jsonPath("$.openPeriods").value(1));
    }
    @Test void inaccessibleActivityAndOtherAuthorsEvidenceAreRejected() throws Exception {
        UUID other=activity(OTHER,null,OTHER);
        String body=workBody("2026-10-10T10:00:00Z","2026-10-10T11:00:00Z");
        mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType("application/json")
                .content(body.substring(0,body.length()-1)+",\"activityId\":\""+other+"\"}")).andExpect(status().isNotFound());
        UUID own=activity(MEMBER,null,MEMBER);
        mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType("application/json")
                .content(body.substring(0,body.length()-1)+",\"activityId\":\""+own+"\"}")).andExpect(status().isCreated());
    }
    @Test void historyAndCountsAreIsolatedAndInvalidPaginationRejected() throws Exception {
        mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType("application/json").content(workBody("2026-10-10T10:00:00Z","2026-10-10T11:00:00Z"))).andExpect(status().isCreated());
        mvc.perform(get("/api/v1/me/work-logs?memberId="+MEMBER).with(token(OTHER))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/me/participation-history?memberId="+MEMBER).with(token(OTHER))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/me/work-logs?size=101").with(token(MEMBER))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/me/participation-summary?from=2026-10-11&to=2026-10-10").with(token(MEMBER))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/me/participation-summary?from=2026-10-01&to=2026-10-01").with(token(MEMBER)))
                .andExpect(jsonPath("$.registeredSeconds").value(0)).andExpect(jsonPath("$.expectedMeetings").value(0));
    }
    @Test void databaseRejectsDeletionAndSilentRewriteOfClosedPeriod() throws Exception {
        mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType("application/json").content(workBody("2026-10-10T10:00:00Z","2026-10-10T11:00:00Z"))).andExpect(status().isCreated());
        assertThatThrownBy(()->jdbc.update("delete from nite_connect.participation_periods")).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(()->jdbc.update("update nite_connect.participation_periods set ends_at=ends_at+interval '1 hour',version=version+1")).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
}
