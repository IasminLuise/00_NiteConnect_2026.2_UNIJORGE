package br.edu.unijorge.niteconnect.attendance.api;

import br.edu.unijorge.niteconnect.attendance.domain.Frequency.Outcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class FrequencyContracts {
    private FrequencyContracts() {}

    public record OpenList(
            java.util.UUID calendarEventId,
            @Size(max=200) String meetingName,
            @NotNull Integer confirmationWindowMinutes,
            Boolean countsForFrequency) {}

    public record RenewQr(@NotNull @PositiveOrZero Long expectedVersion) {}

    public record FinalizeList(
            @NotNull @PositiveOrZero Long expectedVersion,
            @NotNull Outcome outcome,
            @NotBlank @Size(max=1000) String reason) {}

    public record CancelList(
            @NotNull @PositiveOrZero Long expectedVersion,
            @NotBlank @Size(max=1000) String reason,
            Boolean confirmed) {}

    public record ConfirmPresence(
            @Size(max=64) String qrToken,
            @Size(max=64) String handoffId) {}

    public record CreateHandoff(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String qrToken) {}

    public record ChangeParticipant(
            @NotNull @PositiveOrZero Long expectedVersion,
            @NotBlank @Size(max=20) String targetStatus,
            @NotBlank @Pattern(regexp="EMAIL_EXTERNO_RECEBIDO|CORRECAO_ADMINISTRATIVA") String reason) {}
}
