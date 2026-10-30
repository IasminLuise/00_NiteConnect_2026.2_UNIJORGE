package br.edu.unijorge.niteconnect.attendance.api;

import br.edu.unijorge.niteconnect.attendance.domain.Participation.Modality;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ParticipationGovernanceContracts {
    private ParticipationGovernanceContracts() {}

    public enum CalendarExceptionType { CANCELAMENTO_PRESENCIAL, RECESSO_INTEGRAL, SUSPENSAO_PRESENCIAL, SUSPENSAO_PARCIAL, REMARCACAO }
    public enum AdjustmentDecisionType { ACEITAR, RECUSAR }
    public enum WeeklyReviewAction { APROVAR, REJEITAR, CORRECAO_SOLICITADA }

    public record CalendarChange(@NotNull @PositiveOrZero Integer expectedVersion,
                                 @NotNull LocalDate weekStart, @NotNull LocalDate weekEnd,
                                 @NotNull CalendarExceptionType type,
                                 @NotNull BigDecimal presentialHoursRelieved,
                                 @NotNull BigDecimal remoteHoursRelieved,
                                 UUID affectedEventId, UUID replacementEventId,
                                 @NotBlank @Size(max=1000) String reason) {}
    public record CalendarVersion(UUID id, LocalDate weekStart, LocalDate weekEnd, int version,
                                  CalendarExceptionType type, BigDecimal presentialHoursRelieved,
                                  BigDecimal remoteHoursRelieved, UUID affectedEventId,
                                  UUID replacementEventId, Instant validFrom, Instant validUntil,
                                  UUID createdBy, String reason) {}
    public record ReplacementRequest(@NotNull LocalDate weekStart, @NotNull LocalDate weekEnd,
                                     @NotNull Modality modality, @NotNull @Positive BigDecimal hours,
                                     @NotBlank @Size(max=1000) String reason) {}
    public record ReplacementDecision(@NotNull @PositiveOrZero Long expectedVersion,
                                      @NotNull AdjustmentDecisionType decision,
                                      Modality authorizedModality, @Positive BigDecimal authorizedHours,
                                      LocalDate dueOn, @NotBlank @Size(max=1000) String reason) {}
    public record DispensationGrant(@NotNull UUID memberId, @NotNull LocalDate weekStart,
                                    @NotNull LocalDate weekEnd, @NotNull Modality modality,
                                    @NotNull @Positive BigDecimal hours,
                                    @NotBlank @Size(max=1000) String reason) {}
    public record AdjustmentView(UUID id, UUID memberId, LocalDate sourceWeekStart, LocalDate sourceWeekEnd,
                                 String kind, Modality requestedModality, BigDecimal requestedHours,
                                 Modality authorizedModality, BigDecimal authorizedHours, LocalDate dueOn,
                                 String status, UUID requestedBy, Instant requestedAt,
                                 UUID decidedBy, Instant decidedAt, String decisionReason, String reason,
                                 long version) {}
    public record ReplacementAllocation(UUID adjustmentId, UUID periodId, UUID assignedBy, Instant assignedAt) {}
    public record WeeklyReviewItem(@NotNull UUID periodId, @NotNull @PositiveOrZero Long expectedVersion,
                                   @NotNull WeeklyReviewAction action,
                                   @NotBlank @Size(max=1000) String reason) {}
    public record WeeklyReviewBatch(@NotNull LocalDate weekStart, @NotNull LocalDate weekEnd,
                                    @NotBlank @Size(max=1000) String reason,
                                    @NotNull @Size(min=1,max=100) List<@Valid WeeklyReviewItem> items) {}
    public record WeeklyReviewBatchResult(UUID id, LocalDate weekStart, LocalDate weekEnd,
                                          UUID reviewedBy, Instant createdAt,
                                          List<WeeklyReviewItemResult> items) {}
    public record WeeklyReviewItemResult(UUID periodId, WeeklyReviewAction action, long previousVersion,
                                         long resultingVersion, String reason) {}
    public record ReviewRequest(UUID id, UUID periodId, LocalDate weekStart, LocalDate weekEnd,
                                String reason, Instant createdAt) {}
}
