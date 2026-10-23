package br.edu.unijorge.niteconnect;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AcademicEvidenceTest extends AcademicTestSupport {
    @Test void completedActivitySnapshotDoesNotChangeWhenOriginChanges() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);
        UUID a=activity(MEMBER,null,MEMBER);
        UUID source=identifier(postAs(MEMBER,"/api/v1/activities/"+a+"/evidence",Map.of("type","TEXT","content","Contribuição individual verificada","expectedVersion",av(a),"reason","Entrega"),201));
        state(a,MEMBER,"EM_EXECUCAO");state(a,MEMBER,"CONCLUIDA");
        UUID capture=identifier(postAs(MEMBER,"/api/v1/me/academic/enrollments/"+e+"/evidence",Map.of("source","ACTIVITY","sourceId",source,"expectedVersion",ev(e),"reason","Relacionar"),201));
        String before=jdbc.queryForObject("select snapshot::text from nite_connect.academic_evidence where id=?",String.class,capture);
        state(a,MEMBER,"EM_EXECUCAO");
        mvc.perform(get("/api/v1/academic/evidence/"+capture+"/comparison").with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.changed").value(true));
        assertThat(jdbc.queryForObject("select snapshot::text from nite_connect.academic_evidence where id=?",String.class,capture)).isEqualTo(before);
        mvc.perform(get("/api/v1/me/academic/enrollments/"+e+"/evidence").with(token(OTHER))).andExpect(status().isForbidden());
    }
    @Test void missingDocumentProviderBlocksCaptureAndPresentationRequiresCoordinator() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);
        postAs(MEMBER,"/api/v1/me/academic/enrollments/"+e+"/evidence",Map.of("source","DOCUMENT","sourceId",UUID.randomUUID(),"expectedVersion",ev(e),"reason","Documento"),409);
        var body=Map.of("title","Apresentação sintética","occurredAt",Instant.now().minusSeconds(60).toString(),"reference","Ata sintética","expectedVersion",ev(e),"reason","Conferida presencialmente");
        postAs(MEMBER,"/api/v1/academic/enrollments/"+e+"/presentations",body,403);
        postAs(ADMIN,"/api/v1/academic/enrollments/"+e+"/presentations",body,201);
    }
}
