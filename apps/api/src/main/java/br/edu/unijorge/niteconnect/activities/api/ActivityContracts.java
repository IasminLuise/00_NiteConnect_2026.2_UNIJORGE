package br.edu.unijorge.niteconnect.activities.api;

import br.edu.unijorge.niteconnect.activities.domain.ActivityStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public final class ActivityContracts {
    private ActivityContracts() {}
    public record Create(@NotBlank @Size(max=200) String title,@NotBlank @Size(max=10000) String description,
                         UUID projectId,LocalDate dueOn,@NotNull UUID primaryExecutorId,@NotBlank @Size(max=1000) String reason) {}
    public record Update(@NotBlank @Size(max=200) String title,@NotBlank @Size(max=10000) String description,
                         LocalDate dueOn,@NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=1000) String reason,
                         tools.jackson.databind.JsonNode projectId) {}
    public record StatusChange(@NotNull ActivityStatus status,@NotNull @PositiveOrZero Long expectedVersion,
                               @NotBlank @Size(max=1000) String reason,Boolean confirmed) {}
    public record Assign(@NotNull UUID memberId,@NotNull @PositiveOrZero Long expectedVersion,
                         @NotBlank @Size(max=1000) String reason,Boolean confirmed) {}
    public record Command(@NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=1000) String reason,Boolean confirmed) {}
}
