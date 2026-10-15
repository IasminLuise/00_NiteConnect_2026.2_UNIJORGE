package br.edu.unijorge.niteconnect.identity.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.UUID;

@Validated
@ConfigurationProperties("nite.entra")
public record EntraProperties(@NotNull UUID tenantId, @NotBlank String audience,
                              @NotBlank String requiredScope) {
    public String issuer() {
        return "https://login.microsoftonline.com/" + tenantId + "/v2.0";
    }

    public String jwkSetUri() {
        return "https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys";
    }
}
