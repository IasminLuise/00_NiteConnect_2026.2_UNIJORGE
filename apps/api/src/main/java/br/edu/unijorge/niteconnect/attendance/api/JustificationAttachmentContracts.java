package br.edu.unijorge.niteconnect.attendance.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class JustificationAttachmentContracts {
    private JustificationAttachmentContracts() {}

    public enum Context { JUSTIFICATIVA, COMPLEMENTACAO }

    public record AttachmentView(UUID id, Context context, String filename, String contentType,
                                Long sizeBytes, String status, Instant createdAt, long version,
                                LocalDate retentionExpiresOn, LocalDate deletionDeadlineOn,
                                boolean retentionHeld, boolean downloadable) {}
    public record AttachmentCollection(String uploadAvailability, List<AttachmentView> items) {}
    public record RetentionPolicyProposal(@NotNull @Positive Integer retentionMonths,
                                          @NotNull @Min(30) @Max(30) Integer deletionGraceDays,
                                          @NotBlank @Size(max=1000) String reason) {}
    public record RetentionPolicyRevision(@NotNull @PositiveOrZero Integer expectedVersion,
                                          @NotNull @Positive Integer retentionMonths,
                                          @NotNull @Min(30) @Max(30) Integer deletionGraceDays,
                                          @NotBlank @Size(max=1000) String reason) {}
    public record RetentionPolicyValidation(@NotNull @PositiveOrZero Integer expectedVersion,
                                            @NotBlank @Size(max=1000) String sourceReference,
                                            @NotBlank @Size(max=1000) String backupHandlingReference,
                                            @NotBlank @Size(max=1000) String sensitiveDataHandlingReference,
                                            @NotBlank @Size(max=1000) String reason) {}
    public record RetentionPolicyEventView(UUID id, String type, UUID actorId, Instant occurredAt,
                                           String reason, String sourceReference,
                                           String backupHandlingReference, String sensitiveDataHandlingReference) {}
    public record RetentionPolicyView(UUID id, int version, int retentionMonths, int deletionGraceDays,
                                      String anchorRule, UUID createdBy, Instant createdAt,
                                      String creationReason, String status,
                                      List<RetentionPolicyEventView> events) {}
    public record RetentionClassification(@NotNull LocalDate semesterEndOn,
                                          @NotBlank @Size(max=1000) String sourceReference,
                                          @NotBlank @Size(max=1000) String reason) {}
    public record RetentionHold(@NotBlank @Size(max=1000) String basis,
                                @NotBlank @Size(max=1000) String reason) {}
    public record RetentionHoldRelease(@NotBlank @Size(max=1000) String reason) {}
    public record RetentionHoldView(UUID id, UUID attachmentId, String basis, String reason,
                                    UUID actorId, Instant occurredAt) {}
    public record RetentionHoldEventView(UUID eventId, UUID holdId, UUID attachmentId, String type,
                                         UUID actorId, Instant occurredAt, String basis, String reason) {}
    public record DeleteAttachment(@NotNull @PositiveOrZero Long expectedVersion,
                                   Boolean confirmed,
                                   @NotBlank @Size(max=1000) String reason) {}
}
