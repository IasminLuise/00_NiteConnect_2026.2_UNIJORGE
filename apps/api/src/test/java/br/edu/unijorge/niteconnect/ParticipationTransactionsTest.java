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

class ParticipationTransactionsTest extends AttendanceTestSupport {
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired AdministrativeTransactions transactions;

    @Test void auditFailureRollsBackAttendanceAndPeriodAndJournal() throws Exception {
        UUID event=event();String qr=qr(event);
        jdbc.execute("create function nite_connect.fail_b04_audit() returns trigger language plpgsql as $$ begin if NEW.entity_type='PERIOD' then raise exception 'Simulated audit failure'; end if;return NEW;end;$$");
        jdbc.execute("create trigger fail_b04_audit before insert on nite_connect.audit_events for each row execute function nite_connect.fail_b04_audit()");
        try {
            assertThatThrownBy(()->checkIn(event,qr)).isInstanceOf(Exception.class);
            assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_records",Long.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from nite_connect.participation_periods",Long.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from nite_connect.participation_events where member_id=?",Long.class,MEMBER)).isZero();
        } finally {
            jdbc.execute("drop trigger fail_b04_audit on nite_connect.audit_events");jdbc.execute("drop function nite_connect.fail_b04_audit()");
        }
    }
    @Test void revocationHoldingAdministrativeLockIsRecheckedByWaitingCheckIn() throws Exception {
        UUID event=event();String payload=qr(event),body=json.writeValueAsString(Map.of("eventId",event,"payload",payload));
        var request=new AtomicReference<Future<Integer>>();var ready=new CountDownLatch(1);
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            new TransactionTemplate(transactionManager).executeWithoutResult(status->{
                transactions.lock();
                request.set(pool.submit(()->{ready.countDown();return mvc.perform(post("/api/v1/me/attendance/check-in").with(token(MEMBER)).contentType("application/json").content(body)).andReturn().getResponse().getStatus();}));
                try { assertThat(ready.await(5,TimeUnit.SECONDS)).isTrue(); } catch(InterruptedException e) { Thread.currentThread().interrupt();throw new IllegalStateException(e); }
                long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
                boolean waiting=false;
                while(System.nanoTime()<deadline) {
                    waiting=jdbc.queryForObject("select exists(select 1 from pg_locks where locktype='advisory' and classid=0 and objid=784629301 and not granted)",Boolean.class);
                    if(waiting) break;
                    try { Thread.sleep(20); } catch(InterruptedException e) { Thread.currentThread().interrupt();throw new IllegalStateException(e); }
                }
                assertThat(waiting).as("check-in waiting on the administrative lock before revocation").isTrue();
                jdbc.update("update nite_connect.members set status='AFASTADO' where id=?",MEMBER);
            });
            assertThat(request.get().get(20,TimeUnit.SECONDS)).isEqualTo(403);
        }
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.attendance_records",Long.class)).isZero();
    }
    @Test void databaseSerializesConcurrentClosedIntervalsWithoutServiceLock() throws Exception {
        var latch=new CountDownLatch(1);
        try(var pool=Executors.newVirtualThreadPerTaskExecutor()) {
            Callable<Boolean> insert=()->{
                latch.await();
                try {
                    jdbc.update("""
                        insert into nite_connect.participation_periods(id,member_id,kind,modality,starts_at,ends_at,description,origin)
                        values (?,?,'WORK_LOG','REMOTA','2026-10-10T10:00:00Z','2026-10-10T11:00:00Z','Trabalho','DECLARATION')
                        """,UUID.randomUUID(),MEMBER);return true;
                } catch(org.springframework.dao.DataIntegrityViolationException ex) { return false; }
            };
            var a=pool.submit(insert);var b=pool.submit(insert);latch.countDown();
            assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        }
    }
    @Test void cancelledMeetingsDoNotGenerateAbsenceAndNoExpectationMeansNoAbsence() throws Exception {
        UUID event=event();
        String eventDate=localEventDate(event);
        mvc.perform(post("/api/v1/agenda/events/"+event+"/cancel").with(token(ADMIN)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion",ev(event),"reason","Cancelamento","confirmed",true)))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/me/participation-summary?from="+eventDate+"&to="+eventDate).with(token(MEMBER))).andExpect(status().isOk()).andExpect(jsonPath("$.absentMeetings").value(0));
        UUID second=event();qr(second);
        mvc.perform(post("/api/v1/agenda/events/"+second+"/close").with(token(ADMIN)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion",ev(second),"reason","Conferência")))).andExpect(status().isOk());
        String secondDate=localEventDate(second);
        mvc.perform(get("/api/v1/me/participation-summary?from="+secondDate+"&to="+secondDate).with(token(MEMBER))).andExpect(jsonPath("$.absentMeetings").value(1));
        mvc.perform(get("/api/v1/me/participation-summary?from="+secondDate+"&to="+secondDate).with(token(OTHER))).andExpect(jsonPath("$.absentMeetings").value(0));
    }

    private String localEventDate(UUID event) {
        return jdbc.queryForObject("select starts_at from nite_connect.calendar_events where id=?",java.sql.Timestamp.class,event)
                .toInstant().atZone(java.time.ZoneId.of("America/Bahia")).toLocalDate().toString();
    }
    @Test void eventUpdateEnforcesVersionAndFreezesOpenedProgram() throws Exception {
        UUID event=event();String body=json.writeValueAsString(Map.of("title","Revisado","description","Outro","kind","ENCONTRO","modality","REMOTA",
                "startsAt",Instant.now().minusSeconds(300).toString(),"endsAt",Instant.now().plusSeconds(3600).toString(),"expectedMemberIds",List.of(MEMBER),"expectedVersion",0,"reason","Corrigir"));
        mvc.perform(patch("/api/v1/agenda/events/"+event).with(token(ADMIN)).contentType("application/json").content(body)).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/agenda/events/"+event).with(token(ADMIN)).contentType("application/json").content(body)).andExpect(status().isConflict());
        qr(event);
        mvc.perform(patch("/api/v1/agenda/events/"+event).with(token(ADMIN)).contentType("application/json").content(body.replace("\"expectedVersion\":0","\"expectedVersion\":"+ev(event)))).andExpect(status().isConflict());
    }
}
