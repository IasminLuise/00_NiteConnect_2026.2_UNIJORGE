package br.edu.unijorge.niteconnect.audit.application;

import br.edu.unijorge.niteconnect.shared.api.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditTrail {
    private final JdbcTemplate jdbc;
    public AuditTrail(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID actor, UUID member, String action, String before, String after, String reason) {
        recordEntity(actor, "MEMBER", member, action, before, after, reason);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void recordEntity(UUID actor, String entityType, UUID entityId, String action, String before, String after, String reason) {
        jdbc.update("""
                insert into nite_connect.audit_events(id,actor_id,member_id,entity_type,entity_id,action,before_value,after_value,reason)
                values (?,?,?,?,?,?,?,?,?)
                """, UUID.randomUUID(), actor, "MEMBER".equals(entityType) ? entityId : null,
                entityType, entityId, action, before, after, reason);
    }

    public PageResult<Event> list(int page, int size) {
        PageResult.validate(page, size);
        return new PageResult<>(jdbc.query("select * from nite_connect.audit_events order by occurred_at desc,id limit ? offset ?",
                (rs, row) -> new Event(rs.getObject("id", UUID.class), rs.getObject("actor_id", UUID.class), rs.getObject("member_id", UUID.class),
                        rs.getString("action"), rs.getString("before_value"), rs.getString("after_value"), rs.getString("reason"),
                        rs.getTimestamp("occurred_at").toInstant(), rs.getString("entity_type"),
                        rs.getObject("entity_id", UUID.class) == null ? rs.getObject("member_id", UUID.class) : rs.getObject("entity_id", UUID.class)), size, (long) page * size),
                page, size, jdbc.queryForObject("select count(*) from nite_connect.audit_events", Long.class));
    }
    public record Event(UUID id, UUID actorId, UUID memberId, String action, String beforeValue, String afterValue,
                        String reason, Instant occurredAt, String entityType, UUID entityId) {}
}
