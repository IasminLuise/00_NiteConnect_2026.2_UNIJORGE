package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.time.LocalDate;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AnalyticsTest extends ProjectTestSupport {
    @Test
    void projectReportsIncludeOlderProjectsWhileActivityCreationKeepsItsDateWindow() throws Exception {
        var projectId=project();
        jdbc.update("update nite_connect.projects set created_at=clock_timestamp()-interval '2 years' where id=?", projectId);

        mvc.perform(get("/api/v1/analytics/reports/GERENCIAL_CONSOLIDADO")
                        .queryParam("from",LocalDate.now().minusDays(2).toString())
                        .queryParam("to",LocalDate.now().plusDays(2).toString()).with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0][0]").value(projectId.toString()));

        mvc.perform(get("/api/v1/analytics/reports/ATIVIDADES_ENTREGAS")
                        .queryParam("from",LocalDate.now().minusDays(2).toString())
                        .queryParam("to",LocalDate.now().plusDays(2).toString()).with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void administrativeDashboardParticipationIndicatorsIncludeRecordsBeyondReportRowLimit() throws Exception {
        jdbc.update("""
                insert into nite_connect.participation_periods
                    (id,member_id,kind,modality,starts_at,ends_at,description,origin,review_status)
                select gen_random_uuid(), (array[?,?,?,?]::uuid[])[((n-1)%4)+1], 'WORK_LOG','REMOTA',
                       clock_timestamp()-interval '30 days'+n*interval '15 minutes',
                       clock_timestamp()-interval '30 days'+n*interval '15 minutes'+interval '5 minutes',
                       'Registro de teste','DECLARATION','PENDENTE'
                from generate_series(1,2001) n
                """, ADMIN, LEADER, MEMBER, OTHER);
        for(UUID memberId:java.util.List.of(LEADER,OTHER)) {
            jdbc.update("""
                    insert into nite_connect.participation_periods
                        (id,member_id,kind,modality,starts_at,ends_at,description,origin,review_status)
                    select gen_random_uuid(),?,'WORK_LOG','REMOTA',span.starts_at,span.starts_at+interval '0.4 seconds',
                           'Registro fracionário de teste','DECLARATION','PENDENTE'
                    from (select clock_timestamp()-interval '30 days' starts_at) span
                    """, memberId);
        }

        mvc.perform(get("/api/v1/analytics/dashboard").queryParam("from",LocalDate.now().minusDays(31).toString())
                        .queryParam("to",LocalDate.now().toString()).with(token(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['indicators']['participation.pendente']").value(2003))
                .andExpect(jsonPath("$['indicators']['participation.remota.seconds']").value(600300));
    }

    @Test
    void administrativeDashboardProjectCountIsNotTheLimitedProjectRowsCount() throws Exception {
        jdbc.update("""
                insert into nite_connect.projects(id,title,description,status,created_by)
                select gen_random_uuid(),'Projeto de teste '||n,'Projeto sintético para limite de consulta','PLANEJAMENTO',?
                from generate_series(1,201) n
                """, ADMIN);

        mvc.perform(get("/api/v1/analytics/dashboard").queryParam("from",LocalDate.now().minusDays(2).toString())
                        .queryParam("to",LocalDate.now().plusDays(2).toString()).with(token(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicators.projects").value(201))
                .andExpect(jsonPath("$.rows.length()").value(200))
                .andExpect(jsonPath("$.unavailableMetrics",org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("resultado está truncado"))));
    }

    @Test
    void projectLeaderGetsScopedReportsAndExportsButOtherProjectMembersDoNot() throws Exception {
        var projectId=project(); join(projectId,LEADER); join(projectId,MEMBER); transfer(projectId,LEADER);
        mvc.perform(post("/api/v1/activities").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Entrega demonstrativa","description":"Atividade operacional",
                                 "projectId":"%s","dueOn":"%s","primaryExecutorId":"%s","reason":"Planejamento"}
                                """.formatted(projectId,LocalDate.now().plusDays(3),MEMBER)))
                .andExpect(status().isCreated());
        String endpoint="/api/v1/projects/"+projectId+"/reports/ATIVIDADES_ENTREGAS?from="+LocalDate.now().minusDays(2)+"&to="+LocalDate.now().plusDays(2);
        mvc.perform(get(endpoint).with(token(LEADER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0][0]").isNotEmpty()).andExpect(jsonPath("$.rows[0][2]").value("Entrega demonstrativa"));
        mvc.perform(get(endpoint).with(token(MEMBER))).andExpect(status().isForbidden());
        mvc.perform(get(endpoint).with(token(OTHER))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/projects/"+projectId+"/reports/ATIVIDADES_ENTREGAS")
                        .queryParam("from",LocalDate.now().minusDays(2).toString()).queryParam("to",LocalDate.now().plusDays(2).toString())
                        .queryParam("situation","CONCLUIDA").with(token(LEADER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/projects/"+projectId+"/reports/ATIVIDADES_ENTREGAS")
                        .queryParam("from",LocalDate.now().minusDays(2).toString()).queryParam("to",LocalDate.now().plusDays(2).toString())
                        .queryParam("situation","PENDENTE").with(token(LEADER)))
                .andExpect(status().isBadRequest());

        mvc.perform(get("/api/v1/projects/"+projectId+"/reports/ATIVIDADES_ENTREGAS/export")
                        .queryParam("from",LocalDate.now().minusDays(2).toString()).queryParam("to",LocalDate.now().plusDays(2).toString()).queryParam("format","CSV").with(token(LEADER)))
                .andExpect(status().isOk()).andExpect(header().string("Content-Type","text/csv;charset=UTF-8"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Entrega demonstrativa")));
        mvc.perform(get("/api/v1/projects/"+projectId+"/reports/ATIVIDADES_ENTREGAS/export")
                        .queryParam("from",LocalDate.now().minusDays(2).toString()).queryParam("to",LocalDate.now().plusDays(2).toString()).queryParam("format","PDF").with(token(LEADER)))
                .andExpect(status().isOk()).andExpect(header().string("Content-Type","application/pdf"))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(new String(result.getResponse().getContentAsByteArray(),0,4,java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF"));
    }

    @Test
    void personalDashboardIsScopedAndAdministrativeHistoryUsesPreservedEvents() throws Exception {
        var projectId=project(); join(projectId,MEMBER); join(projectId,LEADER); transfer(projectId,LEADER);
        mvc.perform(get("/api/v1/me/dashboard").queryParam("from",LocalDate.now().minusDays(2).toString())
                        .queryParam("to",LocalDate.now().plusDays(2).toString()).with(token(MEMBER)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.scope").value("integrante"))
                .andExpect(jsonPath("$.indicators.recordsInPeriod").value(0))
                .andExpect(jsonPath("$.unavailableMetrics[0]").value(org.hamcrest.Matchers.containsString("B05")));
        mvc.perform(get("/api/v1/analytics/history").queryParam("from",LocalDate.now().minusDays(2).toString())
                        .queryParam("to",LocalDate.now().plusDays(2).toString()).queryParam("projectId",projectId.toString()).with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.rows[0][0]").value("PROJECT"));
        mvc.perform(get("/api/v1/analytics/dashboard").queryParam("from",LocalDate.now().minusDays(2).toString())
                        .queryParam("to",LocalDate.now().plusDays(2).toString()).with(token(MEMBER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void inactiveMemberCannotReadPersonalDashboard() throws Exception {
        jdbc.update("update nite_connect.members set status='AFASTADO' where id=?", MEMBER);
        mvc.perform(get("/api/v1/me/dashboard").queryParam("from",LocalDate.now().minusDays(2).toString())
                        .queryParam("to",LocalDate.now().plusDays(2).toString()).with(token(MEMBER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void allFourAdministrativeReportModelsShareFiltersAndAuthorization() throws Exception {
        project();
        for(String model:java.util.List.of("FREQUENCIA_PARTICIPACAO","ATIVIDADES_ENTREGAS","ACOMPANHAMENTO_PROJETOS","GERENCIAL_CONSOLIDADO")) {
            mvc.perform(get("/api/v1/analytics/reports/"+model)
                            .queryParam("from",LocalDate.now().minusDays(2).toString())
                            .queryParam("to",LocalDate.now().plusDays(2).toString())
                            .queryParam("page","0").queryParam("size","20").with(token(ADMIN)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.type").value(model))
                    .andExpect(jsonPath("$.generatedAt").isNotEmpty()).andExpect(jsonPath("$.criteria").isNotEmpty());
            mvc.perform(get("/api/v1/analytics/reports/"+model+"/export")
                            .queryParam("from",LocalDate.now().minusDays(2).toString())
                            .queryParam("to",LocalDate.now().plusDays(2).toString())
                            .queryParam("format","CSV").with(token(MEMBER)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void comparisonRequiresEquivalentDisjointWindowsAndUsesPreservedSourceCounts() throws Exception {
        project();
        mvc.perform(get("/api/v1/analytics/comparison")
                        .queryParam("baselineFrom","2026-10-20").queryParam("baselineTo","2026-10-26")
                        .queryParam("comparisonFrom","2026-10-27").queryParam("comparisonTo","2026-11-02").with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.metrics.length()").value(4))
                .andExpect(jsonPath("$.unavailableMetrics[0]").value(org.hamcrest.Matchers.containsString("não equivalem")));
        mvc.perform(get("/api/v1/analytics/comparison")
                        .queryParam("baselineFrom","2026-10-20").queryParam("baselineTo","2026-10-26")
                        .queryParam("comparisonFrom","2026-10-26").queryParam("comparisonTo","2026-11-01").with(token(ADMIN)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/analytics/comparison")
                        .queryParam("baselineFrom","2026-10-20").queryParam("baselineTo","2026-10-25")
                        .queryParam("comparisonFrom","2026-10-27").queryParam("comparisonTo","2026-11-02").with(token(ADMIN)))
                .andExpect(status().isBadRequest());
    }
}
