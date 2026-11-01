package br.edu.unijorge.niteconnect;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class Block04RetirementSafetyTest {
    @Container static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Test
    void ambiguousAuditPeriodStopsUpgradeWithoutRemovingLegacySchemaOrRows() {
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:Migrations").target("20").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        UUID member = UUID.randomUUID();
        UUID event = UUID.randomUUID();
        jdbc.update("insert into nite_connect.members(id,name,institutional_email,status) values (?,'Safety fixture','safety@unijorge.com.br','ATIVO')", member);
        jdbc.update("""
                insert into nite_connect.audit_events(id,actor_id,entity_type,entity_id,action,reason)
                values (?,?, 'PERIOD',?,'LEGACY_UNKNOWN','Fixture ambíguo')
                """, event, member, UUID.randomUUID());

        Flyway upgraded = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:Migrations").load();
        assertThatThrownBy(upgraded::migrate).isInstanceOf(FlywayException.class);

        assertThat(jdbc.queryForObject("select version from flyway_schema_history where success order by installed_rank desc limit 1", String.class)).isEqualTo("20");
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.audit_events where id=?", Long.class, event)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select to_regclass('nite_connect.participation_periods')", String.class)).isEqualTo("nite_connect.participation_periods");
        assertThat(jdbc.queryForObject("select to_regclass('nite_connect.attendance_segments')", String.class)).isNull();
        assertThat(jdbc.queryForObject("select to_regclass('nite_connect.absence_justifications')", String.class)).isEqualTo("nite_connect.absence_justifications");
    }
}
