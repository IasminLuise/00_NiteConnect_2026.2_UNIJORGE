package br.edu.unijorge.niteconnect;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ProjectHealthMigrationTest {
    @Container static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Test
    void v11BackfillsExistingProjectsAndSeedsProjectsCreatedAfterUpgrade() {
        var dataSource = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        var v10 = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:Migrations").target("10").load();
        v10.migrate();
        var jdbc = new JdbcTemplate(dataSource);

        UUID memberId = UUID.randomUUID();
        UUID existingProjectId = UUID.randomUUID();
        jdbc.update("insert into nite_connect.members(id,name,institutional_email,status) values (?,?,?,'ATIVO')",
                memberId, "Migração", memberId + "@unijorge.com.br");
        jdbc.update("insert into nite_connect.projects(id,title,description,created_by) values (?,?,?,?)",
                existingProjectId, "Projeto existente", "Criado antes da V11", memberId);

        var v11 = Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:Migrations").target("11").load();
        assertThat(v11.migrate().migrationsExecuted).isEqualTo(1);
        v11.validate();

        assertBaseline(jdbc, existingProjectId);

        UUID newProjectId = UUID.randomUUID();
        jdbc.update("insert into nite_connect.projects(id,title,description,created_by) values (?,?,?,?)",
                newProjectId, "Projeto novo", "Criado após a V11", memberId);
        assertBaseline(jdbc, newProjectId);
    }

    private static void assertBaseline(JdbcTemplate jdbc, UUID projectId) {
        assertThat(jdbc.queryForObject("""
                select count(*) from nite_connect.project_health_policy_versions
                where project_id=? and version=1 and attention_overdue_days=7 and critical_overdue_days=14
                  and attention_no_update_days=14 and critical_no_update_days=30
                  and origin='BASELINE_APROVADA' and created_by is null and valid_until is null
                """, Long.class, projectId)).isEqualTo(1);
    }
}
