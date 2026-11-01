package br.edu.unijorge.niteconnect.attendance.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class JustificationAttachmentPurgeContracts {
    private JustificationAttachmentPurgeContracts() {}

    public enum Mode { DRY_RUN, EXECUTE }

    public record Command(@NotNull Mode mode, Boolean confirmed,
                         @NotBlank @Size(max=1000) String reason) {}

    public record Item(UUID attachmentId, String outcome, String resultCode, LocalDate retentionExpiresOn,
                       LocalDate deletionDeadlineOn, Instant occurredAt) {}

    public record RunSummary(UUID id, String mode, String status, String resultCode, UUID retentionPolicyId,
                             UUID requestedBy, Instant requestedAt, Instant completedAt, String reason,
                             int candidateCount, int skippedCount, int deletedCount, int failedCount) {}

    public record RunDetail(UUID id, String mode, String status, String resultCode, UUID retentionPolicyId,
                            UUID requestedBy, Instant requestedAt, Instant completedAt, String reason,
                            int candidateCount, int skippedCount, int deletedCount, int failedCount, List<Item> items) {}
}
