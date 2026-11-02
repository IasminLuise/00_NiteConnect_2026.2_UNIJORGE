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

    @Test void frequencyConfirmationCanBeCapturedAsTheNewAdditiveAttendanceEvidenceSource() throws Exception {
        UUID e=enrollment(policy(term()));approve(e);
        UUID attendance=frequencyAttendance();
        UUID capture=identifier(postAs(MEMBER,"/api/v1/me/academic/enrollments/"+e+"/evidence",
                Map.of("source","ATTENDANCE_CONFIRMATION","sourceId",attendance,"expectedVersion",ev(e),"reason","Presença confirmada"),201));
        String snapshot=jdbc.queryForObject("select snapshot::text from nite_connect.academic_evidence where id=?",String.class,capture);
        assertThat(snapshot).contains("ATTENDANCE_CONFIRMATION","PRESENCA_FREQUENCIA","confirmedAt")
                .doesNotContain("durationSeconds","registeredSeconds","validatedSeconds");
    }

    private UUID frequencyAttendance() throws Exception {
        UUID event=identifier(postAs(ADMIN,"/api/v1/agenda/events",Map.of("title","Presença B06","description","Fixture acadêmica",
                "kind","EVENTO","modality","REMOTA","startsAt",Instant.now().minusSeconds(30).toString(),
                "endsAt",Instant.now().plusSeconds(3600).toString(),"expectedMemberIds",List.of(MEMBER),"reason","Fixture"),201));
        String openBody=json.writeValueAsString(Map.of("calendarEventId",event,"confirmationWindowMinutes",3,"countsForFrequency",true));
        String openResponse=mvc.perform(post("/api/v1/attendance/lists").with(token(ADMIN)).header("Idempotency-Key",UUID.randomUUID().toString())
                        .contentType("application/json").content(openBody))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID list=UUID.fromString(json.readTree(openResponse).get("listId").asString());
        String qrResponse=mvc.perform(post("/api/v1/attendance/lists/"+list+"/qr/renew").with(token(ADMIN))
                        .contentType("application/json").content("{\"expectedVersion\":0}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String qr=json.readTree(qrResponse).get("qrToken").asString();
        var confirmation=postAs(MEMBER,"/api/v1/me/frequency/confirmations",Map.of("qrToken",qr),201);
        return UUID.fromString(confirmation.get("attendanceId").asString());
    }
}
