package br.edu.unijorge.niteconnect;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.edu.unijorge.niteconnect.academic.application.AcademicTermService;
import br.edu.unijorge.niteconnect.academic.api.AcademicContracts.CreateTerm;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AcademicBoundaryTest extends AcademicTestSupport {
    @Autowired AcademicTermService terms;
    @Test void optimisticVersionIsMandatoryRatherThanImplicitZero() throws Exception {
        postAs(ADMIN,"/api/v1/academic/terms/"+term()+"/policies",Map.of("rules",rules(),"reason","Versão ausente"),400);
    }
    @Test void incompleteAndInvalidPoliciesCannotBeConfirmed() throws Exception {
        UUID t=term();var invalid=new HashMap<>(rules());invalid.put("weights",Map.of("DOCUMENTATION",30,"PRESENTATIONS",20,"DELIVERIES",60));
        UUID p=identifier(postAs(ADMIN,"/api/v1/academic/terms/"+t+"/policies",Map.of("rules",invalid,"expectedVersion",0,"reason","Rascunho"),201));
        postAs(ADMIN,"/api/v1/academic/policies/"+p+"/confirmation",Map.of("expectedVersion",0,"confirmed",true,"authorityReference","Teste","reason","Teste"),400);
        UUID empty=identifier(postAs(ADMIN,"/api/v1/academic/terms/"+t+"/policies",Map.of("rules",Map.of(),"expectedVersion",0,"reason","Rascunho incompleto"),201));
        postAs(ADMIN,"/api/v1/academic/policies/"+empty+"/confirmation",Map.of("expectedVersion",0,"confirmed",true,"authorityReference","Teste","reason","Teste"),400);
    }
    @Test void identityAndCurrentRoleAreEnforcedAtServiceAndHttpBoundaries() throws Exception {
        mvc.perform(get("/api/v1/academic/terms")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/academic/terms").with(token(ADMIN)).param("size","100000")).andExpect(status().isBadRequest());
        jdbc.update("delete from nite_connect.role_assignments where member_id=?",ADMIN);
        assertThatThrownBy(()->terms.create(ADMIN,new CreateTerm("Teste",java.time.Instant.now(),java.time.Instant.now().plusSeconds(3600),"UTC","Teste")))
                .isInstanceOfSatisfying(br.edu.unijorge.niteconnect.shared.api.ApiException.class,ex->assertThat(ex.status().value()).isEqualTo(403));
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.academic_terms where title='Teste'",Long.class)).isZero();
    }
    @Test void activeMembersCanDiscoverConfirmedPoliciesWithoutSeeingDrafts() throws Exception {
        UUID t=term();policy(t);
        postAs(ADMIN,"/api/v1/academic/terms/"+t+"/policies",Map.of("rules",Map.of(),"expectedVersion",0,"reason","Privado"),201);
        mvc.perform(get("/api/v1/academic/terms/"+t+"/policies").with(token(MEMBER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].status").value("CONFIRMED"));
    }
}
