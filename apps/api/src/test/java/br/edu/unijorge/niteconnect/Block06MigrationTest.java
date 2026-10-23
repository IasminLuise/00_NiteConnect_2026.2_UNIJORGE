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
        assertThat(Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()).target("5").load().migrate().migrationsExecuted).isEqualTo(2);
        var flyway=Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword()).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("7");
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables where table_schema='nite_connect' and table_name='academic_assessment_evidence'",Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select name from nite_connect.members where id=?::uuid",String.class,id)).isEqualTo("Migração");
        jdbc.update("insert into nite_connect.academic_terms(id,title,starts_at,ends_at,zone) values (?::uuid,'Teste','2026-01-01','2027-01-01','UTC')",id);
        jdbc.update("insert into nite_connect.academic_policies(id,term_id,revision,status,rules,authority_reference) values (?::uuid,?::uuid,1,'CONFIRMED','{}','Teste')",id,id);
        assertThatThrownBy(()->jdbc.update("update nite_connect.academic_policies set rules='{\"scale\":3}' where id=?::uuid",id)).isInstanceOf(org.springframework.dao.DataAccessException.class);
        jdbc.update("insert into nite_connect.academic_events(id,entity_type,entity_id,actor_id,action,reason) values (?::uuid,'TERM',?::uuid,?::uuid,'TEST','Teste')",id,id,id);
        assertThatThrownBy(()->jdbc.update("delete from nite_connect.academic_events where id=?::uuid",id)).isInstanceOf(org.springframework.dao.DataAccessException.class);
        java.util.UUID first=java.util.UUID.randomUUID(),second=java.util.UUID.randomUUID(),other=java.util.UUID.randomUUID(),evidence=java.util.UUID.randomUUID(),assessment=java.util.UUID.randomUUID();
        jdbc.update("insert into nite_connect.members(id,name,institutional_email,status) values (?,'Outro','other-migration@unijorge.com.br','ATIVO')",other);
        jdbc.update("insert into nite_connect.academic_enrollments(id,term_id,member_id,policy_id,status,disciplines) values (?,?::uuid,?::uuid,?::uuid,'DEFERIDA','[]')",first,id,id,id);
        jdbc.update("insert into nite_connect.academic_enrollments(id,term_id,member_id,policy_id,status,disciplines) values (?,?::uuid,?,?::uuid,'DEFERIDA','[]')",second,id,other,id);
        jdbc.update("insert into nite_connect.academic_evidence(id,enrollment_id,snapshot,captured_by) values (?,?,'{}',?::uuid)",evidence,second,id);
        var tx=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));
        assertThatThrownBy(()->tx.executeWithoutResult(status->{
            jdbc.update("insert into nite_connect.academic_assessments(id,enrollment_id,pillar,score,reason,revision,assessed_by) values (?,?,'DELIVERIES',5,'Teste',1,?::uuid)",assessment,first,id);
            jdbc.update("insert into nite_connect.academic_assessment_evidence(assessment_id,enrollment_id,evidence_id,position) values (?,?,?,0)",assessment,first,evidence);
        })).isInstanceOf(org.springframework.dao.DataAccessException.class);
        assertThat(jdbc.queryForObject("select count(*) from nite_connect.academic_assessments where id=?",Long.class,assessment)).isZero();
    }
}
