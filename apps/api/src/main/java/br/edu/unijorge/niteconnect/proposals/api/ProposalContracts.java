package br.edu.unijorge.niteconnect.proposals.api;

import br.edu.unijorge.niteconnect.proposals.domain.ProposalStatus;
import jakarta.validation.constraints.*;

public final class ProposalContracts {
    private ProposalContracts() {}
    public record Create(@NotBlank @Size(max=200) String title, @NotBlank @Size(max=10000) String description,
                         @NotBlank @Size(max=1000) String reason) {}
    public record Update(@NotBlank @Size(max=200) String title, @NotBlank @Size(max=10000) String description,
                         @NotNull @PositiveOrZero Long expectedVersion, @NotBlank @Size(max=1000) String reason) {}
    public record Decision(@NotNull ProposalStatus status, @NotNull @PositiveOrZero Long expectedVersion,
                           @NotBlank @Size(max=1000) String reason) {}
    public record Convert(@NotNull @PositiveOrZero Long expectedVersion, @NotBlank @Size(max=1000) String reason) {}
}
