package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Reuses the same isolated PostgreSQL and fictional identity fixture. */
class ProposalsBoundaryTest extends ProjectTestSupport {
    UUID proposal(UUID author) throws Exception {
        mvc.perform(post("/api/v1/proposals").with(token(author)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Ideia\",\"description\":\"Descrição original\",\"reason\":\"Proposição\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.authorId").value(author.toString()));
        return jdbc.queryForObject("select id from nite_connect.project_proposals order by created_at desc limit 1", UUID.class);
    }
    void approve(UUID id) throws Exception {
        mvc.perform(post("/api/v1/proposals/" + id + "/decisions").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APROVADA\",\"expectedVersion\":0,\"reason\":\"Autorizada\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.decidedBy").value(ADMIN.toString()));
    }
    @Test void authorCanOnlySeeOwnProposalsAndCannotDecideOrConvert() throws Exception {
        UUID id = proposal(MEMBER);
        mvc.perform(get("/api/v1/proposals/" + id).with(token(OTHER))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/proposals").with(token(OTHER))).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/proposals").with(token(ADMIN))).andExpect(jsonPath("$.total").value(1));
        mvc.perform(post("/api/v1/proposals/" + id + "/decisions").with(token(MEMBER)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APROVADA\",\"expectedVersion\":0,\"reason\":\"Teste\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/proposals/" + id + "/convert").with(token(MEMBER)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0,\"reason\":\"Teste\"}")).andExpect(status().isForbidden());
    }
    @Test void conversionRequiresApprovalAndCreatesOneProjectWithOriginAndAudit() throws Exception {
        UUID id = proposal(MEMBER);
        mvc.perform(post("/api/v1/proposals/" + id + "/convert").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":0,\"reason\":\"Teste\"}")).andExpect(status().isConflict());
        approve(id);
        mvc.perform(patch("/api/v1/proposals/" + id).with(token(MEMBER)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Alteração tardia\",\"description\":\"Teste\",\"expectedVersion\":1,\"reason\":\"Teste\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/proposals/" + id + "/convert").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"reason\":\"Formalização\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.proposalId").value(id.toString()))
                .andExpect(jsonPath("$.status").value("PLANEJAMENTO")).andExpect(jsonPath("$.description").value("Descrição original"));
        mvc.perform(post("/api/v1/proposals/" + id + "/convert").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"reason\":\"Repetição\"}")).andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.projects where proposal_id=?", Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from nite_connect.project_proposals where id=?", String.class, id)).isEqualTo("CONVERTIDA");
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.audit_events where entity_id=?", Integer.class, id)).isEqualTo(3);
    }
    @Test void simultaneousConversionsHaveOneWinner() throws Exception {
        UUID id = proposal(MEMBER); approve(id);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var start = new java.util.concurrent.CountDownLatch(1);
            var first = executor.submit(() -> convertAfter(start, id));
            var second = executor.submit(() -> convertAfter(start, id)); start.countDown();
            assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.projects where proposal_id=?", Integer.class, id)).isEqualTo(1);
    }
    int convertAfter(java.util.concurrent.CountDownLatch start, UUID id) throws Exception {
        start.await();
        return mvc.perform(post("/api/v1/proposals/" + id + "/convert").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"reason\":\"Formalização\"}")).andReturn().getResponse().getStatus();
    }
    @Test void rejectedProposalCannotBeConvertedOrRedecided() throws Exception {
        UUID id = proposal(MEMBER);
        mvc.perform(post("/api/v1/proposals/" + id + "/decisions").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CONVERTIDA\",\"expectedVersion\":0,\"reason\":\"Pular aprovação\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/proposals/" + id + "/decisions").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REJEITADA\",\"expectedVersion\":0,\"reason\":\"Não autorizada\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/proposals/" + id + "/decisions").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APROVADA\",\"expectedVersion\":1,\"reason\":\"Outra decisão\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/proposals/" + id + "/convert").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"expectedVersion\":1,\"reason\":\"Teste\"}")).andExpect(status().isConflict());
    }
    @Test void failureOfFinalConversionAuditRollsBackProjectProposalAndAllIntermediateEvents() throws Exception {
        UUID id = proposal(MEMBER); approve(id);
        jdbc.execute("""
                create function nite_connect.fail_conversion_test() returns trigger language plpgsql as $$
                begin if NEW.action='PROPOSAL_CONVERTED' then raise exception 'test fault'; end if; return NEW; end; $$
                """);
        jdbc.execute("create trigger fail_conversion_test before insert on nite_connect.audit_events for each row execute function nite_connect.fail_conversion_test()");
        try {
            assertThatThrownBy(() -> mvc.perform(post("/api/v1/proposals/" + id + "/convert").with(token(ADMIN)).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"expectedVersion\":1,\"reason\":\"Formalização\"}")))
                    .hasRootCauseInstanceOf(org.postgresql.util.PSQLException.class);
            assertThat(jdbc.queryForObject("select count(*) from nite_connect.projects where proposal_id=?", Integer.class, id)).isZero();
            assertThat(jdbc.queryForObject("select status from nite_connect.project_proposals where id=?", String.class, id)).isEqualTo("APROVADA");
            assertThat(jdbc.queryForObject("select version from nite_connect.project_proposals where id=?", Long.class, id)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from nite_connect.project_status_history", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from nite_connect.audit_events where entity_type='PROJECT'", Integer.class)).isZero();
        } finally {
            jdbc.execute("drop trigger fail_conversion_test on nite_connect.audit_events");
            jdbc.execute("drop function nite_connect.fail_conversion_test()");
        }
    }
    @Test void pendingProposalCanBeEditedOnlyByAuthorOrAdministrationWithCurrentVersion() throws Exception {
        UUID id = proposal(MEMBER);
        String body = "{\"title\":\"Revisada\",\"description\":\"Descrição revisada\",\"expectedVersion\":0,\"reason\":\"Correção\"}";
        mvc.perform(patch("/api/v1/proposals/" + id).with(token(OTHER)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/proposals/" + id).with(token(MEMBER)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Revisada")).andExpect(jsonPath("$.version").value(1));
        mvc.perform(patch("/api/v1/proposals/" + id).with(token(MEMBER)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/proposals?status=PENDENTE").with(token(MEMBER))).andExpect(jsonPath("$.total").value(1));
    }
}
