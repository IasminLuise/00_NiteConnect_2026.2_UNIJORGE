package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FrequencyBoundaryTest extends AttendanceTestSupport {
    @Test
    void openingRequiresManagerAndSnapshotsEveryActiveMemberExactlyOnce() throws Exception {
        UUID event = exceptionalAgendaEvent();
        String idempotencyKey = UUID.randomUUID().toString();
        String body = json.writeValueAsString(Map.of(
                "calendarEventId", event,
                "confirmationWindowMinutes", 3,
                "countsForFrequency", true));

        mvc.perform(post("/api/v1/attendance/lists").with(token(LEADER))
                        .header("Idempotency-Key", idempotencyKey).contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/attendance/lists").with(token(ADMIN))
                        .header("Idempotency-Key", idempotencyKey).contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.calendarEventId").value(event.toString()))
                .andExpect(jsonPath("$.meetingName").value("Reunião excepcional"))
                .andExpect(jsonPath("$.classification").value("EXCEPTIONAL"))
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.qr").value(nullValue()));

        UUID list = jdbc.queryForObject("select id from nite_connect.attendance_lists where calendar_event_id=?", UUID.class, event);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_list_members where list_id=?", Long.class, list)).isEqualTo(4);
        assertThat(jdbc.queryForObject("select count(distinct member_id) from nite_connect.attendance_list_members where list_id=?", Long.class, list)).isEqualTo(4);
        assertThat(jdbc.queryForObject("select status from nite_connect.calendar_events where id=?", String.class, event)).isEqualTo("ABERTO");

        mvc.perform(post("/api/v1/attendance/lists").with(token(ADMIN))
                        .header("Idempotency-Key", idempotencyKey).contentType("application/json").content(body))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.listId").value(list.toString()));
        mvc.perform(post("/api/v1/attendance/lists").with(token(ADMIN))
                        .header("Idempotency-Key", UUID.randomUUID().toString()).contentType("application/json").content(body))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/attendance/lists/" + list + "/participants").with(token(MEMBER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void qrConfirmationUsesAuthenticatedIdentityAndIsIdempotentAfterRotation() throws Exception {
        UUID event = exceptionalAgendaEvent();
        UUID list = openExceptionList(event, UUID.randomUUID().toString());
        String firstToken = issueQr(list, 0);

        String confirmation = json.writeValueAsString(Map.of("qrToken", firstToken, "memberId", OTHER));
        String response = mvc.perform(post("/api/v1/me/frequency/confirmations").with(token(MEMBER))
                        .contentType("application/json").content(confirmation))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PRESENTE"))
                .andExpect(jsonPath("$.alreadyRegistered").value(false))
                .andReturn().getResponse().getContentAsString();
        UUID attendance = UUID.fromString(json.readTree(response).get("attendanceId").asString());

        long currentListVersion = jdbc.queryForObject("select version from nite_connect.attendance_lists where id=?", Long.class, list);
        issueQr(list, currentListVersion);
        mvc.perform(post("/api/v1/me/frequency/confirmations").with(token(MEMBER))
                        .contentType("application/json").content(confirmation))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.attendanceId").value(attendance.toString()))
                .andExpect(jsonPath("$.alreadyRegistered").value(true));

        assertThat(jdbc.queryForObject("select member_id from nite_connect.attendance_records where id=?", UUID.class, attendance)).isEqualTo(MEMBER);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_records where event_id=? and member_id=?", Long.class, event, MEMBER)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_segments where attendance_id=?", Long.class, attendance)).isZero();

        mvc.perform(get("/api/v1/attendance/lists/" + list).with(token(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrToken").doesNotExist());
        mvc.perform(post("/api/v1/agenda/events/" + event + "/session").with(token(ADMIN))
                        .contentType("application/json").content("{\"expectedVersion\":1,\"reason\":\"QR legado\"}"))
                .andExpect(status().isGone());
    }

    @Test
    void anonymousQrHandoffDoesNotCreatePresenceAndRequiresAuthenticatedConsumption() throws Exception {
        UUID event = exceptionalAgendaEvent();
        UUID list = openExceptionList(event, UUID.randomUUID().toString());
        String qrToken = issueQr(list, 0);
        String handoffBody = json.writeValueAsString(Map.of("qrToken", qrToken));

        String response = mvc.perform(post("/api/v1/frequency/qr/handoffs")
                        .contentType("application/json").content(handoffBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listId").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String handoffId = json.readTree(response).get("handoffId").asString();
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_records where event_id=?", Long.class, event)).isZero();

        mvc.perform(post("/api/v1/me/frequency/confirmations").with(token(MEMBER))
                        .contentType("application/json").content(json.writeValueAsString(Map.of("handoffId", handoffId))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PRESENTE"));
        mvc.perform(post("/api/v1/me/frequency/confirmations").with(token(OTHER))
                        .contentType("application/json").content(json.writeValueAsString(Map.of("handoffId", handoffId))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/me/frequency/confirmations").with(token(MEMBER))
                        .contentType("application/json").content(json.writeValueAsString(Map.of("handoffId", handoffId))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.alreadyRegistered").value(true));
        mvc.perform(post("/api/v1/me/frequency/confirmations").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("handoffId", handoffId))))
                .andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_records where event_id=?", Long.class, event)).isEqualTo(1);
    }

    @Test
    void handoffRevalidatesQrAfterRotationAndListClosure() throws Exception {
        UUID event = exceptionalAgendaEvent();
        UUID list = openExceptionList(event, UUID.randomUUID().toString());
        String firstQr = issueQr(list, 0);
        String oldHandoffBody = json.writeValueAsString(Map.of("qrToken", firstQr));
        mvc.perform(post("/api/v1/frequency/qr/handoffs").contentType("application/json").content(oldHandoffBody))
                .andExpect(status().isCreated());

        String currentQr = issueQr(list, 1);
        mvc.perform(post("/api/v1/frequency/qr/handoffs").contentType("application/json").content(oldHandoffBody))
                .andExpect(status().isConflict());
        String currentHandoff = mvc.perform(post("/api/v1/frequency/qr/handoffs").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("qrToken", currentQr))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String handoffId = json.readTree(currentHandoff).get("handoffId").asString();

        mvc.perform(post("/api/v1/attendance/lists/" + list + "/finalize").with(token(ADMIN))
                        .contentType("application/json").content(json.writeValueAsString(Map.of("expectedVersion", 2,
                                "outcome", "REALIZADA_CONTABILIZAVEL", "reason", "Reunião realizada"))))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/me/frequency/confirmations").with(token(MEMBER)).contentType("application/json")
                        .content(json.writeValueAsString(Map.of("handoffId", handoffId))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/frequency/qr/handoffs").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("qrToken", currentQr))))
                .andExpect(status().isConflict());
    }

    @Test
    void handoffCreationRacingQrRenewalFinishesWithoutDeadlockAndOldQrCannotBeConsumed() throws Exception {
        UUID event = exceptionalAgendaEvent();
        UUID list = openExceptionList(event, UUID.randomUUID().toString());
        String qr = issueQr(list, 0);
        String handoffBody = json.writeValueAsString(Map.of("qrToken", qr));
        String renewalBody = json.writeValueAsString(Map.of("expectedVersion", 1));
        var latch = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var handoff = pool.submit(() -> {
                latch.await();
                return mvc.perform(post("/api/v1/frequency/qr/handoffs").contentType("application/json")
                        .content(handoffBody)).andReturn().getResponse();
            });
            var renewal = pool.submit(() -> {
                latch.await();
                return mvc.perform(post("/api/v1/attendance/lists/" + list + "/qr/renew").with(token(ADMIN))
                        .contentType("application/json").content(renewalBody)).andReturn().getResponse();
            });
            latch.countDown();
            var handoffResponse = handoff.get(10, java.util.concurrent.TimeUnit.SECONDS);
            var renewalResponse = renewal.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(renewalResponse.getStatus()).isEqualTo(200);
            assertThat(handoffResponse.getStatus()).isIn(201, 409);
            if (handoffResponse.getStatus() == 201) {
                String handoffId = json.readTree(handoffResponse.getContentAsString()).get("handoffId").asString();
                mvc.perform(post("/api/v1/me/frequency/confirmations").with(token(MEMBER)).contentType("application/json")
                                .content(json.writeValueAsString(Map.of("handoffId", handoffId))))
                        .andExpect(status().isConflict());
            }
        }
    }

    @Test
    void gestorGeralCanOpenListsAndConcurrentDuplicateOpensAreSerialized() throws Exception {
        UUID event = exceptionalAgendaEvent();
        jdbc.update("insert into nite_connect.role_assignments(member_id,role,granted_by,reason) values (?,'GESTOR_GERAL',?,'Fixture de autorização')",
                MEMBER, ADMIN);
        String body = json.writeValueAsString(Map.of("calendarEventId", event, "confirmationWindowMinutes", 3,
                "countsForFrequency", true));
        var latch = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var first = pool.submit(() -> {
                latch.await();
                return mvc.perform(post("/api/v1/attendance/lists").with(token(MEMBER))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json").content(body)).andReturn().getResponse().getStatus();
            });
            var second = pool.submit(() -> {
                latch.await();
                return mvc.perform(post("/api/v1/attendance/lists").with(token(MEMBER))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json").content(body)).andReturn().getResponse().getStatus();
            });
            latch.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_lists where calendar_event_id=?", Long.class, event)).isEqualTo(1);
    }

    @Test
    void finalizationRetryAfterLostResponseReturnsCommittedResultWithoutDuplicatingAudit() throws Exception {
        UUID event = exceptionalAgendaEvent();
        UUID list = openExceptionList(event, UUID.randomUUID().toString());
        jdbc.update("insert into nite_connect.role_assignments(member_id,role,granted_by,reason) values (?,'GESTOR_GERAL',?,'Fixture de autorização')",
                OTHER, ADMIN);
        String body = json.writeValueAsString(Map.of("expectedVersion", 0,
                "outcome", "REALIZADA_CONTABILIZAVEL", "reason", "Reunião realizada"));

        // A primeira resposta é descartada para simular perda depois do commit.
        mvc.perform(post("/api/v1/attendance/lists/" + list + "/finalize").with(token(ADMIN))
                        .contentType("application/json").content(body))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/attendance/lists/" + list + "/finalize").with(token(ADMIN))
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.version").value(1));

        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_list_events where list_id=? and action='LIST_FINALIZED'",
                Long.class, list)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.audit_events where entity_type='CALENDAR_EVENT' and entity_id=? and action='SESSION_CLOSED'",
                Long.class, event)).isEqualTo(1);

        mvc.perform(post("/api/v1/attendance/lists/" + list + "/finalize").with(token(ADMIN))
                        .contentType("application/json").content(json.writeValueAsString(Map.of("expectedVersion", 0,
                                "outcome", "NAO_REALIZADA", "reason", "Reunião realizada"))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/attendance/lists/" + list + "/finalize").with(token(ADMIN))
                        .contentType("application/json").content(json.writeValueAsString(Map.of("expectedVersion", 0,
                                "outcome", "REALIZADA_CONTABILIZAVEL", "reason", "Outro motivo"))))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/attendance/lists/" + list + "/finalize").with(token(OTHER))
                        .contentType("application/json").content(body))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/attendance/lists/" + list + "/finalize").with(token(ADMIN))
                        .contentType("application/json").content(json.writeValueAsString(Map.of("expectedVersion", 1,
                                "outcome", "REALIZADA_CONTABILIZAVEL", "reason", "Reunião realizada"))))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_list_events where list_id=? and action='LIST_FINALIZED'",
                Long.class, list)).isEqualTo(1);
    }

    @Test
    void concurrentIdenticalFinalizationsCommitOneResultAndOneAuditEvent() throws Exception {
        UUID event = exceptionalAgendaEvent();
        UUID list = openExceptionList(event, UUID.randomUUID().toString());
        String body = json.writeValueAsString(Map.of("expectedVersion", 0,
                "outcome", "REALIZADA_CONTABILIZAVEL", "reason", "Reunião realizada"));
        var latch = new java.util.concurrent.CountDownLatch(1);
        try (var pool = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var first = pool.submit(() -> {
                latch.await();
                return mvc.perform(post("/api/v1/attendance/lists/" + list + "/finalize").with(token(ADMIN))
                        .contentType("application/json").content(body)).andReturn().getResponse().getStatus();
            });
            var second = pool.submit(() -> {
                latch.await();
                return mvc.perform(post("/api/v1/attendance/lists/" + list + "/finalize").with(token(ADMIN))
                        .contentType("application/json").content(body)).andReturn().getResponse().getStatus();
            });
            latch.countDown();
            assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 200);
        }
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_list_events where list_id=? and action='LIST_FINALIZED'",
                Long.class, list)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.audit_events where entity_type='CALENDAR_EVENT' and entity_id=? and action='SESSION_CLOSED'",
                Long.class, event)).isEqualTo(1);
    }

    @Test
    void malformedHandoffQrIsRejectedByRequestValidation() throws Exception {
        mvc.perform(post("/api/v1/frequency/qr/handoffs").contentType("application/json")
                        .content(json.writeValueAsString(Map.of("qrToken", "A".repeat(42)))))
                .andExpect(status().isBadRequest());
    }

    private UUID exceptionalAgendaEvent() throws Exception {
        String body = json.writeValueAsString(Map.of(
                "title", "Reunião excepcional",
                "description", "Fixture sintético",
                "kind", "EVENTO",
                "modality", "REMOTA",
                "startsAt", Instant.now().minusSeconds(30).toString(),
                "endsAt", Instant.now().plusSeconds(3600).toString(),
                "expectedMemberIds", List.of(MEMBER),
                "reason", "Fixture de frequência"));
        String response = mvc.perform(post("/api/v1/agenda/events").with(token(ADMIN))
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).get("id").asString());
    }

    private UUID openExceptionList(UUID event, String idempotencyKey) throws Exception {
        String body = json.writeValueAsString(Map.of(
                "calendarEventId", event,
                "confirmationWindowMinutes", 3,
                "countsForFrequency", true));
        String response = mvc.perform(post("/api/v1/attendance/lists").with(token(ADMIN))
                        .header("Idempotency-Key", idempotencyKey).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).get("listId").asString());
    }

    private String issueQr(UUID list, long expectedVersion) throws Exception {
        String response = mvc.perform(post("/api/v1/attendance/lists/" + list + "/qr/renew").with(token(ADMIN))
                        .contentType("application/json").content(json.writeValueAsString(Map.of("expectedVersion", expectedVersion))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("qrToken").asString();
    }
}
