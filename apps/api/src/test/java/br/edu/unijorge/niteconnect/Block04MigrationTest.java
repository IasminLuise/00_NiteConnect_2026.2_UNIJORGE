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
class Block04MigrationTest {
    @Container static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Test void upgradesV4WithoutRewritingPreviousDataAndProtectsHistory() {
        Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()).target("4").load().migrate();
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()));
        UUID member=UUID.randomUUID(), audit=UUID.randomUUID();
        jdbc.update("insert into nite_connect.members(id,name,institutional_email,status) values (?,'Migração','b04@unijorge.com.br','ATIVO')",member);
        jdbc.update("insert into nite_connect.audit_events(id,actor_id,member_id,action,reason) values (?,?,?,'MEMBER_CREATED','Preservar')",audit,member,member);
        UUID project=UUID.randomUUID(),activity=UUID.randomUUID(),assignment=UUID.randomUUID(),evidence=UUID.randomUUID();
        var migrationTx=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));
        migrationTx.executeWithoutResult(status->{
            jdbc.update("insert into nite_connect.projects(id,title,description,created_by) values (?,'Projeto V4','Preservado',?)",project,member);
            jdbc.update("insert into nite_connect.activities(id,title,description,project_id,created_by) values (?,'Atividade V4','Preservada',?,?)",activity,project,member);
            jdbc.update("insert into nite_connect.activity_assignments(id,activity_id,member_id,role,started_by,start_reason) values (?,?,?,'EXECUTOR',?,'Anterior à V5')",assignment,activity,member,member);
            jdbc.update("insert into nite_connect.activity_evidence(id,activity_id,author_id,assignment_id,type,content) values (?,?,?,?,'TEXT','Evidência V4')",evidence,activity,member,assignment);
        });
        var flyway=Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("5");
        assertThat(jdbc.queryForObject("select reason from nite_connect.audit_events where id=?",String.class,audit)).isEqualTo("Preservar");
        assertThat(jdbc.queryForObject("select title from nite_connect.projects where id=?",String.class,project)).isEqualTo("Projeto V4");
        assertThat(jdbc.queryForObject("select title from nite_connect.activities where id=?",String.class,activity)).isEqualTo("Atividade V4");
        assertThat(jdbc.queryForObject("select content from nite_connect.activity_evidence where id=?",String.class,evidence)).isEqualTo("Evidência V4");
        UUID event=UUID.randomUUID();
        jdbc.update("insert into nite_connect.participation_events(id,entity_type,entity_id,actor_id,action,reason) values (?,'CALENDAR_EVENT',?,?,'CREATED','Registro')",event,UUID.randomUUID(),member);
        assertThatThrownBy(()->jdbc.update("update nite_connect.participation_events set reason='Outro' where id=?",event)).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThatThrownBy(()->jdbc.update("delete from nite_connect.participation_events where id=?",event)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
}
