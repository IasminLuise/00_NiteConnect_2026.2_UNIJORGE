package br.edu.unijorge.niteconnect;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class Block06MigrationTest {
    @Container static final PostgreSQLContainer postgres=new PostgreSQLContainer("postgres:17-alpine");
    @Test void upgradesPersistentV3ThroughAcademicV6AndProtectsPoliciesAndHistory() {
        var config=Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword());
        config.target("3").load().migrate();
        var jdbc=new JdbcTemplate(new DriverManagerDataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()));
        String id="10000000-0000-4000-8000-000000000001";
        jdbc.update("insert into nite_connect.members(id,name,institutional_email,status) values (?::uuid,'Migração','migration@unijorge.com.br','ATIVO')",id);
        var flyway=Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(3);flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("6");
        assertThat(jdbc.queryForObject("select name from nite_connect.members where id=?::uuid",String.class,id)).isEqualTo("Migração");
        jdbc.update("insert into nite_connect.academic_terms(id,title,starts_at,ends_at,zone) values (?::uuid,'Teste','2026-01-01','2027-01-01','UTC')",id);
        jdbc.update("insert into nite_connect.academic_policies(id,term_id,revision,status,rules,authority_reference) values (?::uuid,?::uuid,1,'CONFIRMED','{}','Teste')",id,id);
        assertThatThrownBy(()->jdbc.update("update nite_connect.academic_policies set rules='{\"scale\":3}' where id=?::uuid",id)).isInstanceOf(org.springframework.dao.DataAccessException.class);
        jdbc.update("insert into nite_connect.academic_events(id,entity_type,entity_id,actor_id,action,reason) values (?::uuid,'TERM',?::uuid,?::uuid,'TEST','Teste')",id,id,id);
        assertThatThrownBy(()->jdbc.update("delete from nite_connect.academic_events where id=?::uuid",id)).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
}
