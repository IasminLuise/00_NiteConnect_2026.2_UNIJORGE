package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

abstract class AttendanceTestSupport extends ActivityTestSupport {
    final tools.jackson.databind.ObjectMapper json=tools.jackson.databind.json.JsonMapper.builder().build();

    UUID event() throws Exception {
        String body=json.writeValueAsString(Map.of("title","Encontro","description","Sessão oficial","kind","ENCONTRO",
                "modality","PRESENCIAL","startsAt",Instant.now().minusSeconds(300).toString(),
                "endsAt",Instant.now().plusSeconds(3600).toString(),"expectedMemberIds",List.of(MEMBER),"reason","Programação"));
        String response=mvc.perform(post("/api/v1/agenda/events").with(token(ADMIN)).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).get("id").asString());
    }
    long ev(UUID id) { return jdbc.queryForObject("select version from nite_connect.calendar_events where id=?",Long.class,id); }
    String qr(UUID event) throws Exception {
        String response=mvc.perform(post("/api/v1/agenda/events/"+event+"/session").with(token(ADMIN)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion",ev(event),"reason","Abrir sessão"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("payload").asString();
    }
    UUID checkIn(UUID event,String payload) throws Exception {
        String response=mvc.perform(post("/api/v1/me/attendance/check-in").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId",event,"payload",payload))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).get("id").asString());
    }
}
