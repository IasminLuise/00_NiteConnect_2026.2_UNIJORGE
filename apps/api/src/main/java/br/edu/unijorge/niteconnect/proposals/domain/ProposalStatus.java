package br.edu.unijorge.niteconnect.proposals.domain;

public enum ProposalStatus {
    PENDENTE, APROVADA, REJEITADA, CONVERTIDA;

    public boolean canDecideTo(ProposalStatus next) {
        return this == PENDENTE && (next == APROVADA || next == REJEITADA);
    }
    public boolean canConvert() { return this == APROVADA; }
}
