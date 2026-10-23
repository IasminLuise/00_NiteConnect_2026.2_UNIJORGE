package br.edu.unijorge.niteconnect;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.access.domain.GlobalRole;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@Import(AcademicSyntheticSupport.Documents.class)
class AcademicConcurrencyTest extends AcademicSyntheticSupport {
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired AdministrativeTransactions transactions;
    @Autowired AccessService access;
    int homologate(UUID calc,long version) throws Exception {
        return mvc.perform(post("/api/v1/academic/calculations/"+calc+"/homologations").with(token(ADMIN)).contentType("application/json").content(json.writeValueAsString(command(version)))).andReturn().getResponse().getStatus();
    }
    @Test void twoConcurrentHomologationsProduceOnlyOneDecision() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);UUID c=completedCalculation(e);long version=ev(e);var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<Integer> call=()->{ start.await();return homologate(c,version); };
            var a=pool.submit(call);var b=pool.submit(call);start.countDown();
            assertThat(List.of(a.get(20,TimeUnit.SECONDS),b.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
        }
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.academic_homologations where enrollment_id=?",Long.class,e)).isEqualTo(1);
    }
    @Test void coordinatorRevokedWhileOperationWaitsCannotWrite() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);UUID c=completedCalculation(e);long version=ev(e);
        access.grant(ADMIN,OTHER,GlobalRole.COORDENADOR,0,"Outro coordenador sintético");
        try(var pool=Executors.newSingleThreadExecutor()) {
            var started=new CountDownLatch(1);List<Future<Integer>> pending=new ArrayList<>();
            new TransactionTemplate(transactionManager).executeWithoutResult(status->{
                transactions.lock();pending.add(pool.submit(()->{ started.countDown();return homologate(c,version); }));
                try { assertThat(started.await(5,TimeUnit.SECONDS)).isTrue(); } catch(InterruptedException ex) { Thread.currentThread().interrupt();throw new IllegalStateException(ex); }
                access.revoke(OTHER,ADMIN,GlobalRole.COORDENADOR,0,"Revogação concorrente");
            });
            assertThat(pending.getFirst().get(20,TimeUnit.SECONDS)).isEqualTo(403);
        }
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.academic_homologations where enrollment_id=?",Long.class,e)).isZero();
    }
}
