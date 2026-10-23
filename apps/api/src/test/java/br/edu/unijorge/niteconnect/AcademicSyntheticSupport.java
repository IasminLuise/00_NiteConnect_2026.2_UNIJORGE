package br.edu.unijorge.niteconnect;

import java.time.Instant;
import java.util.*;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.BeforeEach;
import br.edu.unijorge.niteconnect.academic.integration.DocumentEvidenceReader;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.SourceSnapshot;
import br.edu.unijorge.niteconnect.academic.domain.AcademicStates.Source;

/** Only tests opt into this document provider; no implementation is packaged with the application. */
abstract class AcademicSyntheticSupport extends AcademicTestSupport {
    @Autowired java.util.concurrent.atomic.AtomicInteger syntheticDocumentRevision;
    @BeforeEach void resetDocumentRevision() { syntheticDocumentRevision.set(0); }
    @TestConfiguration static class Documents {
        @Bean java.util.concurrent.atomic.AtomicInteger syntheticDocumentRevision() { return new java.util.concurrent.atomic.AtomicInteger(); }
        @Bean DocumentEvidenceReader syntheticDocuments(java.util.concurrent.atomic.AtomicInteger revision) {
            return (actor,member,source,from,to)->new SourceSnapshot(1,Source.DOCUMENT,source,member,null,"synthetic-v"+revision.get(),member,ADMIN,"APPROVED",Instant.now().minusSeconds(60),Instant.now().minusSeconds(30),from,to,Map.of("fixture","test-only"),actor,actor.equals(member)?"SELF":"ADMINISTRATIVE");
        }
    }
    UUID completedCalculation(UUID e) throws Exception {
        UUID doc=identifier(postAs(MEMBER,"/api/v1/me/academic/enrollments/"+e+"/evidence",Map.of("source","DOCUMENT","sourceId",UUID.randomUUID(),"expectedVersion",ev(e),"reason","Documento sintético"),201));
        UUID presentation=identifier(postAs(ADMIN,"/api/v1/academic/enrollments/"+e+"/presentations",Map.of("title","Progresso","occurredAt",Instant.now().minusSeconds(60).toString(),"reference","Ata teste","expectedVersion",ev(e),"reason","Atestação"),201));
        UUID a=activity(MEMBER,null,MEMBER);
        UUID src=identifier(postAs(MEMBER,"/api/v1/activities/"+a+"/evidence",Map.of("type","TEXT","content","Entrega verificável","expectedVersion",av(a),"reason","Entrega"),201));
        state(a,MEMBER,"EM_EXECUCAO");state(a,MEMBER,"CONCLUIDA");
        UUID delivery=identifier(postAs(MEMBER,"/api/v1/me/academic/enrollments/"+e+"/evidence",Map.of("source","ACTIVITY","sourceId",src,"expectedVersion",ev(e),"reason","Relacionar"),201));
        String[] pillars={"DOCUMENTATION","PRESENTATIONS","DELIVERIES"};UUID[] ids={doc,presentation,delivery};int[] scores={8,7,9};
        for(int i=0;i<3;i++) postAs(ADMIN,"/api/v1/academic/enrollments/"+e+"/assessments",Map.of("pillar",pillars[i],"score",scores[i],"evidenceIds",List.of(ids[i]),"expectedVersion",ev(e),"reason","Conferência humana"),201);
        return identifier(postAs(ADMIN,"/api/v1/academic/enrollments/"+e+"/calculations",command(ev(e)),201));
    }
}
