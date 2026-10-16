package br.edu.unijorge.niteconnect.activities.application;

import br.edu.unijorge.niteconnect.activities.infrastructure.ActivityRepository;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

/** Explicit read contract for participation declarations; never grants access to activity internals. */
@Service
public class ActivityParticipation {
    private final ActivityRepository activities; private final ActivityPolicy policy; private final JdbcTemplate jdbc;
    public ActivityParticipation(ActivityRepository activities,ActivityPolicy policy,JdbcTemplate jdbc) { this.activities=activities;this.policy=policy;this.jdbc=jdbc; }
    public void requireOwnContext(UUID actor,UUID activityId,UUID evidenceId) {
        if(activityId==null) { if(evidenceId!=null) throw ApiException.invalid("Evidência exige atividade");return; }
        var activity=activities.require(activityId);policy.read(actor,activity);
        if(activities.assigned(activityId,actor).isEmpty()) throw ApiException.forbidden();
        if(evidenceId!=null && !Boolean.TRUE.equals(jdbc.queryForObject("""
            select exists(select 1 from nite_connect.activity_evidence where id=? and activity_id=? and author_id=? and withdrawn_at is null)
            """,Boolean.class,evidenceId,activityId,actor))) throw ApiException.notFound();
    }
}
