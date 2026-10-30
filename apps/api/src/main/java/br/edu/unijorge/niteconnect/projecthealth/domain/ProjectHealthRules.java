package br.edu.unijorge.niteconnect.projecthealth.domain;

/** Rules consume only an explicit versioned policy, including the approved project baseline. */
public final class ProjectHealthRules {
    private ProjectHealthRules() {}

    public enum Status { REGULAR, ATENCAO, CRITICO, DADOS_INSUFICIENTES }

    public record Policy(Integer attentionOverdueDays, Integer criticalOverdueDays,
                         Integer attentionNoUpdateDays, Integer criticalNoUpdateDays) {
        public Policy {
            validatePair(attentionOverdueDays, criticalOverdueDays, "atraso");
            validatePair(attentionNoUpdateDays, criticalNoUpdateDays, "sem atualização");
        }

        private static void validatePair(Integer attention, Integer critical, String label) {
            if ((attention == null) != (critical == null)) {
                throw new IllegalArgumentException("Limiares de atenção e criticidade devem ser definidos juntos para " + label);
            }
            if (attention != null && (attention < 1 || critical <= attention)) {
                throw new IllegalArgumentException("Limiares devem ser positivos e o crítico deve superar o de atenção para " + label);
            }
        }

        public boolean configured() { return attentionOverdueDays != null; }
    }

    public record Signals(int maxOverdueDays, Integer daysSinceRelevantUpdate, boolean available) {
        public Signals {
            if (maxOverdueDays < 0 || daysSinceRelevantUpdate != null && daysSinceRelevantUpdate < 0) {
                throw new IllegalArgumentException("Sinais de projeto não podem ser negativos");
            }
        }
    }

    public static Status classify(Policy policy, Signals signals) {
        if (policy == null || !policy.configured() || signals == null || !signals.available()) {
            return Status.DADOS_INSUFICIENTES;
        }

        boolean criticalDelay = signals.maxOverdueDays() >= policy.criticalOverdueDays();
        boolean criticalSilence = signals.daysSinceRelevantUpdate() != null && policy.criticalNoUpdateDays() != null
                && signals.daysSinceRelevantUpdate() >= policy.criticalNoUpdateDays();
        if (criticalDelay || criticalSilence) return Status.CRITICO;

        boolean attentionDelay = signals.maxOverdueDays() >= policy.attentionOverdueDays();
        boolean attentionSilence = signals.daysSinceRelevantUpdate() != null && policy.attentionNoUpdateDays() != null
                && signals.daysSinceRelevantUpdate() >= policy.attentionNoUpdateDays();
        if (attentionDelay || attentionSilence) return Status.ATENCAO;
        if (signals.daysSinceRelevantUpdate() == null && policy.attentionNoUpdateDays() != null) {
            return Status.DADOS_INSUFICIENTES;
        }
        return Status.REGULAR;
    }
}
