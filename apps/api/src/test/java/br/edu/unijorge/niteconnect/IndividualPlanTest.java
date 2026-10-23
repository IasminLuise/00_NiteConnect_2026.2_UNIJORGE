package br.edu.unijorge.niteconnect;

import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class IndividualPlanTest extends AcademicTestSupport {
    @Test void submissionApprovalAndNewRevisionPreservePreviousContent() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);
        UUID plan=identifier(postAs(MEMBER,"/api/v1/me/academic/enrollments/"+e+"/plans",Map.of("content","Objetivos originais","expectedVersion",ev(e),"reason","Planejar"),201));
        postAs(OTHER,"/api/v1/me/academic/plans/"+plan+"/submission",command(0),403);
        postAs(MEMBER,"/api/v1/me/academic/plans/"+plan+"/submission",command(0),200);
        postAs(ADMIN,"/api/v1/academic/plans/"+plan+"/decisions",Map.of("decision","ANALYZE","expectedVersion",1,"reason","Analisar"),200);
        postAs(ADMIN,"/api/v1/academic/plans/"+plan+"/decisions",Map.of("decision","APPROVE","expectedVersion",2,"reason","Aprovar"),200);
        postAs(MEMBER,"/api/v1/me/academic/enrollments/"+e+"/plans",Map.of("content","Objetivos revisados","expectedVersion",ev(e),"reason","Nova revisão"),201);
        assertThat(jdbc.queryForObject("select content from nite_connect.academic_plans where id=?",String.class,plan)).isEqualTo("Objetivos originais");
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.academic_plans where enrollment_id=?",Long.class,e)).isEqualTo(2);
    }
}
