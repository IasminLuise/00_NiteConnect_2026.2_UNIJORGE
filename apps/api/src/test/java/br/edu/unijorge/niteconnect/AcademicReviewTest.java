package br.edu.unijorge.niteconnect;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.*;

@Import(AcademicSyntheticSupport.Documents.class)
class AcademicReviewTest extends AcademicSyntheticSupport {
    @Test void reopeningCreatesAuditedSuccessorAndNeverMutatesPriorHomologation() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);UUID c=completedCalculation(e);
        UUID h=identifier(postAs(ADMIN,"/api/v1/academic/calculations/"+c+"/homologations",command(ev(e)),201));
        String old=jdbc.queryForObject("select row_to_json(h)::text from nite_connect.academic_homologations h where id=?",String.class,h);
        postAs(ADMIN,"/api/v1/academic/homologations/"+h+"/reopen",command(ev(e)),201);
        UUID next=identifier(postAs(ADMIN,"/api/v1/academic/enrollments/"+e+"/calculations",command(ev(e)),201));
        UUID successor=identifier(postAs(ADMIN,"/api/v1/academic/calculations/"+next+"/homologations",command(ev(e)),201));
        assertThat(jdbc.queryForObject("select supersedes_id from nite_connect.academic_homologations where id=?",UUID.class,successor)).isEqualTo(h);
        assertThat(jdbc.queryForObject("select row_to_json(h)::text from nite_connect.academic_homologations h where id=?",String.class,h)).isEqualTo(old);
    }
    @Test void acceptedStudentReviewReopensWithoutErasingDecisionAndDuplicatesAreRejected() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);UUID c=completedCalculation(e);
        postAs(ADMIN,"/api/v1/academic/calculations/"+c+"/homologations",command(ev(e)),201);
        UUID r=identifier(postAs(MEMBER,"/api/v1/me/academic/enrollments/"+e+"/reviews",Map.of("calculationId",c,"reason","Conferência"),201));
        postAs(MEMBER,"/api/v1/me/academic/enrollments/"+e+"/reviews",Map.of("calculationId",c,"reason","Duplicada"),409);
        postAs(OTHER,"/api/v1/academic/reviews/"+r+"/decisions",Map.of("accepted",true,"expectedVersion",0,"confirmed",true,"reason","Tentativa"),403);
        postAs(ADMIN,"/api/v1/academic/reviews/"+r+"/decisions",Map.of("accepted",true,"expectedVersion",0,"confirmed",true,"reason","Revisar pontuação"),200);
        assertThat(jdbc.queryForObject("select status from nite_connect.academic_enrollments where id=?",String.class,e)).isEqualTo("EM_ACOMPANHAMENTO");
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.academic_homologations where enrollment_id=?",Long.class,e)).isEqualTo(1);
    }
}
