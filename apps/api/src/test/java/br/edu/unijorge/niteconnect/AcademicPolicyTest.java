package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AcademicPolicyTest extends ProjectTestSupport {
    final tools.jackson.databind.ObjectMapper json = JsonMapper.builder().build();

    @Test void coordinatorCreatesTermAndLeadershipDoesNotGrantAcademicAdministration() throws Exception {
        String command = json.writeValueAsString(Map.of("title", "2026.2", "startsAt", "2026-07-01T00:00:00Z",
                "endsAt", "2027-01-01T00:00:00Z", "zone", "America/Bahia", "reason", "Calendário aprovado"));
        mvc.perform(post("/api/v1/academic/terms").with(token(ADMIN)).contentType("application/json").content(command))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.title").value("2026.2"));
        mvc.perform(post("/api/v1/academic/terms").with(token(LEADER)).contentType("application/json").content(command))
                .andExpect(status().isForbidden());
    }

    @Test void globalManagerCannotCreateTermAndUnknownAcademicOperationsRemainDenied() throws Exception {
        jdbc.update("insert into nite_connect.role_assignments(member_id,role,granted_by,reason) values (?,'GESTOR_GERAL',?,'Fixture')", OTHER, ADMIN);
        mvc.perform(post("/api/v1/academic/terms").with(token(OTHER)).contentType("application/json")
                .content("{\"title\":\"Semestre\",\"startsAt\":\"2026-01-01T00:00:00Z\",\"endsAt\":\"2027-01-01T00:00:00Z\",\"zone\":\"UTC\",\"reason\":\"Teste\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/academic/unknown").with(token(ADMIN))).andExpect(status().isForbidden());
    }
}
