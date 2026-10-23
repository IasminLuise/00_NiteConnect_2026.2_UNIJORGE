package br.edu.unijorge.niteconnect;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import br.edu.unijorge.niteconnect.attendance.application.AttendanceAcademicReader;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AttendanceAcademicReaderTest extends AttendanceTestSupport {
    @Autowired AttendanceAcademicReader reader;
    @Test void partialIntervalSeparatesDeclarationAndValidatedDuration() throws Exception {
        Instant start=Instant.now().minusSeconds(7200),end=start.plusSeconds(3600),from=start.plusSeconds(900),to=end.minusSeconds(900);
        String response=mvc.perform(post("/api/v1/me/work-logs").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("startsAt",start.toString(),"endsAt",end.toString(),"description","Declaração sintética"))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id=UUID.fromString(json.readTree(response).get("id").asString());
        var declared=reader.read(ADMIN,MEMBER,id,from,to);
        assertThat(declared.attributes().get("registeredSeconds")).isEqualTo("1800");assertThat(declared.attributes()).doesNotContainKey("validatedSeconds");
        mvc.perform(post("/api/v1/attendance/records/"+id+"/reviews").with(token(ADMIN)).contentType("application/json")
                .content("{\"expectedVersion\":0,\"status\":\"APROVADA\",\"reason\":\"Conferência sintética\"}")).andExpect(status().isOk());
        assertThat(reader.read(ADMIN,MEMBER,id,from,to).attributes().get("validatedSeconds")).isEqualTo("1800");
        assertThatThrownBy(()->reader.read(ADMIN,MEMBER,id,end.plusSeconds(1),end.plusSeconds(60))).isInstanceOf(br.edu.unijorge.niteconnect.shared.api.ApiException.class);
    }
    @Test void openPeriodHasNoConsolidatedDurationAndLeadershipDoesNotAuthorizeOtherMembers() throws Exception {
        UUID event=event();UUID attendance=checkIn(event,qr(event));
        UUID period=jdbc.queryForObject("select id from nite_connect.participation_periods where attendance_id=?",UUID.class,attendance);
        Instant from=Instant.now().minusSeconds(3600),to=Instant.now().plusSeconds(3600);
        var snapshot=reader.read(ADMIN,MEMBER,period,from,to);
        assertThat(snapshot.attributes().get("open")).isEqualTo("true");
        assertThat(snapshot.attributes()).doesNotContainKeys("registeredSeconds","validatedSeconds");
        assertThatThrownBy(()->reader.read(LEADER,MEMBER,period,from,to)).isInstanceOfSatisfying(br.edu.unijorge.niteconnect.shared.api.ApiException.class,e->assertThat(e.status().value()).isEqualTo(403));
    }
}
