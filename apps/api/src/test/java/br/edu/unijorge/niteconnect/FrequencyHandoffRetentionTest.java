package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.attendance.application.FrequencyHandoffRetention;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FrequencyHandoffRetentionTest extends AttendanceTestSupport {
    @Autowired FrequencyHandoffRetention retention;

    @Test
    void cleanupIsBoundedAndKeepsRecentlyExpiredAndUsableHandoffs() throws Exception {
        UUID event = event();
        UUID list = openList(event);
        qrForList(list);
        String tokenHash = jdbc.queryForObject("select qr_token_hash from nite_connect.attendance_lists where id=?",
                String.class, list);
        int qrVersion = jdbc.queryForObject("select qr_version from nite_connect.attendance_lists where id=?",
                Integer.class, list);
        Instant now = Instant.now();
        int auditEventsBefore = jdbc.queryForObject("select count(*) from nite_connect.attendance_list_events where list_id=?",
                Integer.class, list);
        insertHandoffs(list, tokenHash, qrVersion, 502, now.minus(Duration.ofHours(25)), 0);
        insertHandoffs(list, tokenHash, qrVersion, 2, now.minus(Duration.ofHours(1)), 502);
        insertHandoffs(list, tokenHash, qrVersion, 1, now.plus(Duration.ofHours(1)), 504);

        retention.cleanupExpiredHandoffs();
        assertThat(countHandoffs(list)).isEqualTo(5);
        assertThat(countHandoffsBefore(list, now.minus(Duration.ofHours(24)))).isEqualTo(2);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_list_events where list_id=?",
                Integer.class, list)).isEqualTo(auditEventsBefore);

        retention.cleanupExpiredHandoffs();
        assertThat(countHandoffs(list)).isEqualTo(3);
        assertThat(countHandoffsBefore(list, now.minus(Duration.ofHours(24)))).isZero();
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_list_events where list_id=?",
                Integer.class, list)).isEqualTo(auditEventsBefore);
    }

    @Test
    void concurrentCleanupBatchesUseSkipLockedWithoutLeavingOldRows() throws Exception {
        UUID event = event();
        UUID list = openList(event);
        qrForList(list);
        String tokenHash = jdbc.queryForObject("select qr_token_hash from nite_connect.attendance_lists where id=?",
                String.class, list);
        int qrVersion = jdbc.queryForObject("select qr_version from nite_connect.attendance_lists where id=?",
                Integer.class, list);
        insertHandoffs(list, tokenHash, qrVersion, 700, Instant.now().minus(Duration.ofHours(25)), 0);

        CountDownLatch start = new CountDownLatch(1);
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = pool.submit(() -> {
                start.await();
                retention.cleanupExpiredHandoffs();
                return true;
            });
            var second = pool.submit(() -> {
                start.await();
                retention.cleanupExpiredHandoffs();
                return true;
            });
            start.countDown();
            assertThat(first.get(10, TimeUnit.SECONDS)).isTrue();
            assertThat(second.get(10, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(countHandoffs(list)).isZero();
    }

    @Test
    void idempotentConfirmationCanBeReplayedAfterItsExpiredHandoffIsPurged() throws Exception {
        UUID event = event();
        UUID list = openList(event);
        String qr = qrForList(list);
        String handoffJson = mvc.perform(post("/api/v1/frequency/qr/handoffs").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("qrToken", qr))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String handoffId = json.readTree(handoffJson).get("handoffId").asString();
        String confirmationBody = json.writeValueAsString(Map.of("handoffId", handoffId));
        String confirmationJson = mvc.perform(post("/api/v1/me/frequency/confirmations").with(token(MEMBER))
                        .contentType("application/json").content(confirmationBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String attendanceId = json.readTree(confirmationJson).get("attendanceId").asString();

        String handoffHash = sha256(handoffId);
        jdbc.update("update nite_connect.frequency_qr_handoffs set expires_at=current_timestamp-interval '25 hours' where handoff_hash=?",
                handoffHash);
        retention.cleanupExpiredHandoffs();
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.frequency_qr_handoffs where handoff_hash=?",
                Long.class, handoffHash)).isZero();

        mvc.perform(post("/api/v1/me/frequency/confirmations").with(token(MEMBER)).contentType("application/json")
                        .content(confirmationBody))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.attendanceId").value(attendanceId))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.alreadyRegistered").value(true));
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_list_events where list_id=? and action='PRESENCE_CONFIRMED'",
                Long.class, list)).isEqualTo(1);
    }

    private void insertHandoffs(UUID list, String tokenHash, int qrVersion, int count, Instant expiresAt, int offset) {
        List<Object[]> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            rows.add(new Object[]{String.format("%064x", offset + i + 1), list, tokenHash, qrVersion,
                    Timestamp.from(expiresAt)});
        }
        jdbc.batchUpdate("""
                insert into nite_connect.frequency_qr_handoffs(handoff_hash,list_id,token_hash,qr_version,expires_at)
                values (?,?,?,?,?)
                """, rows);
    }

    private long countHandoffs(UUID list) {
        return jdbc.queryForObject("select count(*) from nite_connect.frequency_qr_handoffs where list_id=?", Long.class, list);
    }

    private long countHandoffsBefore(UUID list, Instant cutoff) {
        return jdbc.queryForObject("select count(*) from nite_connect.frequency_qr_handoffs where list_id=? and expires_at<?",
                Long.class, list, Timestamp.from(cutoff));
    }

    private String sha256(String value) throws NoSuchAlgorithmException {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
