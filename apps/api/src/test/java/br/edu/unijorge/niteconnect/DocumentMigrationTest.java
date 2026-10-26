package br.edu.unijorge.niteconnect;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class DocumentMigrationTest {
    @Container static final PostgreSQLContainer postgres=new PostgreSQLContainer("postgres:17-alpine");
    @Test void upgradesV5ToV7WithoutLosingExistingAuditAndEnforcesVersionBinding() {
        var old=Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()).target("5").load();
        old.migrate();
        var jdbc=new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()));
        var actor=java.util.UUID.randomUUID();
        jdbc.update("insert into nite_connect.members(id,name,institutional_email,status) values (?,'Sintético','document-migration@unijorge.com.br','ATIVO')",actor);
        jdbc.update("insert into nite_connect.audit_events(id,actor_id,member_id,action,reason) values (?,?,?,'TEST','Antes de V6')",java.util.UUID.randomUUID(),actor,actor);
        var current=Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()).load();
        assertThat(current.migrate().migrationsExecuted).isEqualTo(2);current.validate();
        assertThat(current.info().current().getVersion().getVersion()).isEqualTo("7");
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.audit_events",Long.class)).isEqualTo(1);
        for(String type:java.util.List.of("DOCUMENT","DOCUMENT_TEMPLATE"))
            jdbc.update("insert into nite_connect.audit_events(id,actor_id,entity_type,entity_id,action,reason) values (?,?,?,?,'TEST','Depois de V6/V7')",java.util.UUID.randomUUID(),actor,type,java.util.UUID.randomUUID());
        assertThatThrownBy(()->jdbc.update("insert into nite_connect.audit_events(id,actor_id,entity_type,entity_id,action,reason) values (?,?,'UNKNOWN',?,'TEST','Inválido')",java.util.UUID.randomUUID(),actor,java.util.UUID.randomUUID())).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='nite_connect' and table_name like 'document%'",Long.class)).isEqualTo(10);
    }
}
