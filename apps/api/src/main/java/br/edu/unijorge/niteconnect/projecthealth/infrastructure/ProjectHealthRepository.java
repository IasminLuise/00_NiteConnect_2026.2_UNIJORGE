package br.edu.unijorge.niteconnect.projecthealth.infrastructure;

import br.edu.unijorge.niteconnect.projecthealth.api.ProjectHealthContracts.Evaluation;
import br.edu.unijorge.niteconnect.projecthealth.api.ProjectHealthContracts.PolicyVersion;
import br.edu.unijorge.niteconnect.projecthealth.api.ProjectHealthContracts.PolicyOrigin;
import br.edu.unijorge.niteconnect.projecthealth.domain.ProjectHealthRules.Status;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProjectHealthRepository {
    private final JdbcTemplate jdbc;

    public ProjectHealthRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static PolicyVersion policy(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        Timestamp until = rs.getTimestamp("valid_until");
        return new PolicyVersion(rs.getObject("id", UUID.class), rs.getObject("project_id", UUID.class),
                rs.getInt("version"), rs.getInt("attention_overdue_days"), rs.getInt("critical_overdue_days"),
                (Integer) rs.getObject("attention_no_update_days"), (Integer) rs.getObject("critical_no_update_days"),
                rs.getTimestamp("valid_from").toInstant(), until == null ? null : until.toInstant(),
                rs.getObject("created_by", UUID.class), PolicyOrigin.valueOf(rs.getString("origin")), rs.getString("reason"));
    }

    public Optional<PolicyVersion> currentPolicy(UUID project) {
        return jdbc.query("select * from nite_connect.project_health_policy_versions where project_id=? and valid_until is null",
                ProjectHealthRepository::policy, project).stream().findFirst();
    }

    public int currentVersion(UUID project) {
        return currentPolicy(project).map(PolicyVersion::version).orElse(0);
    }

    public PageResult<PolicyVersion> policies(UUID project, int page, int size) {
        PageResult.validate(page, size);
        return new PageResult<>(jdbc.query("""
                select * from nite_connect.project_health_policy_versions where project_id=?
                order by version desc,id limit ? offset ?
                """, ProjectHealthRepository::policy, project, size, (long) page * size), page, size,
                jdbc.queryForObject("select count(*) from nite_connect.project_health_policy_versions where project_id=?", Long.class, project));
    }

    public PolicyVersion addPolicy(UUID project, int version, int attentionOverdueDays, int criticalOverdueDays,
                                   Integer attentionNoUpdateDays, Integer criticalNoUpdateDays,
                                   UUID actor, String reason, Instant now) {
        currentPolicy(project).ifPresent(current -> jdbc.update(
                "update nite_connect.project_health_policy_versions set valid_until=? where id=? and valid_until is null",
                Timestamp.from(now), current.id()));
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into nite_connect.project_health_policy_versions(
                    id,project_id,version,attention_overdue_days,critical_overdue_days,
                    attention_no_update_days,critical_no_update_days,valid_from,created_by,origin,reason)
                values (?,?,?,?,?,?,?,?,?,?,?)
                """, id, project, version, attentionOverdueDays, criticalOverdueDays,
                attentionNoUpdateDays, criticalNoUpdateDays, Timestamp.from(now), actor,
                PolicyOrigin.CONFIGURADA_POR_GESTAO.name(), reason.strip());
        return currentPolicy(project).orElseThrow();
    }

    public Evaluation record(UUID project, PolicyVersion policy, Status status, UUID actor,
                             int maxOverdueDays, Integer daysSinceRelevantUpdate, List<UUID> overdueActivityIds,
                             UUID latestUpdatedActivityId,
                             String reason, Instant now) {
        var sourceActivityIds = new java.util.LinkedHashSet<UUID>(overdueActivityIds);
        if (latestUpdatedActivityId != null) sourceActivityIds.add(latestUpdatedActivityId);
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into nite_connect.project_health_evaluations(
                    id,project_id,policy_version_id,status,evaluated_by,evaluated_at,
                    max_overdue_days,days_since_relevant_update,attention_overdue_days,critical_overdue_days,
                    attention_no_update_days,critical_no_update_days,source_activity_count,reason)
                values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, id, project, policy.id(), status.name(), actor, Timestamp.from(now), maxOverdueDays,
                daysSinceRelevantUpdate, policy.attentionOverdueDays(), policy.criticalOverdueDays(),
                policy.attentionNoUpdateDays(), policy.criticalNoUpdateDays(), sourceActivityIds.size(), reason);
        for (UUID activity : overdueActivityIds) {
            jdbc.update("insert into nite_connect.project_health_evaluation_sources(evaluation_id,activity_id,source_kind) values (?,?,?)",
                    id, activity, "ATRASO");
        }
        if (latestUpdatedActivityId != null) {
            jdbc.update("insert into nite_connect.project_health_evaluation_sources(evaluation_id,activity_id,source_kind) values (?,?,?)",
                    id, latestUpdatedActivityId, "ATUALIZACAO_RELEVANTE");
        }
        return new Evaluation(id, project, policy.version(), status, maxOverdueDays, daysSinceRelevantUpdate,
                sourceActivityIds.size(), reason, actor, now, List.copyOf(sourceActivityIds));
    }

    public PageResult<Evaluation> evaluations(UUID project, int page, int size) {
        PageResult.validate(page, size);
        long total = jdbc.queryForObject("select count(*) from nite_connect.project_health_evaluations where project_id=?", Long.class, project);
        var items = jdbc.query("""
                select e.*,p.version as policy_version from nite_connect.project_health_evaluations e
                join nite_connect.project_health_policy_versions p on p.id=e.policy_version_id
                where e.project_id=? order by e.evaluated_at desc,e.id limit ? offset ?
                """, (rs, row) -> new Evaluation(rs.getObject("id", UUID.class), rs.getObject("project_id", UUID.class),
                rs.getInt("policy_version"), Status.valueOf(rs.getString("status")), rs.getInt("max_overdue_days"),
                (Integer) rs.getObject("days_since_relevant_update"), rs.getInt("source_activity_count"), rs.getString("reason"),
                rs.getObject("evaluated_by", UUID.class), rs.getTimestamp("evaluated_at").toInstant(), List.of()), project, size, (long) page * size);
        var withSources = new ArrayList<Evaluation>(items.size());
        for (Evaluation item : items) {
            List<UUID> sources = jdbc.query("select distinct activity_id from nite_connect.project_health_evaluation_sources where evaluation_id=? order by activity_id",
                    (rs, row) -> rs.getObject("activity_id", UUID.class), item.id());
            withSources.add(new Evaluation(item.id(), item.projectId(), item.policyVersion(), item.status(), item.maxOverdueDays(),
                    item.daysSinceRelevantUpdate(), item.sourceActivityCount(), item.reason(), item.evaluatedBy(), item.evaluatedAt(), sources));
        }
        return new PageResult<>(withSources, page, size, total);
    }
}
