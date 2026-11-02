package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AttendanceTransactionsTest extends AttendanceTestSupport {
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired AdministrativeTransactions transactions;

    @Test void auditFailureRollsBackAttendancePeriodAndJournal() throws Exception {
        UUID event = event();
        String payload = qr(event);
        jdbc.execute("create function nite_connect.fail_b04_audit() returns trigger language plpgsql as $$ begin if NEW.entity_type='ATTENDANCE' then raise exception 'Simulated audit failure'; end if;return NEW;end;$$");
        jdbc.execute("create trigger fail_b04_audit before insert on nite_connect.audit_events for each row execute function nite_connect.fail_b04_audit()");
        try {
            assertThatThrownBy(() -> checkIn(event, payload)).isInstanceOf(Exception.class);
            assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_records", Long.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_segments", Long.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_events where member_id=?", Long.class, MEMBER)).isZero();
        } finally {
            jdbc.execute("drop trigger fail_b04_audit on nite_connect.audit_events");
            jdbc.execute("drop function nite_connect.fail_b04_audit()");
        }
    }

    @Test void revocationWhileCheckInWaitsForAdministrativeLockIsRechecked() throws Exception {
        UUID event = event();
        String body = json.writeValueAsString(Map.of("eventId", event, "payload", qr(event)));
        var request = new AtomicReference<Future<Integer>>();
        var ready = new CountDownLatch(1);
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                transactions.lock();
                request.set(pool.submit(() -> {
                    ready.countDown();
                    return mvc.perform(post("/api/v1/me/attendance/check-in").with(token(MEMBER)).contentType("application/json")
                            .content(body)).andReturn().getResponse().getStatus();
                }));
                try {
                    assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                boolean waiting = false;
                while (System.nanoTime() < deadline) {
                    waiting = jdbc.queryForObject("select exists(select 1 from pg_locks where locktype='advisory' and classid=0 and objid=784629301 and not granted)", Boolean.class);
                    if (waiting) break;
                    try { Thread.sleep(20); } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(error);
                    }
                }
                assertThat(waiting).as("check-in waiting on administrative lock before revocation").isTrue();
                jdbc.update("update nite_connect.members set status='AFASTADO' where id=?", MEMBER);
            });
            assertThat(request.get().get(20, TimeUnit.SECONDS)).isEqualTo(403);
        }
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_records", Long.class)).isZero();
    }

    @Test void concurrentInvalidAnonymousHandoffsDoNotAcquireTheAdministrativeAdvisoryLock() throws Exception {
        UUID event = event();
        String openBody = json.writeValueAsString(Map.of("calendarEventId", event,
                "confirmationWindowMinutes", 3, "countsForFrequency", true));
        String invalidHandoff = json.writeValueAsString(Map.of("qrToken", "A".repeat(43)));
        var start = new CountDownLatch(1);
        var invalidRequests = new ArrayList<Future<Integer>>();
        var adminRequest = new AtomicReference<Future<Integer>>();

        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                transactions.lock();
                for (int i = 0; i < 4; i++) {
                    invalidRequests.add(pool.submit(() -> {
                        start.await();
                        return mvc.perform(post("/api/v1/frequency/qr/handoffs").contentType("application/json")
                                .content(invalidHandoff)).andReturn().getResponse().getStatus();
                    }));
                }
                start.countDown();
                for (Future<Integer> invalid : invalidRequests) {
                    try {
                        assertThat(invalid.get(5, TimeUnit.SECONDS)).isEqualTo(409);
                    } catch (Exception error) {
                        throw new AssertionError("QR inválido disputou o lock administrativo", error);
                    }
                }
                assertThat(jdbc.queryForObject("select exists(select 1 from pg_locks where locktype='advisory' and classid=0 and objid=784629301 and not granted)",
                        Boolean.class)).isFalse();

                adminRequest.set(pool.submit(() -> mvc.perform(post("/api/v1/attendance/lists").with(token(ADMIN))
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType("application/json").content(openBody)).andReturn().getResponse().getStatus()));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                boolean waiting = false;
                while (System.nanoTime() < deadline) {
                    waiting = jdbc.queryForObject("select exists(select 1 from pg_locks where locktype='advisory' and classid=0 and objid=784629301 and not granted)",
                            Boolean.class);
                    if (waiting) break;
                    try { Thread.sleep(20); } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(error);
                    }
                }
                assertThat(waiting).as("mutação administrativa espera pelo lock global mantido pelo fixture").isTrue();
            });
            assertThat(adminRequest.get().get(20, TimeUnit.SECONDS)).isEqualTo(201);
        }
    }

    @Test void eventUpdateUsesOptimisticVersionAndFreezesAnOpenedSession() throws Exception {
        UUID event = event();
        String body = json.writeValueAsString(Map.of("title", "Revisado", "description", "Outro", "kind", "ENCONTRO", "modality", "REMOTA",
                "startsAt", Instant.now().minusSeconds(300).toString(), "endsAt", Instant.now().plusSeconds(3600).toString(),
                "expectedMemberIds", List.of(MEMBER), "expectedVersion", 0, "reason", "Corrigir"));
        mvc.perform(patch("/api/v1/agenda/events/" + event).with(token(ADMIN)).contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/agenda/events/" + event).with(token(ADMIN)).contentType("application/json").content(body)).andExpect(status().isConflict());
        qr(event);
        mvc.perform(patch("/api/v1/agenda/events/" + event).with(token(ADMIN)).contentType("application/json")
                .content(body.replace("\"expectedVersion\":0", "\"expectedVersion\":" + ev(event)))).andExpect(status().isConflict());
    }
}
