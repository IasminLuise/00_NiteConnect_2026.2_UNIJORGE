package br.edu.unijorge.niteconnect.activities.api;

import jakarta.validation.constraints.*;
import java.util.UUID;

public final class EvidenceContracts {
    private EvidenceContracts() {}
    public enum Type { TEXT, LINK, TECHNICAL }
    public record Create(@NotNull Type type,@Size(max=10000) String content,@Size(max=2048) String url,UUID correctsEvidenceId,
                         @NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=1000) String reason) {}
    public record Upload(@NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=1000) String reason,UUID correctsEvidenceId) {}
}
