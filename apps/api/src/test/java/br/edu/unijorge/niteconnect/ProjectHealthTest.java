package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.time.LocalDate;
import java.time.ZoneId;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProjectHealthTest extends ProjectTestSupport {
    @Test
    void projectLeaderCanReadHealthButCannotAdministerParameters() throws Exception {
        var projectId = project();
        join(projectId, LEADER);
        transfer(projectId, LEADER);

        mvc.perform(get("/api/v1/projects/" + projectId + "/health").with(token(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DADOS_INSUFICIENTES"))
                .andExpect(jsonPath("$.policyVersion").value(1))
                .andExpect(jsonPath("$.attentionOverdueDays").value(7))
                .andExpect(jsonPath("$.criticalOverdueDays").value(14))
                .andExpect(jsonPath("$.attentionNoUpdateDays").value(14))
                .andExpect(jsonPath("$.criticalNoUpdateDays").value(30));

        mvc.perform(post("/api/v1/projects/" + projectId + "/health-policy").with(token(LEADER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":1,"attentionOverdueDays":3,"criticalOverdueDays":10,
                                 "attentionNoUpdateDays":null,"criticalNoUpdateDays":null,"reason":"Configuração por projeto"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void policyChangesPreservePriorVersionsAndRequireExpectedVersion() throws Exception {
        var projectId = project();
        String endpoint = "/api/v1/projects/" + projectId + "/health-policy";

        mvc.perform(post(endpoint).with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":1,"attentionOverdueDays":3,"criticalOverdueDays":10,
                                 "attentionNoUpdateDays":null,"criticalNoUpdateDays":null,"reason":"Primeira configuração"}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(2));

        mvc.perform(post(endpoint).with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":2,"attentionOverdueDays":4,"criticalOverdueDays":12,
                                 "attentionNoUpdateDays":null,"criticalNoUpdateDays":null,"reason":"Revisão autorizada"}
                                """))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(3));

        mvc.perform(get("/api/v1/projects/" + projectId + "/health-policy/history").with(token(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].attentionOverdueDays").value(4))
                .andExpect(jsonPath("$.items[1].attentionOverdueDays").value(3))
                .andExpect(jsonPath("$.items[2].attentionOverdueDays").value(7))
                .andExpect(jsonPath("$.items[2].origin").value("BASELINE_APROVADA"));

        mvc.perform(post(endpoint).with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":1,"attentionOverdueDays":5,"criticalOverdueDays":15,
                                 "attentionNoUpdateDays":null,"criticalNoUpdateDays":null,"reason":"Versão obsoleta"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void unrelatedMemberCannotReadProjectHealth() throws Exception {
        var projectId = project();

        mvc.perform(get("/api/v1/projects/" + projectId + "/health").with(jwt().jwt(j -> j
                        .claim("tid", TENANT.toString()).claim("oid", OTHER.toString()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void evaluationExplainsConfiguredDelayAndRetainsItsSourceActivity() throws Exception {
        var projectId = project();
        mvc.perform(patch("/api/v1/projects/" + projectId + "/status").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ATIVO","expectedVersion":0,"reason":"Início autorizado"}
                                """))
                .andExpect(status().isOk());
        join(projectId, MEMBER);
        join(projectId, LEADER);
        transfer(projectId, LEADER);

        mvc.perform(post("/api/v1/activities").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Entrega atrasada","description":"Atividade operacional de teste",
                                 "projectId":"%s","dueOn":"%s","primaryExecutorId":"%s","reason":"Planejamento"}
                                """.formatted(projectId, LocalDate.now(ZoneId.of("America/Bahia")).minusDays(5), MEMBER)))
                .andExpect(status().isCreated());

        String endpoint = "/api/v1/projects/" + projectId + "/health-policy";
        mvc.perform(post(endpoint).with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":1,"attentionOverdueDays":3,"criticalOverdueDays":10,
                                 "attentionNoUpdateDays":null,"criticalNoUpdateDays":null,"reason":"Configuração"}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/projects/" + projectId + "/health").with(token(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATENCAO"))
                .andExpect(jsonPath("$.maxOverdueDays").value(5))
                .andExpect(jsonPath("$.reasons[0]").value(org.hamcrest.Matchers.containsString("limiar de atenção")));

        mvc.perform(post("/api/v1/projects/" + projectId + "/health-evaluations").with(token(ADMIN)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceActivityCount").value(1));
        mvc.perform(get("/api/v1/projects/" + projectId + "/health-history").with(token(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sourceActivityIds.length()").value(1));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void relevantUpdateAgeUsesAuditedActivityEventsInsteadOfGenericUpdatedAt() throws Exception {
        var projectId = project();
        mvc.perform(patch("/api/v1/projects/" + projectId + "/status").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ATIVO","expectedVersion":0,"reason":"Início autorizado"}
                                """))
                .andExpect(status().isOk());
        join(projectId, MEMBER);
        join(projectId, LEADER);
        transfer(projectId, LEADER);

        var activityId = java.util.UUID.randomUUID();
        jdbc.update("""
                insert into nite_connect.activities(id,title,description,project_id,status,due_on,created_by,created_at,updated_at)
                values (?,?,?,?,'PLANEJADA',null,?,clock_timestamp()-interval '20 days',clock_timestamp())
                """, activityId, "Atividade acompanhada", "Teste de atualização relevante", projectId, ADMIN);
        jdbc.update("""
                insert into nite_connect.activity_assignments(id,activity_id,member_id,role,started_by,start_reason)
                values (?,?,?,'EXECUTOR',?,'Fixture de teste')
                """, java.util.UUID.randomUUID(), activityId, MEMBER, ADMIN);
        jdbc.update("""
                insert into nite_connect.activity_events(id,activity_id,actor_id,action,reason,occurred_at,activity_version)
                values (?,?,?,'CREATED','Fixture de teste',clock_timestamp()-interval '20 days',0)
                """, java.util.UUID.randomUUID(), activityId, ADMIN);

        mvc.perform(get("/api/v1/projects/" + projectId + "/health").with(token(LEADER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ATENCAO"))
                .andExpect(jsonPath("$.daysSinceRelevantUpdate").value(20))
                .andExpect(jsonPath("$.latestUpdatedActivityId").value(activityId.toString()));
    }

    @Test
    @org.springframework.transaction.annotation.Transactional
    void recentlyCompletedActivityWithAuditedDeliveryIsARelevantProjectUpdate() throws Exception {
        var projectId = project();
        mvc.perform(patch("/api/v1/projects/" + projectId + "/status").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ATIVO","expectedVersion":0,"reason":"Início autorizado"}
                                """))
                .andExpect(status().isOk());
        join(projectId, MEMBER);

        var activityId = java.util.UUID.randomUUID();
        jdbc.update("""
                insert into nite_connect.activities(id,title,description,project_id,status,due_on,created_by,created_at,updated_at,completed_at)
                values (?,?,?,?,'CONCLUIDA',null,?,clock_timestamp()-interval '1 day',clock_timestamp(),clock_timestamp())
                """, activityId, "Entrega concluída", "Entrega recente", projectId, ADMIN);
        jdbc.update("""
                insert into nite_connect.activity_assignments(id,activity_id,member_id,role,started_by,start_reason)
                values (?,?,?,'EXECUTOR',?,'Fixture de teste')
                """, java.util.UUID.randomUUID(), activityId, MEMBER, ADMIN);
        jdbc.update("""
                insert into nite_connect.activity_events(id,activity_id,actor_id,action,reason,occurred_at,activity_version)
                values (?,?,?,'STATUS_CHANGED','Entrega concluída',clock_timestamp(),1)
                """, java.util.UUID.randomUUID(), activityId, ADMIN);

        mvc.perform(get("/api/v1/projects/" + projectId + "/health").with(token(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REGULAR"))
                .andExpect(jsonPath("$.daysSinceRelevantUpdate").value(0))
                .andExpect(jsonPath("$.latestUpdatedActivityId").value(activityId.toString()));
    }
}
