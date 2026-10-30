package br.edu.unijorge.niteconnect.projecthealth.api;

import br.edu.unijorge.niteconnect.projecthealth.domain.ProjectHealthRules.Status;
import br.edu.unijorge.niteconnect.projects.domain.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProjectHealthContracts {
    private ProjectHealthContracts() {}

    public enum PolicyOrigin { BASELINE_APROVADA, CONFIGURADA_POR_GESTAO }

    public record PolicyChange(@NotNull @PositiveOrZero Integer expectedVersion,
                               @NotNull @Positive Integer attentionOverdueDays,
                               @NotNull @Positive Integer criticalOverdueDays,
                               @Positive Integer attentionNoUpdateDays,
                               @Positive Integer criticalNoUpdateDays,
                               @NotBlank @Size(max=1000) String reason) {}

    public record PolicyVersion(UUID id, UUID projectId, int version,
                                int attentionOverdueDays, int criticalOverdueDays,
                                Integer attentionNoUpdateDays, Integer criticalNoUpdateDays,
                                Instant validFrom, Instant validUntil, UUID createdBy, PolicyOrigin origin, String reason) {}

    public record Health(UUID projectId, ProjectStatus projectStatus, Status status, int policyVersion,
                         int maxOverdueDays, Integer daysSinceRelevantUpdate,
                         Integer attentionOverdueDays, Integer criticalOverdueDays,
                         Integer attentionNoUpdateDays, Integer criticalNoUpdateDays,
                         List<String> reasons, List<UUID> sourceActivityIds,
                         List<UUID> overdueActivityIds, UUID latestUpdatedActivityId, Instant evaluatedAt) {}

    public record Evaluation(UUID id, UUID projectId, int policyVersion, Status status,
                             int maxOverdueDays, Integer daysSinceRelevantUpdate,
                             int sourceActivityCount, String reason, UUID evaluatedBy, Instant evaluatedAt,
                             List<UUID> sourceActivityIds) {}
}
