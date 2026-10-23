package br.edu.unijorge.niteconnect.academic.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicStates.*;

public final class AcademicContracts {
    private AcademicContracts() {}
    public record CreateTerm(@NotBlank @Size(max=100) String title,@NotNull Instant startsAt,@NotNull Instant endsAt,
                             @NotBlank @Size(max=80) String zone,@NotBlank @Size(max=1000) String reason) {}
    public record CreatePolicy(@NotNull PolicyRules rules,@PositiveOrZero long expectedVersion,@NotBlank @Size(max=1000) String reason) {}
    public record ConfirmPolicy(@PositiveOrZero long expectedVersion,boolean confirmed,@NotBlank @Size(max=1000) String authorityReference,
                                @NotBlank @Size(max=1000) String reason) {}
    public record RequestEnrollment(@NotNull UUID policyId,@NotEmpty @Size(max=100) List<Discipline> disciplines,@NotBlank @Size(max=1000) String reason) {}
    public record ReviseEnrollment(@NotEmpty @Size(max=100) List<Discipline> disciplines,@PositiveOrZero long expectedVersion,@NotBlank @Size(max=1000) String reason) {}
    public record DecideEnrollment(@NotNull EnrollmentDecision decision,@PositiveOrZero long expectedVersion,@NotBlank @Size(max=1000) String reason) {}
    public record SavePlan(@NotBlank @Size(max=20000) String content,@PositiveOrZero long expectedVersion,@NotBlank @Size(max=1000) String reason) {}
    public record Command(@PositiveOrZero long expectedVersion,boolean confirmed,@NotBlank @Size(max=1000) String reason) {}
    public record DecidePlan(@NotNull PlanDecision decision,@PositiveOrZero long expectedVersion,@NotBlank @Size(max=1000) String reason) {}
    public record CaptureEvidence(@NotNull Source source,@NotNull UUID sourceId,@PositiveOrZero long expectedVersion,@NotBlank @Size(max=1000) String reason) {}
    public record Presentation(@NotBlank @Size(max=1000) String title,@NotNull Instant occurredAt,@NotBlank @Size(max=1000) String reference,
                               @PositiveOrZero long expectedVersion,@NotBlank @Size(max=1000) String reason) {}
    public record AssessPillar(@NotNull Pillar pillar,@NotNull BigDecimal score,@NotEmpty @Size(max=100) List<UUID> evidenceIds,
                              @PositiveOrZero long expectedVersion,@NotBlank @Size(max=1000) String reason) {}
    public record RequestReview(@NotNull UUID calculationId,@NotBlank @Size(max=1000) String reason) {}
    public record DecideReview(boolean accepted,@PositiveOrZero long expectedVersion,boolean confirmed,@NotBlank @Size(max=1000) String reason) {}
    public record ForwardAssessment(@PositiveOrZero long expectedVersion,boolean confirmed,@NotBlank @Size(max=1000) String receiptReference,
                                     @NotBlank @Size(max=1000) String reason) {}
}
