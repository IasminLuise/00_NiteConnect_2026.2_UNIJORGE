package br.edu.unijorge.niteconnect.academic.domain;

public final class AcademicStates {
    private AcademicStates() {}
    public enum Pillar { DOCUMENTATION, PRESENTATIONS, DELIVERIES }
    public enum Source { ACTIVITY, ATTENDANCE, ATTENDANCE_CONFIRMATION, DOCUMENT, PRESENTATION }
    public enum EnrollmentStatus { SOLICITADA, EM_ANALISE, CORRECAO_SOLICITADA, DEFERIDA, INDEFERIDA, EM_ACOMPANHAMENTO, CALCULADA, EM_REVISAO, HOMOLOGADA }
    public enum PlanStatus { RASCUNHO, SUBMETIDO, EM_ANALISE, CORRECAO_SOLICITADA, APROVADO }
    public enum EnrollmentDecision { ANALYZE, REQUEST_CORRECTION, APPROVE, REJECT }
    public enum PlanDecision { ANALYZE, REQUEST_CORRECTION, APPROVE }
}
