package br.edu.unijorge.niteconnect.projects.domain;

public enum ProjectStatus {
    PLANEJAMENTO, ATIVO, PAUSADO, CONCLUIDO;

    public boolean canTransitionTo(ProjectStatus next) {
        if (next == null) return false;
        return switch (this) {
            case PLANEJAMENTO, CONCLUIDO -> next == ATIVO;
            case ATIVO -> next == PAUSADO || next == CONCLUIDO;
            case PAUSADO -> next == ATIVO || next == CONCLUIDO;
        };
    }
}
