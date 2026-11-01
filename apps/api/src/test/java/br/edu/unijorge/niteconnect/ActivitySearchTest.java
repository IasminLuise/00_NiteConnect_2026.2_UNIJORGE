package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ActivitySearchTest extends ActivityTestSupport {
    @Test void searchIsPersonalLiteralAndPaginated() throws Exception {
        UUID one=activity(MEMBER,null,MEMBER), two=activity(MEMBER,null,MEMBER), hidden=activity(OTHER,null,OTHER);
        jdbc.update("update nite_connect.activities set title=?,description=? where id=?", "Revisão 100%", "Primeira entrega",one);
        jdbc.update("update nite_connect.activities set title=?,description=? where id=?", "Segunda", "Revisão 100% concluída",two);
        jdbc.update("update nite_connect.activities set title=? where id=?", "Revisão 100% privada",hidden);
        mvc.perform(get("/api/v1/me/activities").with(token(MEMBER)).param("q","  REVISÃO  ").param("size","1").param("page","0"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get("/api/v1/me/activities").with(token(MEMBER)).param("q","revisão").param("size","1").param("page","1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.items.length()").value(1));
        mvc.perform(get("/api/v1/me/activities").with(token(MEMBER)).param("q","%_")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/me/activities").with(token(MEMBER)).param("q","% ")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/me/activities").with(token(MEMBER)).param("q","x".repeat(161))).andExpect(status().isBadRequest());
    }
    @Test void searchCombinesProjectStatusAndDeadlines() throws Exception {
        UUID project=activeProject(), match=activity(LEADER,project,MEMBER), other=activity(MEMBER,null,MEMBER);
        jdbc.update("update nite_connect.activities set description=?,due_on=? where id=?", "Termo comum", java.time.LocalDate.of(2020,1,5),match);
        jdbc.update("update nite_connect.activities set description=? where id=?", "Termo comum",other);
        mvc.perform(get("/api/v1/me/activities").with(token(MEMBER)).param("q","termo").param("projectId",project.toString())
                .param("status","PLANEJADA").param("dueFrom","2020-01-01").param("dueTo","2020-01-31").param("overdue","true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].id").value(match.toString()));
        mvc.perform(get("/api/v1/me/activities").with(token(OTHER)).param("q","termo").param("projectId",project.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }
}
