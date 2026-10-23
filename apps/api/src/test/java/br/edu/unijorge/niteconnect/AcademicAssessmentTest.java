package br.edu.unijorge.niteconnect;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AcademicAssessmentTest extends AcademicTestSupport {
    @Test void zeroIsAnAssessmentButMissingPillarsPreventCalculation() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);
        UUID presentation=identifier(postAs(ADMIN,"/api/v1/academic/enrollments/"+e+"/presentations",Map.of("title","Progresso","occurredAt",Instant.now().minusSeconds(60).toString(),"reference","Ata teste","expectedVersion",ev(e),"reason","Atestação"),201));
        postAs(ADMIN,"/api/v1/academic/enrollments/"+e+"/assessments",Map.of("pillar","PRESENTATIONS","score",0,"evidenceIds",List.of(presentation),"expectedVersion",ev(e),"reason","Nota zero atribuída"),201);
        assertThat(jdbc.queryForObject("select score from nite_connect.academic_assessments where enrollment_id=?",java.math.BigDecimal.class,e)).isEqualByComparingTo("0");
        postAs(ADMIN,"/api/v1/academic/enrollments/"+e+"/calculations",command(ev(e)),409);
        mvc.perform(get("/api/v1/me/academic/enrollments/"+e+"/calculations").with(token(MEMBER))).andExpect(status().isConflict());
    }
    @Test void globalManagerCannotAssignScoreEvenThroughDirectEndpoint() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);
        jdbc.update("insert into nite_connect.role_assignments(member_id,role,granted_by,reason) values (?,'GESTOR_GERAL',?,'Fixture')",OTHER,ADMIN);
        postAs(OTHER,"/api/v1/academic/enrollments/"+e+"/assessments",Map.of("pillar","DELIVERIES","score",9,"evidenceIds",List.of(UUID.randomUUID()),"expectedVersion",ev(e),"reason","Tentativa"),403);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.academic_assessments where enrollment_id=?",Long.class,e)).isZero();
    }
}
