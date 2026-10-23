package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AcademicEnrollmentTest extends AcademicTestSupport {
    @Test void optInIsUniqueAndOnlyCoordinatorApproves() throws Exception {
        UUID p=policy(term());
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.academic_enrollments",Long.class)).isZero();
        UUID e=enrollment(p);
        postAs(MEMBER,"/api/v1/me/academic/enrollments",Map.of("policyId",p,"disciplines",List.of(Map.of("courseCode","COMPUTACAO","code","ALG")),"reason","Duplicada"),409);
        jdbc.update("insert into nite_connect.role_assignments(member_id,role,granted_by,reason) values (?,'GESTOR_GERAL',?,'Fixture')",OTHER,ADMIN);
        postAs(OTHER,"/api/v1/academic/enrollments/"+e+"/decisions",Map.of("decision","APPROVE","expectedVersion",ev(e),"reason","Tentativa"),403);
        approve(e);
        mvc.perform(get("/api/v1/me/academic/enrollments/"+e).with(token(OTHER))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/me/academic/enrollments/"+e).with(token(MEMBER))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DEFERIDA"));
    }
    @Test void duplicateNormalizedDisciplinesAndInactiveMemberAreRejected() throws Exception {
        UUID p=policy(term());
        postAs(MEMBER,"/api/v1/me/academic/enrollments",Map.of("policyId",p,"reason","Teste","disciplines",List.of(Map.of("courseCode","computacao","code","alg"),Map.of("courseCode","COMPUTACAO","code","ALG"))),400);
        jdbc.update("update nite_connect.members set status='AFASTADO' where id=?",MEMBER);
        postAs(MEMBER,"/api/v1/me/academic/enrollments",Map.of("policyId",p,"reason","Teste","disciplines",List.of(Map.of("courseCode","COMPUTACAO","code","ALG"))),403);
    }
}
