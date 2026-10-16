package br.edu.unijorge.niteconnect.activities.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Activity(UUID id, String title, String description, UUID projectId, ActivityStatus status,
                       LocalDate dueOn, UUID createdBy, long version, Instant createdAt, Instant updatedAt,
                       Instant completedAt, UUID primaryExecutorId) {
    public boolean overdue(LocalDate today) { return dueOn != null && dueOn.isBefore(today) && !status.terminal(); }
}
