package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.projects.domain.ProjectStatus;
import br.edu.unijorge.niteconnect.proposals.domain.ProposalStatus;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class Block02RulesTest {
    @ParameterizedTest
    @CsvSource({
            "PLANEJAMENTO,ATIVO,true", "PLANEJAMENTO,PAUSADO,false", "PLANEJAMENTO,CONCLUIDO,false", "PLANEJAMENTO,PLANEJAMENTO,false",
            "ATIVO,PAUSADO,true", "ATIVO,CONCLUIDO,true", "ATIVO,PLANEJAMENTO,false", "ATIVO,ATIVO,false",
            "PAUSADO,ATIVO,true", "PAUSADO,CONCLUIDO,true", "PAUSADO,PLANEJAMENTO,false", "PAUSADO,PAUSADO,false",
            "CONCLUIDO,ATIVO,true", "CONCLUIDO,PAUSADO,false", "CONCLUIDO,PLANEJAMENTO,false", "CONCLUIDO,CONCLUIDO,false"
    })
    void onlyApprovedProjectTransitionsAreAccepted(ProjectStatus before, ProjectStatus next, boolean allowed) {
        assertThat(before.canTransitionTo(next)).isEqualTo(allowed);
    }
    @ParameterizedTest
    @CsvSource({"PENDENTE,APROVADA,true", "PENDENTE,REJEITADA,true", "PENDENTE,CONVERTIDA,false",
            "PENDENTE,PENDENTE,false", "APROVADA,REJEITADA,false", "REJEITADA,APROVADA,false", "CONVERTIDA,APROVADA,false"})
    void decisionsCannotSkipApprovalOrRewriteFinalDecisions(ProposalStatus before, ProposalStatus next, boolean allowed) {
        assertThat(before.canDecideTo(next)).isEqualTo(allowed);
    }
    @ParameterizedTest
    @CsvSource({"PENDENTE,false", "APROVADA,true", "REJEITADA,false", "CONVERTIDA,false"})
    void conversionOnlyAcceptsAnApprovedProposal(ProposalStatus status, boolean allowed) {
        assertThat(status.canConvert()).isEqualTo(allowed);
    }
}
