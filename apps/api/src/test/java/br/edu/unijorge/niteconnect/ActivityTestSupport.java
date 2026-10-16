package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

abstract class ActivityTestSupport extends ProjectTestSupport {
    UUID activity(UUID actor, UUID project, UUID executor) throws Exception {
        mvc.perform(post("/api/v1/activities").with(token(actor)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Atividade\",\"description\":\"Objetivo\",\"projectId\":"
                        + (project == null ? "null" : "\"" + project + "\"") + ",\"primaryExecutorId\":\"" + executor
                        + "\",\"reason\":\"Planejamento\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PLANEJADA"));
        return jdbc.queryForObject("select id from nite_connect.activities order by created_at desc limit 1", UUID.class);
    }
    long av(UUID id) { return jdbc.queryForObject("select version from nite_connect.activities where id=?", Long.class, id); }
    String command(UUID id, String extra) { return "{\"expectedVersion\":" + av(id) + ",\"reason\":\"Ajuste\"" + extra + "}"; }
    void state(UUID id, UUID actor, String next) throws Exception {
        mvc.perform(patch("/api/v1/activities/" + id + "/status").with(token(actor)).contentType(MediaType.APPLICATION_JSON)
                .content(command(id, ",\"status\":\"" + next + "\",\"confirmed\":true"))).andExpect(status().isOk());
    }
    UUID activeProject() throws Exception {
        UUID p = project(); join(p, LEADER); join(p, MEMBER); join(p, OTHER); transfer(p, LEADER);
        mvc.perform(patch("/api/v1/projects/" + p + "/status").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ATIVO\",\"expectedVersion\":" + version(p) + ",\"reason\":\"Iniciar\"}"))
                .andExpect(status().isOk());
        return p;
    }
}
