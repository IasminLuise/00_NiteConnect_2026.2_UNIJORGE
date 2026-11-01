package br.edu.unijorge.niteconnect.attendance.api;

import br.edu.unijorge.niteconnect.attendance.domain.Participation.Modality;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.PeriodKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
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
    public enum WorkLogCorrectionDecisionType { ACEITA, RECUSADA }
    public enum LoadPolicyEffectiveWindow { PRIMEIRA_SEMANA_OFICIAL_DE_ATIVIDADES }

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
    public record PersonalCalendarVersion(UUID id, LocalDate weekStart, LocalDate weekEnd, int version,
                                          CalendarExceptionType type, BigDecimal presentialHoursRelieved,
                                          BigDecimal remoteHoursRelieved, UUID affectedEventId,
                                          String affectedEventTitle, UUID replacementEventId,
                                          String replacementEventTitle, Instant validFrom, Instant validUntil) {}
    public record LoadPolicyVersion(UUID id, int version, LocalDate effectiveFrom, LocalDate effectiveUntil,
                                    BigDecimal presentialHours, BigDecimal remoteHours) {}
    public record InstitutionalLoadConfirmation(BigDecimal presentialHours, BigDecimal remoteHours, String status) {}
    public record PlannedLoadPolicy(UUID id, int version, BigDecimal presentialHours, BigDecimal remoteHours,
                                    LocalDate effectiveFrom, String intendedSemester,
                                    LoadPolicyEffectiveWindow effectiveWindow, String status) {}
    public record LoadPolicySummary(String status, LoadPolicyVersion currentConfiguration,
                                    LoadPolicyVersion applicableToWeek,
                                    InstitutionalLoadConfirmation institutionalConfirmation,
                                    PlannedLoadPolicy plannedConfiguration) {}
    public record CreateLoadPolicyDraft(@NotNull @PositiveOrZero @Digits(integer=5,fraction=2) BigDecimal presentialHours,
                                        @NotNull @PositiveOrZero @Digits(integer=5,fraction=2) BigDecimal remoteHours,
                                        LocalDate effectiveFrom,
                                        @jakarta.validation.constraints.Pattern(regexp="^\\d{4}\\.[1-2]$") String intendedSemester,
                                        LoadPolicyEffectiveWindow effectiveWindow,
                                        @NotBlank @Size(max=1000) String reason) {}
    public record ReviseLoadPolicyDraft(@NotNull @PositiveOrZero Integer expectedVersion,
                                        @NotNull @PositiveOrZero @Digits(integer=5,fraction=2) BigDecimal presentialHours,
                                        @NotNull @PositiveOrZero @Digits(integer=5,fraction=2) BigDecimal remoteHours,
                                        LocalDate effectiveFrom,
                                        @jakarta.validation.constraints.Pattern(regexp="^\\d{4}\\.[1-2]$") String intendedSemester,
                                        LoadPolicyEffectiveWindow effectiveWindow,
                                        @NotBlank @Size(max=1000) String reason) {}
    public record ApproveLoadPolicyDraft(@NotNull @PositiveOrZero Integer expectedVersion,
                                         @NotBlank @Size(max=1000) String sourceReference,
                                         @NotBlank @Size(max=1000) String reason) {}
    public record ActivateLoadPolicyDraft(@NotNull @PositiveOrZero Integer expectedVersion,
                                          @NotBlank @Size(max=1000) String reason) {}
    public record LoadPolicyEventView(UUID id, UUID draftId, String type, UUID actorId, Instant occurredAt,
                                      String reason, String sourceReference) {}
    public record LoadPolicyAdministrationView(UUID id, int version, BigDecimal presentialHours,
                                               BigDecimal remoteHours, LocalDate effectiveFrom,
                                               String intendedSemester, LoadPolicyEffectiveWindow effectiveWindow,
                                               UUID createdBy, Instant createdAt, String creationReason,
                                               String status, List<LoadPolicyEventView> events) {}
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
    public record ReplacementPeriod(UUID periodId, PeriodKind kind, Modality modality, String reviewStatus,
                                   Instant startsAt, Instant endsAt, String eventTitle, String description,
                                   long recognizedSeconds, Instant assignedAt) {}
    public record ReplacementView(AdjustmentView adjustment, List<ReplacementPeriod> periods,
                                  long recognizedSeconds, Long remainingSeconds) {}
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
    public record SubmitWorkLogCorrection(@NotNull @PositiveOrZero Long expectedPeriodVersion,
                                          @NotNull Instant startsAt, @NotNull Instant endsAt,
                                          @NotBlank @Size(max=10000) String description,
                                          UUID activityId, UUID evidenceId,
                                          @NotBlank @Size(max=1000) String reason) {}
    public record DecideWorkLogCorrection(@NotNull @PositiveOrZero Long expectedResponseVersion,
                                          @NotNull WorkLogCorrectionDecisionType decision,
                                          @NotBlank @Size(max=1000) String reason,
                                          Boolean confirmed) {}
    public record WorkLogSnapshot(UUID id, Instant startsAt, Instant endsAt, String description,
                                   Modality modality, UUID activityId, UUID evidenceId,
                                   String reviewStatus, long version) {}
    public record WorkLogCorrectionResponseView(UUID id, Instant proposedStart, Instant proposedEnd,
                                                String proposedDescription, UUID proposedActivityId,
                                                UUID proposedEvidenceId, String reason, Instant respondedAt,
                                                long expectedPeriodVersion, long version,
                                                WorkLogCorrectionDecisionType decision, String decisionReason,
                                                Instant decidedAt) {}
    public record ReviewRequest(UUID id, UUID periodId, LocalDate weekStart, LocalDate weekEnd,
                                String reason, Instant createdAt, String status,
                                WorkLogSnapshot currentWorkLog, WorkLogCorrectionResponseView response) {}
    public record AdministrativeReviewRequest(UUID memberId, String memberName, ReviewRequest request) {}
}
