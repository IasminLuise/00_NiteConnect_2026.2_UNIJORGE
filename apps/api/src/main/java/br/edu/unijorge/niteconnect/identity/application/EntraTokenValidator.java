package br.edu.unijorge.niteconnect.identity.application;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Arrays;
import java.util.UUID;

/** Additional access-token checks; issuer, signature and timestamps are validated by the decoder. */
public final class EntraTokenValidator implements OAuth2TokenValidator<Jwt> {
    private final EntraProperties properties;

    public EntraTokenValidator(EntraProperties properties) {
        this.properties = properties;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            boolean valid = properties.tenantId().equals(UUID.fromString(jwt.getClaimAsString("tid")))
                    && UUID.fromString(jwt.getClaimAsString("oid")) != null
                    && "2.0".equals(jwt.getClaimAsString("ver"))
                    && jwt.getAudience().contains(properties.audience())
                    && jwt.getExpiresAt() != null && jwt.getNotBefore() != null
                    && jwt.getSubject() != null && !jwt.getSubject().isBlank()
                    && jwt.getClaimAsString("scp") != null
                    && Arrays.asList(jwt.getClaimAsString("scp").split(" +"))
                        .contains(properties.requiredScope())
                    && !"app".equals(jwt.getClaimAsString("idtyp"));
            if (valid) {
                return OAuth2TokenValidatorResult.success();
            }
        } catch (IllegalArgumentException | NullPointerException ex) {
            // Malformed identity claims never enter application authorization.
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                "Token de acesso institucional inválido", null));
    }
}
