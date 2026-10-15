package br.edu.unijorge.niteconnect;

import br.edu.unijorge.niteconnect.identity.infrastructure.EntraConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import static org.assertj.core.api.Assertions.assertThat;

class EntraConfigurationTest {
    private final ApplicationContextRunner contexts = new ApplicationContextRunner().withUserConfiguration(EntraConfiguration.class);
    @Test
    void disabledEntraDoesNotInstallAnAuthenticationDecoder() {
        contexts.withPropertyValues("nite.entra.enabled=false").run(context -> assertThat(context).doesNotHaveBean(JwtDecoder.class));
    }
    @Test
    void enabledEntraWithoutInstitutionalConfigurationFailsClosed() {
        contexts.withPropertyValues("nite.entra.enabled=true").run(context -> assertThat(context).hasFailed());
    }
    @Test
    void invalidTenantCannotSelectAnArbitraryIdentityProvider() {
        contexts.withPropertyValues("nite.entra.enabled=true", "nite.entra.tenant-id=https://example.org",
                "nite.entra.audience=test-api", "nite.entra.required-scope=access_as_user")
                .run(context -> assertThat(context).hasFailed());
    }
}
