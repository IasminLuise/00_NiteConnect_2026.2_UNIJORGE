package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import java.time.*;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(ActivityDeadlinesTest.FixedTime.class)
class ActivityDeadlinesTest extends ActivityTestSupport {
    @TestConfiguration static class FixedTime {
        @Bean @Primary Clock testActivityClock() { return Clock.fixed(Instant.parse("2026-10-17T02:00:00Z"),ZoneId.of("America/Bahia")); }
    }
    void due(UUID actor,UUID id,String date) throws Exception {
        mvc.perform(patch("/api/v1/activities/"+id).with(token(actor)).contentType("application/json")
                .content(command(id,",\"title\":\"Atividade\",\"description\":\"Objetivo\",\"dueOn\":"+(date==null?"null":"\""+date+"\""))))
                .andExpect(status().isOk());
    }
    @Test void institutionalDayAndOverdueFilterUseSameClock() throws Exception {
        UUID yesterday=activity(MEMBER,null,MEMBER);due(MEMBER,yesterday,"2026-10-15");
        UUID today=activity(MEMBER,null,MEMBER);due(MEMBER,today,"2026-10-16");
        activity(MEMBER,null,MEMBER);
        mvc.perform(get("/api/v1/activities").param("overdue","true").with(token(MEMBER))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].id").value(yesterday.toString()));
        mvc.perform(get("/api/v1/activities/"+today).with(token(MEMBER))).andExpect(jsonPath("$.overdue").value(false));
        state(yesterday,MEMBER,"EM_EXECUCAO");state(yesterday,MEMBER,"CONCLUIDA");
        mvc.perform(get("/api/v1/activities").param("overdue","true").with(token(MEMBER))).andExpect(jsonPath("$.total").value(0));
    }
    @Test void pausedProjectKeepsObjectiveOverdueButSuppressesAttention() throws Exception {
        UUID p=activeProject(),id=activity(LEADER,p,MEMBER);due(LEADER,id,"2026-10-15");
        mvc.perform(get("/api/v1/activities/"+id).with(token(LEADER))).andExpect(jsonPath("$.attentionRequired").value(true));
        mvc.perform(patch("/api/v1/projects/"+p+"/status").with(token(ADMIN)).contentType("application/json")
                .content("{\"status\":\"PAUSADO\",\"expectedVersion\":"+version(p)+",\"reason\":\"Pausa\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/activities/"+id).with(token(LEADER))).andExpect(jsonPath("$.overdue").value(true)).andExpect(jsonPath("$.attentionRequired").value(false));
    }
    @Test void dueChangeHistoryPreservesOldValueAndInvalidRangeIsRejected() throws Exception {
        UUID id=activity(MEMBER,null,MEMBER);due(MEMBER,id,"2026-10-15");due(MEMBER,id,"2026-10-20");
        String before=jdbc.queryForObject("select before_value from nite_connect.activity_events where activity_id=? and activity_version=2",String.class,id);
        assertThat(before).contains("2026-10-15");
        mvc.perform(get("/api/v1/activities").param("dueFrom","2026-10-20").param("dueTo","2026-10-01").with(token(MEMBER))).andExpect(status().isBadRequest());
    }
}
