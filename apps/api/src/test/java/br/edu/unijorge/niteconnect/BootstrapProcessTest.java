package br.edu.unijorge.niteconnect;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.*;

@Testcontainers
class BootstrapProcessTest {
    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Test
    void oneShotBootstrapCreatesFirstCoordinatorAndCannotRunAgain() {
        String[] args = {
                "--spring.main.web-application-type=none",
                "--spring.datasource.url=" + postgres.getJdbcUrl(),
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword(),
                "--nite.entra.enabled=true",
                "--nite.entra.tenant-id=11111111-1111-4111-8111-111111111111",
                "--nite.entra.audience=22222222-2222-4222-8222-222222222222",
                "--nite.entra.required-scope=access_as_user",
                "--nite.bootstrap.enabled=true",
                "--nite.bootstrap.object-id=bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
                "--nite.bootstrap.name=Coordenador de teste",
                "--nite.bootstrap.institutional-email=bootstrap-test@unijorge.com.br",
                "--nite.bootstrap.reason=Provisionamento inicial de teste"
        };
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(NiteConnectApplication.class)
                .web(WebApplicationType.NONE).run(args)) {
            JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
            assertThat(jdbc.queryForObject("select count(*) from nite_connect.role_assignments where role='COORDENADOR'", Integer.class)).isEqualTo(1);
            assertThat(jdbc.queryForObject("select count(*) from nite_connect.audit_events where action='ADMIN_BOOTSTRAPPED'", Integer.class)).isEqualTo(1);
            assertThat(context.isActive()).isFalse();
        }
        assertThatThrownBy(() -> {
            try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(NiteConnectApplication.class)
                    .web(WebApplicationType.NONE).run(args)) { }
        }).isInstanceOf(br.edu.unijorge.niteconnect.shared.api.ApiException.class);
    }
}
