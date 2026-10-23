package br.edu.unijorge.niteconnect;

import java.time.Instant;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

abstract class AcademicTestSupport extends ActivityTestSupport {
    final tools.jackson.databind.ObjectMapper json=JsonMapper.builder().build();
    JsonNode postAs(UUID actor,String path,Object data,int expected) throws Exception {
        String body=mvc.perform(post(path).with(token(actor)).contentType("application/json").content(json.writeValueAsString(data)))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return body.isEmpty()?json.createObjectNode():json.readTree(body);
    }
    UUID identifier(JsonNode node) { return UUID.fromString(node.get("id").asString()); }
    Map<String,Object> command(long version) { return Map.of("expectedVersion",version,"reason","Decisão sintética","confirmed",true); }
    UUID term() throws Exception {
        return identifier(postAs(ADMIN,"/api/v1/academic/terms",Map.of("title","Semestre sintético","startsAt",Instant.now().minusSeconds(86400).toString(),
                "endsAt",Instant.now().plusSeconds(86400*90).toString(),"zone","America/Bahia","reason","Fixture"),201));
    }
    Map<String,Object> rules() {
        return Map.ofEntries(Map.entry("weights",Map.of("DOCUMENTATION",30,"PRESENTATIONS",20,"DELIVERIES",50)),Map.entry("minimumScore",0),Map.entry("maximumScore",10),
                Map.entry("scale",2),Map.entry("rounding","HALF_UP"),Map.entry("eligibleDisciplines",List.of(Map.of("courseCode","COMPUTACAO","code","ALG"))),
                Map.entry("maximumDisciplines",2),Map.entry("enrollmentOpensAt",Instant.now().minusSeconds(3600).toString()),
                Map.entry("enrollmentClosesAt",Instant.now().plusSeconds(86400).toString()),Map.entry("reviewWindowSeconds",86400),
                Map.entry("discloseResults",true),Map.entry("planRequired",false));
    }
    UUID policy(UUID term) throws Exception {
        UUID p=identifier(postAs(ADMIN,"/api/v1/academic/terms/"+term+"/policies",Map.of("rules",rules(),"expectedVersion",0,"reason","Fixture"),201));
        postAs(ADMIN,"/api/v1/academic/policies/"+p+"/confirmation",Map.of("expectedVersion",0,"confirmed",true,"authorityReference","Política sintética exclusiva de teste","reason","Fixture"),200);
        return p;
    }
    UUID enrollment(UUID p) throws Exception {
        return identifier(postAs(MEMBER,"/api/v1/me/academic/enrollments",Map.of("policyId",p,"disciplines",List.of(Map.of("courseCode","COMPUTACAO","code","ALG")),"reason","Solicitação voluntária"),201));
    }
    long ev(UUID id) { return jdbc.queryForObject("select version from nite_connect.academic_enrollments where id=?",Long.class,id); }
    void approve(UUID e) throws Exception {
        postAs(ADMIN,"/api/v1/academic/enrollments/"+e+"/decisions",Map.of("decision","ANALYZE","expectedVersion",ev(e),"reason","Conferir"),200);
        postAs(ADMIN,"/api/v1/academic/enrollments/"+e+"/decisions",Map.of("decision","APPROVE","expectedVersion",ev(e),"reason","Pertinência conferida"),200);
    }
}
