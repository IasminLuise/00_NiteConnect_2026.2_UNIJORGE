package br.edu.unijorge.niteconnect;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class Block02MigrationTest {
    @Container static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");
    @Test void upgradePreservesV2MembersStatusHistoryAndImmutableAuditWithoutBackfill() {
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .target("2").load().migrate();
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()));
        UUID member = UUID.randomUUID(), event = UUID.randomUUID(), history = UUID.randomUUID();
        jdbc.update("insert into nite_connect.members(id,name,institutional_email,status) values (?,'Teste migração','migration-test@unijorge.com.br','ATIVO')", member);
        jdbc.update("insert into nite_connect.audit_events(id,actor_id,member_id,action,reason) values (?,?,?,'MEMBER_CREATED','Registro anterior')", event, member, member);
        jdbc.update("insert into nite_connect.member_status_history(id,member_id,actor_id,new_status,reason) values (?,?,?,'ATIVO','Histórico anterior')", history, member, member);
        var flyway = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword()).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        flyway.validate();
        assertThat(jdbc.queryForObject("select name from nite_connect.members where id=?", String.class, member)).isEqualTo("Teste migração");
        assertThat(jdbc.queryForObject("select reason from nite_connect.audit_events where id=?", String.class, event)).isEqualTo("Registro anterior");
        assertThat(jdbc.queryForObject("select entity_type from nite_connect.audit_events where id=?", String.class, event)).isEqualTo("MEMBER");
        assertThat(jdbc.queryForObject("select coalesce(entity_id,member_id) from nite_connect.audit_events where id=?", UUID.class, event)).isEqualTo(member);
        assertThat(jdbc.queryForObject("select reason from nite_connect.member_status_history where id=?", String.class, history)).isEqualTo("Histórico anterior");
        assertThatThrownBy(() -> jdbc.update("update nite_connect.audit_events set reason='alterado' where id=?", event)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
}
