package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AttendanceHistoryIntegrityTest extends AttendanceTestSupport {
    @Test void departureAndReturnPreserveOnePersonalAttendanceAndOnlyPresenceSegments() throws Exception {
        UUID event = event(), attendance = checkIn(event, qr(event));
        mvc.perform(post("/api/v1/me/attendance/check-out").with(token(MEMBER)).contentType("application/json")
                .content("{\"expectedVersion\":0}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.periods[0].endsAt").isNotEmpty());
        mvc.perform(post("/api/v1/me/attendance/" + attendance + "/return").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion", 1, "payload", qr(event)))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.periods.length()").value(2))
                .andExpect(jsonPath("$.periods[1].endsAt").doesNotExist())
                .andExpect(jsonPath("$.periods[0].durationSeconds").doesNotExist())
                .andExpect(jsonPath("$.periods[0].modality").doesNotExist());
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_records", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.participation_periods where attendance_id=?", Long.class, attendance)).isEqualTo(2);
        mvc.perform(get("/api/v1/me/attendance").with(token(OTHER))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(post("/api/v1/me/attendance/" + attendance + "/return").with(token(OTHER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion", 2, "payload", qr(event))))).andExpect(status().isNotFound());
    }

    @Test void simultaneousCheckInsCreateOnlyOneRecord() throws Exception {
        UUID event = event();
        String body = json.writeValueAsString(Map.of("eventId", event, "payload", qr(event)));
        var latch = new CountDownLatch(1);
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<Integer> task = () -> {
                latch.await();
                return mvc.perform(post("/api/v1/me/attendance/check-in").with(token(MEMBER)).contentType("application/json")
                        .content(body)).andReturn().getResponse().getStatus();
            };
            var first = pool.submit(task);
            var second = pool.submit(task);
            latch.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_records", Long.class)).isEqualTo(1);
    }

    @Test void oneMemberCannotKeepTwoSessionsOpenAndExpiredQrIsRejected() throws Exception {
        UUID first = event();
        String firstQr = qr(first);
        checkIn(first, firstQr);
        UUID second = event();
        String secondQr = qr(second);
        mvc.perform(post("/api/v1/me/attendance/check-in").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId", second, "payload", secondQr))))
                .andExpect(status().isConflict());
        jdbc.update("update nite_connect.calendar_events set qr_expires_at=current_timestamp-interval '1 second' where id=?", second);
        mvc.perform(post("/api/v1/me/attendance/check-in").with(token(OTHER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId", second, "payload", secondQr))))
                .andExpect(status().isConflict());
    }

    @Test void unplannedAttendeeIsRecordedAndRosterRemainsAdministrative() throws Exception {
        UUID event = event();
        String code = qr(event);
        mvc.perform(post("/api/v1/me/attendance/check-in").with(token(OTHER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId", event, "payload", code))))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/agenda/events/" + event + "/participants").with(token(ADMIN))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[?(@.memberId == '" + OTHER + "')].expected").value(false))
                .andExpect(jsonPath("$.items[?(@.memberId == '" + OTHER + "')].checkedIn").value(true));
        mvc.perform(get("/api/v1/agenda/events/" + event + "/participants").with(token(MEMBER))).andExpect(status().isForbidden());
    }

    @Test void legacyWorkLogAndAdministrativeFrequencyEndpointsAreNoLongerExposed() throws Exception {
        mvc.perform(get("/api/v1/me/participation-weekly-summary?from=2026-10-26&to=2026-11-01").with(token(MEMBER)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType("application/json")
                .content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/attendance/participation-load-policies").with(token(ADMIN)))
                .andExpect(status().isForbidden());
    }
}
