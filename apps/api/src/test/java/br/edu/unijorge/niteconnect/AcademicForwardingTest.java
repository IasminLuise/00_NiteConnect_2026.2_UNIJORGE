package br.edu.unijorge.niteconnect;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(AcademicSyntheticSupport.Documents.class)
class AcademicForwardingTest extends AcademicSyntheticSupport {
    @Override Map<String,Object> rules() { var r=new HashMap<>(super.rules());r.put("forwardingProcedureReference","Procedimento sintético exclusivo de teste");return r; }
    @Test void forwardingRecordsExactHomologationAndDoesNotConfirmUniversityPublication() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);UUID c=completedCalculation(e);
        UUID h=identifier(postAs(ADMIN,"/api/v1/academic/calculations/"+c+"/homologations",command(ev(e)),201));
        var body=Map.of("expectedVersion",ev(e),"confirmed",true,"receiptReference","Protocolo sintético","reason","Encaminhamento registrado");
        postAs(ADMIN,"/api/v1/academic/homologations/"+h+"/forwardings",body,201);
        assertThat(jdbc.queryForObject("select homologation_id from nite_connect.academic_forwardings where homologation_id=?",UUID.class,h)).isEqualTo(h);
        mvc.perform(post("/api/v1/academic/homologations/"+h+"/external-confirmation").with(token(ADMIN))).andExpect(status().isForbidden());
    }
}
