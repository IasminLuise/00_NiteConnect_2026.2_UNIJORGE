package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.projecthealth.domain.ProjectHealthRules;
import br.edu.unijorge.niteconnect.projecthealth.domain.ProjectHealthRules.Policy;
import br.edu.unijorge.niteconnect.projecthealth.domain.ProjectHealthRules.Signals;
import br.edu.unijorge.niteconnect.projecthealth.domain.ProjectHealthRules.Status;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectHealthRulesTest {
    @Test
    void missingPolicyIsInsufficientInsteadOfUsingAnImplicitThreshold() {
        assertThat(ProjectHealthRules.classify(null, new Signals(0, 0, true)))
                .isEqualTo(Status.DADOS_INSUFICIENTES);
    }

    @Test
    void approvedBaselineThresholdsClassifyBoundaries() {
        Policy policy = new Policy(7, 14, 14, 30);

        assertThat(ProjectHealthRules.classify(policy, new Signals(6, 0, true))).isEqualTo(Status.REGULAR);
        assertThat(ProjectHealthRules.classify(policy, new Signals(7, 0, true))).isEqualTo(Status.ATENCAO);
        assertThat(ProjectHealthRules.classify(policy, new Signals(13, 0, true))).isEqualTo(Status.ATENCAO);
        assertThat(ProjectHealthRules.classify(policy, new Signals(14, 0, true))).isEqualTo(Status.CRITICO);
    }

    @Test
    void noUpdateThresholdCanBeConfiguredIndependently() {
        Policy policy = new Policy(7, 14, 14, 30);

        assertThat(ProjectHealthRules.classify(policy, new Signals(0, 13, true))).isEqualTo(Status.REGULAR);
        assertThat(ProjectHealthRules.classify(policy, new Signals(0, 14, true))).isEqualTo(Status.ATENCAO);
        assertThat(ProjectHealthRules.classify(policy, new Signals(0, 29, true))).isEqualTo(Status.ATENCAO);
        assertThat(ProjectHealthRules.classify(policy, new Signals(0, 30, true))).isEqualTo(Status.CRITICO);
    }

    @Test
    void missingSourceFactsAreInsufficientAndNegativeSignalsAreRejected() {
        assertThat(ProjectHealthRules.classify(new Policy(3, 10, null, null), new Signals(0, 0, false)))
                .isEqualTo(Status.DADOS_INSUFICIENTES);
        assertThat(ProjectHealthRules.classify(new Policy(7, 14, 14, 30), new Signals(0, null, true)))
                .isEqualTo(Status.DADOS_INSUFICIENTES);
        assertThatThrownBy(() -> new Signals(-1, 0, true)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void criticalThresholdMustExceedAttentionThreshold() {
        assertThatThrownBy(() -> new Policy(10, 3, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Policy(3, 10, 7, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
