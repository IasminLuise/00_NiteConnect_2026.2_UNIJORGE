package br.edu.unijorge.niteconnect;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(AcademicSyntheticSupport.Documents.class)
class AcademicPrivacyTest extends AcademicSyntheticSupport {
    void manager() { jdbc.update("insert into nite_connect.role_assignments(member_id,role,granted_by,reason) values (?,'GESTOR_GERAL',?,'Fixture')",OTHER,ADMIN); }
    @Test void managerGetsPreparationProjectionOnReadAndCalculateWithoutPedagogicalContent() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);completedCalculation(e);manager();
        mvc.perform(get("/api/v1/academic/enrollments/"+e+"/assessments").with(token(OTHER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].reason").doesNotExist()).andExpect(jsonPath("$.items[0].score").doesNotExist()).andExpect(jsonPath("$.items[0].evidenceIds").isEmpty());
        mvc.perform(get("/api/v1/academic/enrollments/"+e+"/calculations").with(token(OTHER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].data.assessments").isEmpty()).andExpect(jsonPath("$.items[0].data.evidence").isEmpty());
        var response=postAs(OTHER,"/api/v1/academic/enrollments/"+e+"/calculations",command(ev(e)),201);
        org.assertj.core.api.Assertions.assertThat(response.get("data").get("assessments").size()).isZero();
        org.assertj.core.api.Assertions.assertThat(response.get("data").get("evidence").size()).isZero();
    }
    @Test void ownAdministrativeReviewDoesNotLeakPrivateReasonsBeforeDisclosure() throws Exception {
        var r=new HashMap<>(rules());r.put("discloseResults",false);UUID e=enrollment(policy(term(),r));approve(e);UUID c=completedCalculation(e);
        postAs(ADMIN,"/api/v1/academic/enrollments/"+e+"/reviews",Map.of("calculationId",c,"reason","Discussão interna sobre nota e documento privado"),201);
        mvc.perform(get("/api/v1/me/academic/enrollments/"+e+"/reviews").with(token(MEMBER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].reason").doesNotExist()).andExpect(jsonPath("$.items[0].decisionReason").doesNotExist());
    }
    @Test void capturedEvidenceContentIsNotExposedByAdministrativePreparationListing() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);completedCalculation(e);manager();
        mvc.perform(get("/api/v1/academic/enrollments/"+e+"/evidence").with(token(OTHER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].snapshot.attributes").isEmpty());
    }
    @Test void publishingSecondCalculationDoesNotPublishDiscardedDraftsOrAssessments() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);UUID first=completedCalculation(e);
        UUID reference=jdbc.queryForObject("select x.evidence_id from nite_connect.academic_assessment_evidence x join nite_connect.academic_assessments a on a.id=x.assessment_id where a.enrollment_id=? and a.pillar='PRESENTATIONS'",UUID.class,e);
        postAs(ADMIN,"/api/v1/academic/enrollments/"+e+"/assessments",Map.of("pillar","PRESENTATIONS","score",6,"evidenceIds",List.of(reference),"expectedVersion",ev(e),"reason","Revisão privada descartando primeira nota"),201);
        UUID second=identifier(postAs(ADMIN,"/api/v1/academic/enrollments/"+e+"/calculations",command(ev(e)),201));
        postAs(ADMIN,"/api/v1/academic/calculations/"+second+"/homologations",command(ev(e)),201);
        mvc.perform(get("/api/v1/me/academic/enrollments/"+e+"/calculations").with(token(MEMBER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].id").value(second.toString()))
                .andExpect(jsonPath("$.items[0].data.assessments").isEmpty()).andExpect(jsonPath("$.items[0].data.evidence").isEmpty());
        mvc.perform(get("/api/v1/me/academic/enrollments/"+e+"/assessments").with(token(MEMBER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.items[0].reason").doesNotExist());
    }
    @Test void studentReceivesIntendedCorrectionFeedbackWithoutPrivateAudit() throws Exception {
        UUID e=enrollment(policy(term()));
        postAs(ADMIN,"/api/v1/academic/enrollments/"+e+"/decisions",Map.of("decision","ANALYZE","expectedVersion",ev(e),"reason","Análise interna"),200);
        postAs(ADMIN,"/api/v1/academic/enrollments/"+e+"/decisions",Map.of("decision","REQUEST_CORRECTION","expectedVersion",ev(e),"reason","Raciocínio administrativo privado","feedback","Confira o código da disciplina"),200);
        mvc.perform(get("/api/v1/me/academic/enrollments/"+e).with(token(MEMBER))).andExpect(status().isOk()).andExpect(jsonPath("$.feedback").value("Confira o código da disciplina"));
        mvc.perform(put("/api/v1/me/academic/enrollments/"+e).with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("disciplines",List.of(Map.of("courseCode","COMPUTACAO","code","ALG")),"expectedVersion",ev(e),"reason","Corrigido")))).andExpect(status().isOk());
        approve(e);
        UUID plan=identifier(postAs(MEMBER,"/api/v1/me/academic/enrollments/"+e+"/plans",Map.of("content","Objetivos","expectedVersion",ev(e),"reason","Plano"),201));
        postAs(MEMBER,"/api/v1/me/academic/plans/"+plan+"/submission",command(0),200);
        postAs(ADMIN,"/api/v1/academic/plans/"+plan+"/decisions",Map.of("decision","ANALYZE","expectedVersion",1,"reason","Análise"),200);
        postAs(ADMIN,"/api/v1/academic/plans/"+plan+"/decisions",Map.of("decision","REQUEST_CORRECTION","expectedVersion",2,"reason","Nota interna","feedback","Detalhe o objetivo técnico"),200);
        mvc.perform(get("/api/v1/me/academic/enrollments/"+e+"/plans").with(token(MEMBER))).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].feedback").value("Detalhe o objetivo técnico"));
    }
    @Test void managerAuditProjectionContainsNoPrivateSnapshotsOrReasons() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);manager();
        mvc.perform(get("/api/v1/academic/enrollments/"+e+"/history").with(token(OTHER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].beforeValue").doesNotExist()).andExpect(jsonPath("$.items[0].afterValue").doesNotExist()).andExpect(jsonPath("$.items[0].reason").doesNotExist());
    }
}
