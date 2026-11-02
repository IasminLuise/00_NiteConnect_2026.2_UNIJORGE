package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AttendanceHistoryIntegrityTest extends AttendanceTestSupport {
    @Test void retiredSegmentOperationsReturnGoneAndNewPresenceCreatesNoSegment() throws Exception {
        UUID event = event(), list = openList(event), attendance = checkIn(event, qrForList(list));
        mvc.perform(post("/api/v1/me/attendance/check-out").with(token(MEMBER)).contentType("application/json")
                .content("{\"expectedVersion\":0}"))
                .andExpect(status().isGone());
        mvc.perform(post("/api/v1/me/attendance/" + attendance + "/return").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion", 0, "payload", "legacy"))))
                .andExpect(status().isGone());
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_records", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_segments", Long.class)).isZero();
        mvc.perform(get("/api/v1/me/attendance").with(token(MEMBER))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/me/attendance").with(token(OTHER))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test void simultaneousConfirmationsAreIdempotentAndCreateOnlyOneRecord() throws Exception {
        UUID event = event(), list = openList(event);
        String body = json.writeValueAsString(Map.of("eventId", event, "payload", qrForList(list)));
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
                    .containsExactlyInAnyOrder(201, 200);
        }
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_records", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_segments", Long.class)).isZero();
    }

    @Test void eachListAllowsOnePresenceAndExpiredQrIsRejectedForAnotherMember() throws Exception {
        UUID first = event(), firstList = openList(first);
        checkIn(first, qrForList(firstList));
        UUID second = event(), secondList = openList(second);
        String secondQr = qrForList(secondList);
        mvc.perform(post("/api/v1/me/attendance/check-in").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId", second, "payload", secondQr))))
                .andExpect(status().isCreated());
        String secondHash = jdbc.queryForObject("select token_hash from nite_connect.attendance_list_qr_tokens where list_id=? order by qr_version desc limit 1", String.class, secondList);
        jdbc.update("update nite_connect.attendance_list_qr_tokens set issued_at=current_timestamp-interval '2 seconds',expires_at=current_timestamp-interval '1 second' where token_hash=?", secondHash);
        mvc.perform(get("/api/v1/attendance/lists/" + secondList).with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.qr.windowState").value("ATIVA"));
        mvc.perform(post("/api/v1/me/attendance/check-in").with(token(OTHER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId", second, "payload", secondQr))))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_records where member_id=?", Long.class, MEMBER)).isEqualTo(2);
    }

    @Test void frequencySnapshotIncludesEveryActiveMemberAndAgendaRosterRemainsDistinct() throws Exception {
        UUID event = event(), list = openList(event);
        mvc.perform(post("/api/v1/me/frequency/confirmations").with(token(OTHER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("qrToken", qrForList(list)))))
                .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/attendance/lists/" + list + "/participants").with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.items[?(@.memberId == '" + OTHER + "')].status").value("PRESENTE"));
        mvc.perform(get("/api/v1/agenda/events/" + event + "/participants").with(token(ADMIN))).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.memberId == '" + OTHER + "')].expected").value(false))
                .andExpect(jsonPath("$.items[?(@.memberId == '" + OTHER + "')].checkedIn").value(true));
        mvc.perform(get("/api/v1/attendance/lists/" + list + "/participants").with(token(MEMBER))).andExpect(status().isForbidden());
    }

    @Test void retiredParticipationAndWorkLogRoutesRemainUnavailable() throws Exception {
        mvc.perform(get("/api/v1/me/participation-weekly-summary?from=2026-10-26&to=2026-11-01").with(token(MEMBER)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/attendance/participation-load-policies").with(token(ADMIN))).andExpect(status().isForbidden());
    }
}
