package br.edu.unijorge.niteconnect.identity.infrastructure;

import br.edu.unijorge.niteconnect.identity.application.EntraProperties;
import br.edu.unijorge.niteconnect.identity.application.EntraTokenValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
@ConditionalOnProperty(name = "nite.entra.enabled", havingValue = "true")
@EnableConfigurationProperties(EntraProperties.class)
public class EntraConfiguration {
    @Bean
    JwtDecoder jwtDecoder(EntraProperties properties) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).build();
        decoder.setJwtValidator(tokenValidator(properties));
        return decoder;
    }

    public static OAuth2TokenValidator<Jwt> tokenValidator(EntraProperties properties) {
        return new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(properties.issuer()),
                new EntraTokenValidator(properties));
    }
}
