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
@Import(AcademicSyntheticSupport.Documents.class)
class AcademicHomologationTest extends AcademicSyntheticSupport {
    @Test void changedOriginPreventsHomologationWithoutRewritingTheCalculation() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);UUID c=completedCalculation(e);
        String before=jdbc.queryForObject("select data::text from nite_connect.academic_calculations where id=?",String.class,c);
        syntheticDocumentRevision.incrementAndGet();
        postAs(ADMIN,"/api/v1/academic/calculations/"+c+"/homologations",command(ev(e)),409);
        assertThat(jdbc.queryForObject("select data::text from nite_connect.academic_calculations where id=?",String.class,c)).isEqualTo(before);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.academic_homologations where enrollment_id=?",Long.class,e)).isZero();
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
