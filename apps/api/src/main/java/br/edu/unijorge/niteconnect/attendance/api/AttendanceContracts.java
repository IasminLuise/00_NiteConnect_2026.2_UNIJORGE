package br.edu.unijorge.niteconnect.attendance.api;

import br.edu.unijorge.niteconnect.attendance.domain.Participation.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AttendanceContracts {
    private AttendanceContracts() {}
    public record EventCreate(@NotBlank @Size(max=200) String title, @NotNull @Size(max=10000) String description,
                              @NotNull EventKind kind, @NotNull Modality modality, @NotNull Instant startsAt,
                              @NotNull Instant endsAt, @NotNull @Size(max=1000) List<@NotNull UUID> expectedMemberIds,
                              @NotBlank @Size(max=1000) String reason) {}
    public record EventUpdate(@NotBlank @Size(max=200) String title, @NotNull @Size(max=10000) String description,
                              @NotNull EventKind kind, @NotNull Modality modality, @NotNull Instant startsAt,
                              @NotNull Instant endsAt, @NotNull @Size(max=1000) List<@NotNull UUID> expectedMemberIds,
                              @NotNull @PositiveOrZero Long expectedVersion, @NotBlank @Size(max=1000) String reason) {}
    public record Command(@NotNull @PositiveOrZero Long expectedVersion, @NotBlank @Size(max=1000) String reason,
                          Boolean confirmed) {}
    public record CheckIn(@NotNull UUID eventId, @NotBlank @Size(max=64) String payload) {}
    public record CheckOut(@NotNull @PositiveOrZero Long expectedVersion) {}
    public record Return(@NotNull @PositiveOrZero Long expectedVersion, @NotBlank @Size(max=64) String payload) {}
    public record WorkLog(@NotNull Instant startsAt, @NotNull Instant endsAt,
                          @NotBlank @Size(max=10000) String description, UUID activityId, UUID evidenceId) {}
    public record Justify(@NotNull UUID eventId, @NotBlank @Size(max=1000) String reason) {}
    public record Complement(@NotNull @PositiveOrZero Long expectedVersion, @NotBlank @Size(max=1000) String reason) {}
    public record Decide(@NotNull @PositiveOrZero Long expectedVersion, @NotNull Decision decision,
                         @NotBlank @Size(max=1000) String reason) {}
    public record Review(@NotNull @PositiveOrZero Long expectedVersion, @NotNull ReviewStatus status,
                         @NotBlank @Size(max=1000) String reason) {}
    public record RequestCorrection(UUID periodId, UUID eventId, @NotNull Instant startsAt, @NotNull Instant endsAt,
                                    @NotBlank @Size(max=1000) String reason) {}
    public record Correct(@NotNull @PositiveOrZero Long expectedVersion, @NotNull Instant startsAt,
                          @NotNull Instant endsAt, @NotBlank @Size(max=1000) String reason, Boolean confirmed) {}
    public record DecideCorrection(@NotNull @PositiveOrZero Long expectedVersion, @NotNull Decision decision,
                                   @PositiveOrZero Long expectedPeriodVersion, @NotBlank @Size(max=1000) String reason,
                                   Boolean confirmed) {}
}
