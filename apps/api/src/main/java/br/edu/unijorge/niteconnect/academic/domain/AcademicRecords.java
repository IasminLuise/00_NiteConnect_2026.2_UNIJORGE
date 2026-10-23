package br.edu.unijorge.niteconnect.academic.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import br.edu.unijorge.niteconnect.academic.domain.AcademicStates.*;

public final class AcademicRecords {
    private AcademicRecords() {}
    public record TermData(String title, Instant startsAt, Instant endsAt, String zone) {}
    public record Term(UUID id, TermData data, long version) {}
    public record Discipline(String courseCode, String code) {}
    public record PolicyRules(Map<Pillar,BigDecimal> weights, BigDecimal minimumScore, BigDecimal maximumScore,
                              Integer scale, RoundingMode rounding, List<Discipline> eligibleDisciplines, Integer maximumDisciplines,
                              Instant enrollmentOpensAt, Instant enrollmentClosesAt, Long reviewWindowSeconds,
                              Boolean discloseResults, Boolean planRequired, String forwardingProcedureReference) {}
    public record Policy(UUID id, UUID termId, int revision, String status, PolicyRules rules, String authorityReference, long version) {}
    public record Enrollment(UUID id, UUID termId, UUID memberId, UUID policyId, EnrollmentStatus status,
                             List<Discipline> disciplines, long version, Instant createdAt) {}
    public record Plan(UUID id, UUID enrollmentId, int revision, PlanStatus status, String content, long version) {}
    /** Contract version is independent of the provider's revision. Attributes contain only relevant, authorized data. */
    public record SourceSnapshot(int contractVersion, Source source, UUID sourceId, UUID memberId, UUID projectId,
                                 String sourceRevision, UUID authorId, UUID validatedBy, String validationStatus,
                                 Instant occurredAt, Instant validatedAt, Instant from, Instant to,
                                 Map<String,String> attributes) {}
    public record Evidence(UUID id, UUID enrollmentId, SourceSnapshot snapshot, UUID capturedBy, Instant capturedAt) {}
    public record Assessment(UUID id, UUID enrollmentId, Pillar pillar, BigDecimal score, List<UUID> evidenceIds,
                             String reason, int revision, UUID assessedBy, Instant createdAt) {}
    public record CalculationData(Policy policy, List<Assessment> assessments, List<Evidence> evidence,
                                  BigDecimal result, String algorithm, Instant cutAt) {}
    public record Calculation(UUID id, UUID enrollmentId, UUID policyId, long enrollmentVersion, CalculationData data, Instant createdAt) {}
    public record Review(UUID id, UUID enrollmentId, UUID calculationId, UUID homologationId, UUID requestedBy,
                         String reason, String status, UUID decidedBy, String decisionReason, long version, Instant createdAt) {}
    public record Homologation(UUID id, UUID enrollmentId, UUID calculationId, UUID supersedesId, UUID decidedBy, String reason, Instant createdAt) {}
    public record Forwarding(UUID id, UUID homologationId, UUID forwardedBy, String procedureReference, String receiptReference, Instant createdAt) {}
    public record AcademicEvent(UUID id, UUID enrollmentId, String entityType, UUID entityId, UUID actorId,
                                String action, String beforeValue, String afterValue, String reason, Instant occurredAt) {}
}
