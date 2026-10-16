package br.edu.unijorge.niteconnect;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class Block03MigrationTest {
    @Container static final PostgreSQLContainer postgres=new PostgreSQLContainer("postgres:17-alpine");
    @Test void upgradeFromV3PreservesMembersProjectsAndImmutableAudit() {
        Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()).target("3").load().migrate();
        var jdbc=new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()));
        UUID member=UUID.randomUUID(),project=UUID.randomUUID(),event=UUID.randomUUID();
        jdbc.update("insert into nite_connect.members(id,name,institutional_email,status) values (?,'Migração','migration@unijorge.com.br','ATIVO')",member);
        jdbc.update("insert into nite_connect.projects(id,title,description,created_by) values (?,'Projeto anterior','Preservado',?)",project,member);
        jdbc.update("insert into nite_connect.audit_events(id,actor_id,entity_type,entity_id,action,reason) values (?,?,'PROJECT',?,'PROJECT_CREATED','Anterior à V4')",event,member,project);
        var flyway=Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("4");
        assertThat(jdbc.queryForObject("select title from nite_connect.projects where id=?",String.class,project)).isEqualTo("Projeto anterior");
        assertThat(jdbc.queryForObject("select reason from nite_connect.audit_events where id=?",String.class,event)).isEqualTo("Anterior à V4");
        assertThatThrownBy(()->jdbc.update("update nite_connect.audit_events set reason='Reescrito' where id=?",event)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
}
