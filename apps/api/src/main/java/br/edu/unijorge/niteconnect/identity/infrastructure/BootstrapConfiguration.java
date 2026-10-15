package br.edu.unijorge.niteconnect.identity.infrastructure;

import br.edu.unijorge.niteconnect.identity.application.BootstrapService;
import br.edu.unijorge.niteconnect.identity.application.EntraProperties;
import jakarta.validation.constraints.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;
import org.springframework.core.env.Environment;

import java.util.UUID;

@Configuration
@ConditionalOnProperty(name="nite.bootstrap.enabled", havingValue="true")
@EnableConfigurationProperties(BootstrapConfiguration.Properties.class)
public class BootstrapConfiguration {
    @Bean
    ApplicationRunner bootstrapRunner(BootstrapService service, EntraProperties entra, Properties properties,
                                       ConfigurableApplicationContext context, Environment environment) {
        // Refuse HTTP mode before the server is started, during bean creation.
        if (!"none".equalsIgnoreCase(environment.getProperty("spring.main.web-application-type", ""))) {
            throw new IllegalStateException("Bootstrap exige spring.main.web-application-type=none");
        }
        return args -> {
            service.provision(entra.tenantId(), properties.objectId(), properties.name(), properties.institutionalEmail(), properties.reason());
            context.close();
        };
    }
    @Validated
    @ConfigurationProperties("nite.bootstrap")
    public record Properties(@NotNull UUID objectId, @NotBlank @Size(max=160) String name,
                             @NotBlank @Email @Size(max=254) String institutionalEmail,
                             @NotBlank @Size(max=1000) String reason) {}
}
