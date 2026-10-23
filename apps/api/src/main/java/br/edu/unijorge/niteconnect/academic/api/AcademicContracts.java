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
    public record CreatePolicy(@NotNull PolicyRules rules,@NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=1000) String reason) {}
    public record ConfirmPolicy(@NotNull @PositiveOrZero Long expectedVersion,boolean confirmed,@NotBlank @Size(max=1000) String authorityReference,
                                @NotBlank @Size(max=1000) String reason) {}
    public record RequestEnrollment(@NotNull UUID policyId,@NotEmpty @Size(max=100) List<Discipline> disciplines,@NotBlank @Size(max=1000) String reason) {}
    public record ReviseEnrollment(@NotEmpty @Size(max=100) List<Discipline> disciplines,@NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=1000) String reason) {}
    public record DecideEnrollment(@NotNull EnrollmentDecision decision,@NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=1000) String reason,@Size(max=1000) String feedback) {}
    public record SavePlan(@NotBlank @Size(max=20000) String content,@NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=1000) String reason) {}
    public record Command(@NotNull @PositiveOrZero Long expectedVersion,boolean confirmed,@NotBlank @Size(max=1000) String reason) {}
    public record DecidePlan(@NotNull PlanDecision decision,@NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=1000) String reason,@Size(max=1000) String feedback) {}
    public record CaptureEvidence(@NotNull Source source,@NotNull UUID sourceId,@NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=1000) String reason) {}
    public record Presentation(@NotBlank @Size(max=1000) String title,@NotNull Instant occurredAt,@NotBlank @Size(max=1000) String reference,
                               @NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=1000) String reason) {}
    public record AssessPillar(@NotNull Pillar pillar,@NotNull BigDecimal score,@NotEmpty @Size(max=100) List<UUID> evidenceIds,
                              @NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=1000) String reason) {}
    public record RequestReview(@NotNull UUID calculationId,@NotBlank @Size(max=1000) String reason) {}
    public record DecideReview(boolean accepted,@NotNull @PositiveOrZero Long expectedVersion,boolean confirmed,@NotBlank @Size(max=1000) String reason) {}
    public record ForwardAssessment(@NotNull @PositiveOrZero Long expectedVersion,boolean confirmed,@NotBlank @Size(max=1000) String receiptReference,
                                     @NotBlank @Size(max=1000) String reason) {}
}
