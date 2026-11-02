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
        String body=json.writeValueAsString(Map.of("title","Encontro","description","Sessão oficial","kind","EVENTO",
                "modality","REMOTA","startsAt",Instant.now().minusSeconds(300).toString(),
                "endsAt",Instant.now().plusSeconds(3600).toString(),"expectedMemberIds",List.of(MEMBER),"reason","Programação"));
        String response=mvc.perform(post("/api/v1/agenda/events").with(token(ADMIN)).contentType("application/json").content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).get("id").asString());
    }
    long ev(UUID id) { return jdbc.queryForObject("select version from nite_connect.calendar_events where id=?",Long.class,id); }
    UUID openList(UUID event) throws Exception {
        Map<String,Object> command=new HashMap<>();command.put("calendarEventId",event);command.put("confirmationWindowMinutes",3);
        command.put("countsForFrequency",true);
        String response=mvc.perform(post("/api/v1/attendance/lists").with(token(ADMIN)).header("Idempotency-Key",UUID.randomUUID().toString())
                .contentType("application/json").content(json.writeValueAsString(command)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).get("listId").asString());
    }
    String qrForList(UUID list) throws Exception {
        long version=jdbc.queryForObject("select version from nite_connect.attendance_lists where id=?",Long.class,list);
        String response=mvc.perform(post("/api/v1/attendance/lists/"+list+"/qr/renew").with(token(ADMIN)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("expectedVersion",version))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("qrToken").asString();
    }
    UUID listForEvent(UUID event) {
        return jdbc.queryForObject("select id from nite_connect.attendance_lists where calendar_event_id=?",UUID.class,event);
    }
    String qr(UUID event) throws Exception {
        return qrForList(openList(event));
    }
    UUID checkIn(UUID event,String payload) throws Exception {
        String response=mvc.perform(post("/api/v1/me/attendance/check-in").with(token(MEMBER)).contentType("application/json")
                .content(json.writeValueAsString(Map.of("eventId",event,"payload",payload))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(response).get("attendanceId").asString());
    }
    UUID legacyAttendance(UUID event,UUID member) {
        UUID attendance=UUID.randomUUID();UUID segment=UUID.randomUUID();
        jdbc.update("insert into nite_connect.attendance_records(id,event_id,member_id) values (?,?,?)",attendance,event,member);
        jdbc.update("insert into nite_connect.attendance_segments(id,member_id,attendance_id,starts_at,ends_at) values (?,?,?,current_timestamp-interval '10 minutes',current_timestamp)",segment,member,attendance);
        return attendance;
    }
}
