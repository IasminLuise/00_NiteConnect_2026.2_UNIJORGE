package br.edu.unijorge.niteconnect;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import br.edu.unijorge.niteconnect.academic.integration.DocumentEvidenceReader;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.SourceSnapshot;
import br.edu.unijorge.niteconnect.academic.domain.AcademicStates.Source;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Synthetic document provider exists exclusively in this test context; B05 remains unavailable in product. */
@Import(AcademicHomologationTest.Documents.class)
class AcademicHomologationTest extends AcademicTestSupport {
    @TestConfiguration static class Documents {
        @Bean DocumentEvidenceReader syntheticDocuments() {
            return (actor,member,source,from,to)->new SourceSnapshot(1,Source.DOCUMENT,source,member,null,"synthetic-v1",member,ADMIN,"APPROVED",Instant.now().minusSeconds(60),Instant.now().minusSeconds(30),from,to,Map.of("fixture","test-only"),actor,actor.equals(member)?"SELF":"ADMINISTRATIVE");
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
    @Test void calculationIsReproducibleAndOnlyCoordinatorCanHomologateOrReopen() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);UUID c=completedCalculation(e);
        String original=jdbc.queryForObject("select data::text from nite_connect.academic_calculations where id=?",String.class,c);
        assertThat(new java.math.BigDecimal(json.readTree(original).get("result").toString())).isEqualByComparingTo("8.30");
        jdbc.update("insert into nite_connect.role_assignments(member_id,role,granted_by,reason) values (?,'GESTOR_GERAL',?,'Fixture')",OTHER,ADMIN);
        postAs(OTHER,"/api/v1/academic/calculations/"+c+"/homologations",command(ev(e)),403);
        UUID h=identifier(postAs(ADMIN,"/api/v1/academic/calculations/"+c+"/homologations",command(ev(e)),201));
        postAs(OTHER,"/api/v1/academic/homologations/"+h+"/reopen",command(ev(e)),403);
        postAs(ADMIN,"/api/v1/academic/calculations/"+c+"/homologations",command(ev(e)),409);
        postAs(ADMIN,"/api/v1/academic/homologations/"+h+"/forwardings",Map.of("expectedVersion",ev(e),"confirmed",true,"receiptReference","Protocolo teste","reason","Envio"),409);
        assertThat(jdbc.queryForObject("select data::text from nite_connect.academic_calculations where id=?",String.class,c)).isEqualTo(original);
        UUID review=identifier(postAs(MEMBER,"/api/v1/me/academic/enrollments/"+e+"/reviews",Map.of("calculationId",c,"reason","Conferir minha avaliação"),201));
        postAs(ADMIN,"/api/v1/academic/reviews/"+review+"/decisions",Map.of("accepted",false,"expectedVersion",0,"confirmed",true,"reason","Insumos corretos"),200);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.academic_homologations where enrollment_id=?",Long.class,e)).isEqualTo(1);
    }
}
