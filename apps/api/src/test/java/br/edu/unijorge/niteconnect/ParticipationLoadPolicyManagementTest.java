package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ParticipationLoadPolicyManagementTest extends AttendanceTestSupport {
    @Test
    void policyLifecycleRequiresGlobalAdministrationAndKeepsTheExplicitEffectiveDate() throws Exception {
        mvc.perform(get("/api/v1/attendance/participation-load-policies").with(token(MEMBER)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/attendance/participation-load-policies").with(token(LEADER))
                        .contentType(MediaType.APPLICATION_JSON).content(draftBody("2026-10-26", 5, 4, "Criar política")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/attendance/participation-load-policies").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"presentialHours\":5,\"remoteHours\":4,\"reason\":\"Sem data explícita\"}"))
                .andExpect(status().isBadRequest());

        String created = mvc.perform(post("/api/v1/attendance/participation-load-policies").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(draftBody("2026-10-26", 5, 4, "Parâmetros confirmados")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.status").value("CRIADA"))
                .andExpect(jsonPath("$.effectiveFrom").value("2026-10-26"))
                .andExpect(jsonPath("$.createdBy").value(ADMIN.toString()))
                .andExpect(jsonPath("$.events[0].type").value("CRIADA"))
                .andReturn().getResponse().getContentAsString();
        UUID draftId = UUID.fromString(json.readTree(created).get("id").asString());

        mvc.perform(post("/api/v1/attendance/participation-load-policies/" + draftId + "/activations").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"reason\":\"Ativar sem aprovação\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/attendance/participation-load-policies/" + draftId + "/approvals").with(token(LEADER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"sourceReference\":\"Decisão institucional\",\"reason\":\"Aprovação\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/v1/attendance/participation-load-policies/" + draftId + "/approvals").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"sourceReference\":\"Confirmação institucional registrada\",\"reason\":\"Aprovada para ativação\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"))
                .andExpect(jsonPath("$.events[1].actorId").value(ADMIN.toString()))
                .andExpect(jsonPath("$.events[1].sourceReference").value("Confirmação institucional registrada"));
        mvc.perform(post("/api/v1/attendance/participation-load-policies/" + draftId + "/activations").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"reason\":\"Ativação com vigência explícita no rascunho\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ATIVADA"))
                .andExpect(jsonPath("$.events[2].type").value("ATIVADA"));

        mvc.perform(get("/api/v1/me/participation-weekly-summary").with(token(MEMBER))
                        .param("from", "2026-10-26").param("to", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadPolicy.status").value("HOMOLOGADA_NO_PERIODO"))
                .andExpect(jsonPath("$.loadPolicy.applicableToWeek.version").value(1))
                .andExpect(jsonPath("$.modalities.presential.requiredSeconds").value(18_000))
                .andExpect(jsonPath("$.modalities.remote.requiredSeconds").value(14_400));
    }

    @Test
    void policyRevisionsAreAppendOnlyAndHistoricalWeeksKeepTheirApplicableVersion() throws Exception {
        UUID initial = activate(draft("2026-10-26", 5, 4, "Política oficial"));
        String next = mvc.perform(post("/api/v1/attendance/participation-load-policies").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(draftBody("2026-11-02", 5, 4, "Nova vigência proposta")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(2))
                .andReturn().getResponse().getContentAsString();
        UUID nextId = UUID.fromString(json.readTree(next).get("id").asString());

        String revised = mvc.perform(post("/api/v1/attendance/participation-load-policies/" + nextId + "/revisions").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":2,\"presentialHours\":6,\"remoteHours\":3,\"effectiveFrom\":\"2026-11-02\",\"reason\":\"Revisão versionada\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.status").value("CRIADA"))
                .andReturn().getResponse().getContentAsString();
        UUID revisedId = UUID.fromString(json.readTree(revised).get("id").asString());
        mvc.perform(get("/api/v1/attendance/participation-load-policies").with(token(ADMIN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[1].id").value(nextId.toString()))
                .andExpect(jsonPath("$.items[1].status").value("SUBSTITUIDA"))
                .andExpect(jsonPath("$.items[1].events[*].reason").value(org.hamcrest.Matchers.hasItem("Substituída pela versão 3: Revisão versionada")));
        approveAndActivate(revisedId, 3, "Nova decisão formal", "Atualização institucional registrada");

        assertThat(jdbc.queryForObject("select effective_until from nite_connect.participation_load_policy_versions where id=?",
                LocalDate.class, initial)).isEqualTo(LocalDate.parse("2026-11-02"));
        mvc.perform(get("/api/v1/me/participation-weekly-summary").with(token(MEMBER))
                        .param("from", "2026-10-26").param("to", "2026-11-01"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.loadPolicy.applicableToWeek.version").value(1))
                .andExpect(jsonPath("$.modalities.presential.requiredSeconds").value(18_000))
                .andExpect(jsonPath("$.modalities.remote.requiredSeconds").value(14_400));
        mvc.perform(get("/api/v1/me/participation-weekly-summary").with(token(MEMBER))
                        .param("from", "2026-11-02").param("to", "2026-11-08"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.loadPolicy.applicableToWeek.version").value(3))
                .andExpect(jsonPath("$.modalities.presential.requiredSeconds").value(21_600))
                .andExpect(jsonPath("$.modalities.remote.requiredSeconds").value(10_800));
    }

    @Test
    void firstPolicyUsesConfirmedHoursAndARevisionCannotMoveBackBeforeTheLatestActivatedVersion() throws Exception {
        mvc.perform(post("/api/v1/attendance/participation-load-policies").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(draftBody("2026-10-26", 4, 4, "Valor incompatível")))
                .andExpect(status().isBadRequest());
        UUID initial = activate(draft("2026-10-26", 5, 4, "Política oficial"));
        String next = mvc.perform(post("/api/v1/attendance/participation-load-policies").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(draftBody("2026-10-20", 6, 3, "Vigência anterior explícita")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID nextId = UUID.fromString(json.readTree(next).get("id").asString());
        mvc.perform(post("/api/v1/attendance/participation-load-policies/" + nextId + "/approvals").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":2,\"sourceReference\":\"Atualização registrada\",\"reason\":\"Aprovada\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/attendance/participation-load-policies/" + nextId + "/activations").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":2,\"reason\":\"Repetição de ativação\"}"))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select effective_until from nite_connect.participation_load_policy_versions where id=?",
                LocalDate.class, initial)).isNull();
    }

    @Test
    void official20271PolicyCanBeApprovedWithoutInventingItsEffectiveDate() throws Exception {
        String plan = mvc.perform(post("/api/v1/attendance/participation-load-policies").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"presentialHours\":5,\"remoteHours\":4,\"intendedSemester\":\"2027.1\","
                                + "\"effectiveWindow\":\"PRIMEIRA_SEMANA_OFICIAL_DE_ATIVIDADES\","
                                + "\"reason\":\"Carga confirmada para 2027.1; data letiva pendente\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.effectiveFrom").doesNotExist())
                .andExpect(jsonPath("$.intendedSemester").value("2027.1"))
                .andReturn().getResponse().getContentAsString();
        UUID planId = UUID.fromString(json.readTree(plan).get("id").asString());
        mvc.perform(post("/api/v1/attendance/participation-load-policies/" + planId + "/approvals").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"sourceReference\":\"Confirmação institucional\",\"reason\":\"Valores aprovados; data pendente\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"));

        mvc.perform(get("/api/v1/me/participation-weekly-summary").with(token(MEMBER))
                        .param("from", "2026-10-26").param("to", "2026-11-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadPolicy.status").value("SEM_POLITICA_APLICAVEL"))
                .andExpect(jsonPath("$.loadPolicy.plannedConfiguration.status").value("SEM_VIGENCIA_DEFINIDA"))
                .andExpect(jsonPath("$.loadPolicy.plannedConfiguration.effectiveFrom").doesNotExist())
                .andExpect(jsonPath("$.modalities.presential.requiredSeconds").doesNotExist())
                .andExpect(jsonPath("$.modalities.presential.remainingSeconds").doesNotExist())
                .andExpect(jsonPath("$.modalities.remote.requiredSeconds").doesNotExist())
                .andExpect(jsonPath("$.modalities.remote.remainingSeconds").doesNotExist());
        mvc.perform(post("/api/v1/attendance/participation-load-policies/" + planId + "/activations").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"reason\":\"Data oficial ainda não publicada\"}"))
                .andExpect(status().isConflict());

        // Data sintética apenas para validar o limite temporal; não representa calendário institucional de 2027.1.
        String dated = mvc.perform(post("/api/v1/attendance/participation-load-policies/" + planId + "/revisions").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"presentialHours\":5,\"remoteHours\":4,"
                                + "\"effectiveFrom\":\"2027-02-15\",\"intendedSemester\":\"2027.1\","
                                + "\"effectiveWindow\":\"PRIMEIRA_SEMANA_OFICIAL_DE_ATIVIDADES\",\"reason\":\"Data sintética de regressão\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(2))
                .andReturn().getResponse().getContentAsString();
        UUID datedId = UUID.fromString(json.readTree(dated).get("id").asString());
        approveAndActivate(datedId, 2, "Vigência sintética de teste", "Calendário sintético de regressão; sem valor institucional");

        mvc.perform(get("/api/v1/me/participation-weekly-summary").with(token(MEMBER))
                        .param("from", "2027-02-08").param("to", "2027-02-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadPolicy.status").value("SEM_POLITICA_APLICAVEL"))
                .andExpect(jsonPath("$.modalities.presential.requiredSeconds").doesNotExist())
                .andExpect(jsonPath("$.modalities.remote.requiredSeconds").doesNotExist());
        mvc.perform(get("/api/v1/me/participation-weekly-summary").with(token(MEMBER))
                        .param("from", "2027-02-15").param("to", "2027-02-21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loadPolicy.applicableToWeek.version").value(2))
                .andExpect(jsonPath("$.modalities.presential.requiredSeconds").value(18_000))
                .andExpect(jsonPath("$.modalities.remote.requiredSeconds").value(14_400));
    }

    private String draftBody(String effectiveFrom, int presential, int remote, String reason) {
        return "{\"presentialHours\":" + presential + ",\"remoteHours\":" + remote
                + ",\"effectiveFrom\":\"" + effectiveFrom + "\",\"reason\":\"" + reason + "\"}";
    }

    private UUID draft(String effectiveFrom, int presential, int remote, String reason) throws Exception {
        String body = mvc.perform(post("/api/v1/attendance/participation-load-policies").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content(draftBody(effectiveFrom, presential, remote, reason)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(json.readTree(body).get("id").asString());
    }

    private UUID activate(UUID id) throws Exception {
        approveAndActivate(id, 1, "Aprovação inicial", "Confirmação institucional registrada");
        return id;
    }

    private void approveAndActivate(UUID id, int version, String reason, String sourceReference) throws Exception {
        mvc.perform(post("/api/v1/attendance/participation-load-policies/" + id + "/approvals").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":" + version + ",\"sourceReference\":\"" + sourceReference
                                + "\",\"reason\":\"" + reason + "\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/attendance/participation-load-policies/" + id + "/activations").with(token(ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":" + version + ",\"reason\":\"Ativada\"}"))
                .andExpect(status().isOk());
    }
}
