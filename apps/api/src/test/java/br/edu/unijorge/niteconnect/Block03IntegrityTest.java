package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.members.application.MemberAdministration;
import br.edu.unijorge.niteconnect.members.domain.MemberStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class Block03IntegrityTest extends ActivityTestSupport {
    @Autowired PlatformTransactionManager manager;
    @Autowired MemberAdministration members;

    @Test void postgresRejectsActivityWithoutExecutorAtCommit() {
        UUID id=UUID.randomUUID();
        assertThatThrownBy(()->new TransactionTemplate(manager).executeWithoutResult(tx->jdbc.update(
                "insert into nite_connect.activities(id,title,description,created_by) values (?,'Sem executor','Teste',?)",id,MEMBER)))
                .hasStackTraceContaining("exactly one current executor");
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.activities where id=?",Integer.class,id)).isZero();
    }
    @Test void databaseProtectsExclusiveRolesAndClosedAssignmentHistory() throws Exception {
        UUID id=activity(MEMBER,null,MEMBER);
        assertThatThrownBy(()->jdbc.update("insert into nite_connect.activity_assignments(id,activity_id,member_id,role,started_by,start_reason) values (?,?,?,'COLLABORATOR',?,'Duplicado')",UUID.randomUUID(),id,MEMBER,ADMIN))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        mvc.perform(put("/api/v1/activities/"+id+"/executor").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content(command(id,",\"memberId\":\""+OTHER+"\",\"confirmed\":true"))).andExpect(status().isOk());
        assertThatThrownBy(()->jdbc.update("update nite_connect.activity_assignments set start_reason='Outra' where activity_id=? and member_id=?",id,MEMBER))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(()->jdbc.update("delete from nite_connect.activities where id=?",id)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
    @Test void databaseRejectsEvidencePointingAtPendingObjectAndIncompleteAvailability() throws Exception {
        UUID activity=activity(MEMBER,null,MEMBER),object=UUID.randomUUID();
        jdbc.update("insert into nite_connect.evidence_objects(id,activity_id,author_id,object_key,filename,state) values (?,?,?,?,'private.txt','PENDING')",object,activity,MEMBER,UUID.randomUUID());
        assertThatThrownBy(()->jdbc.update("insert into nite_connect.activity_evidence(id,activity_id,author_id,type,object_id) values (?,?,?,'FILE',?)",UUID.randomUUID(),activity,MEMBER,object))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("update nite_connect.evidence_objects set state='AVAILABLE',content_type='text/plain',size_bytes=4 where id=?",object))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    @Test void twoExecutorTransfersUsingSameVersionHaveExactlyOneWinner() throws Exception {
        UUID id=activity(ADMIN,null,MEMBER);
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch start=new CountDownLatch(1);
            Callable<Integer> operation=()-> { start.await();return mvc.perform(put("/api/v1/activities/"+id+"/executor").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"memberId\":\""+OTHER+"\",\"expectedVersion\":0,\"reason\":\"Troca\",\"confirmed\":true}")).andReturn().getResponse().getStatus(); };
            var first=executor.submit(operation);var second=executor.submit(operation);start.countDown();
            assertThat(List.of(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(av(id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.activity_assignments where activity_id=? and role='EXECUTOR' and ended_at is null",Integer.class,id)).isEqualTo(1);
    }
    @Test void revocationCommittedWhileWriterWaitsIsRevalidatedBeforeMutation() throws Exception {
        UUID id=activity(MEMBER,null,MEMBER);
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()) {
            var pending=new java.util.concurrent.atomic.AtomicReference<Future<Integer>>();
            new TransactionTemplate(manager).executeWithoutResult(tx->{
                jdbc.queryForList("select pg_advisory_xact_lock(784629301)");
                pending.set(executor.submit(()->mvc.perform(patch("/api/v1/activities/"+id+"/status").with(token(MEMBER)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"EM_EXECUCAO\",\"expectedVersion\":0,\"reason\":\"Iniciar\"}")).andReturn().getResponse().getStatus()));
                awaitBlocked();members.changeStatus(ADMIN,MEMBER,MemberStatus.AFASTADO,0,"Afastamento concorrente");
            });
            assertThat(pending.get().get(10,TimeUnit.SECONDS)).isEqualTo(403);
        }
        assertThat(av(id)).isZero();
    }
    void awaitBlocked() {
        long end=System.nanoTime()+Duration.ofSeconds(5).toNanos();
        while(System.nanoTime()<end) {
            if(Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from pg_locks where locktype='advisory' and classid=0 and objid=784629301 and not granted)",Boolean.class))) return;
            try { Thread.sleep(20); } catch(InterruptedException e) { Thread.currentThread().interrupt();throw new AssertionError(e); }
        }
        throw new AssertionError("Escrita não aguardou o lock administrativo");
    }
    @Test void auditFailureRollsBackVersionStatusAndOperationalEvent() throws Exception {
        UUID id=activity(MEMBER,null,MEMBER);
        jdbc.execute("create function nite_connect.test_reject_activity_audit() returns trigger language plpgsql as $$ begin if NEW.action='ACTIVITY_STATUS_CHANGED' then raise exception 'Injected audit failure'; end if; return NEW; end; $$");
        jdbc.execute("create trigger test_reject_activity_audit before insert on nite_connect.audit_events for each row execute function nite_connect.test_reject_activity_audit()");
        try {
            assertThatThrownBy(()->state(id,MEMBER,"EM_EXECUCAO")).hasStackTraceContaining("Injected audit failure");
            assertThat(av(id)).isZero();
            assertThat(jdbc.queryForObject("select status from nite_connect.activities where id=?",String.class,id)).isEqualTo("PLANEJADA");
            assertThat(jdbc.queryForObject("select count(*) from nite_connect.activity_events where activity_id=?",Integer.class,id)).isEqualTo(1);
        } finally {
            jdbc.execute("drop trigger test_reject_activity_audit on nite_connect.audit_events");
            jdbc.execute("drop function nite_connect.test_reject_activity_audit()");
        }
    }
    @Test void leavingProjectPreservesOwnEvidenceWithoutGrantingAccessToOtherContributions() throws Exception {
        UUID p=activeProject(),id=activity(LEADER,p,MEMBER);
        mvc.perform(post("/api/v1/activities/"+id+"/evidence").with(token(MEMBER)).contentType(MediaType.APPLICATION_JSON)
                .content(command(id,",\"type\":\"TEXT\",\"content\":\"Meu trabalho\""))).andExpect(status().isCreated());
        UUID own=jdbc.queryForObject("select id from nite_connect.activity_evidence where author_id=?",UUID.class,MEMBER);
        mvc.perform(post("/api/v1/activities/"+id+"/evidence").with(token(LEADER)).contentType(MediaType.APPLICATION_JSON)
                .content(command(id,",\"type\":\"TEXT\",\"content\":\"Trabalho alheio\""))).andExpect(status().isCreated());
        UUID other=jdbc.queryForObject("select id from nite_connect.activity_evidence where author_id=?",UUID.class,LEADER);
        mvc.perform(delete("/api/v1/projects/"+p+"/members/"+MEMBER).with(token(LEADER)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":"+version(p)+",\"reason\":\"Saída\"}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/activities/"+id).with(token(MEMBER))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/me/activities/history").with(token(MEMBER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].activityId").value(id.toString())).andExpect(jsonPath("$.items[0].startReason").doesNotExist());
        mvc.perform(get("/api/v1/me/activities/evidence/"+own).with(token(MEMBER))).andExpect(status().isOk()).andExpect(jsonPath("$.content").value("Meu trabalho"));
        mvc.perform(get("/api/v1/me/activities/evidence/"+other).with(token(MEMBER))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/activities/"+id).with(token(LEADER))).andExpect(jsonPath("$.executorAvailable").value(false));
        mvc.perform(get("/api/v1/activities/"+id+"/assignees/history").with(token(LEADER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].memberName").value("Teste")).andExpect(jsonPath("$.items[0].available").value(false))
                .andExpect(jsonPath("$.items[0].institutionalEmail").doesNotExist());
    }
}
