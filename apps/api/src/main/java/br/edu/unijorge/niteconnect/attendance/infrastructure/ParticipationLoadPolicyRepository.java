package br.edu.unijorge.niteconnect.attendance.infrastructure;

import br.edu.unijorge.niteconnect.attendance.api.ParticipationGovernanceContracts.LoadPolicyVersion;
import br.edu.unijorge.niteconnect.attendance.api.ParticipationGovernanceContracts.LoadPolicyAdministrationView;
import br.edu.unijorge.niteconnect.attendance.api.ParticipationGovernanceContracts.LoadPolicyEventView;
import br.edu.unijorge.niteconnect.attendance.api.ParticipationGovernanceContracts.LoadPolicyEffectiveWindow;
import br.edu.unijorge.niteconnect.attendance.api.ParticipationGovernanceContracts.PlannedLoadPolicy;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ParticipationLoadPolicyRepository {
    private final JdbcTemplate jdbc;

    public ParticipationLoadPolicyRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Draft(UUID id, int version, BigDecimal presentialHours, BigDecimal remoteHours,
                        LocalDate effectiveFrom, UUID createdBy, Instant createdAt,
                        String intendedSemester, LoadPolicyEffectiveWindow effectiveWindow,
                        String creationReason, UUID supersedesId) {}

    private static final org.springframework.jdbc.core.RowMapper<Draft> DRAFT = (rs, row) -> new Draft(
            rs.getObject("id", UUID.class), rs.getInt("version"), rs.getBigDecimal("presential_hours"),
            rs.getBigDecimal("remote_hours"), rs.getObject("effective_from", LocalDate.class),
            rs.getObject("created_by", UUID.class), rs.getTimestamp("created_at").toInstant(),
            rs.getString("intended_semester"), rs.getString("effective_window") == null ? null
                    : LoadPolicyEffectiveWindow.valueOf(rs.getString("effective_window")),
            rs.getString("creation_reason"), rs.getObject("supersedes_id", UUID.class));

    private static final org.springframework.jdbc.core.RowMapper<LoadPolicyEventView> EVENT = (rs, row) ->
            new LoadPolicyEventView(rs.getObject("id", UUID.class), rs.getObject("draft_id", UUID.class), rs.getString("event_type"),
                    rs.getObject("actor_id", UUID.class), rs.getTimestamp("occurred_at").toInstant(),
                    rs.getString("reason"), rs.getString("source_reference"));

    private static LoadPolicyVersion map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new LoadPolicyVersion(rs.getObject("id", java.util.UUID.class), rs.getInt("version"),
                rs.getObject("effective_from", LocalDate.class), rs.getObject("effective_until", LocalDate.class),
                rs.getBigDecimal("presential_hours"), rs.getBigDecimal("remote_hours"));
    }

    public List<LoadPolicyVersion> current(LocalDate date) {
        return jdbc.query("""
                select * from nite_connect.participation_load_policy_versions
                where effective_from<=? and (effective_until is null or effective_until>?)
                order by effective_from desc,version desc,id
                """, ParticipationLoadPolicyRepository::map, date, date);
    }

    public List<LoadPolicyVersion> overlapping(LocalDate from, LocalDate toExclusive) {
        return jdbc.query("""
                select * from nite_connect.participation_load_policy_versions
                where effective_from<? and (effective_until is null or effective_until>?)
                order by effective_from,version,id
                """, ParticipationLoadPolicyRepository::map, toExclusive, from);
    }

    public int nextVersion() {
        return jdbc.queryForObject("""
                select coalesce(max(version),0)+1 from (
                  select version from nite_connect.participation_load_policy_versions
                  union all
                  select version from nite_connect.participation_load_policy_drafts
                ) versions
                """, Integer.class);
    }

    public boolean hasActivatedPolicies() {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists(select 1 from nite_connect.participation_load_policy_versions)", Boolean.class));
    }

    public boolean hasUnfinishedDraft() {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists(
                  select 1 from nite_connect.participation_load_policy_drafts d
                  where (select event_type from nite_connect.participation_load_policy_events e
                         where e.draft_id=d.id order by e.sequence desc limit 1) in ('CRIADA','APROVADA')
                )
                """, Boolean.class));
    }

    public Draft draft(UUID id) {
        return jdbc.query("select * from nite_connect.participation_load_policy_drafts where id=?", DRAFT, id)
                .stream().findFirst().orElseThrow(ApiException::notFound);
    }

    public String status(UUID id) {
        return jdbc.query("""
                select event_type from nite_connect.participation_load_policy_events
                where draft_id=? order by sequence desc limit 1
                """, (rs, row) -> rs.getString(1), id).stream().findFirst().orElseThrow(ApiException::notFound);
    }

    public LoadPolicyAdministrationView administration(UUID id) {
        Draft draft = draft(id);
        return new LoadPolicyAdministrationView(draft.id(), draft.version(), draft.presentialHours(),
                draft.remoteHours(), draft.effectiveFrom(), draft.intendedSemester(), draft.effectiveWindow(),
                draft.createdBy(), draft.createdAt(),
                draft.creationReason(), status(id), events(id));
    }

    public Optional<PlannedLoadPolicy> planned(LocalDate today) {
        return jdbc.query("""
                select d.id,d.version,d.presential_hours,d.remote_hours,d.effective_from,
                       d.intended_semester,d.effective_window,e.event_type
                from nite_connect.participation_load_policy_drafts d
                join lateral (
                  select event_type from nite_connect.participation_load_policy_events e
                  where e.draft_id=d.id order by e.sequence desc limit 1
                ) e on true
                where e.event_type in ('APROVADA','ATIVADA')
                  and (d.effective_from is null or d.effective_from>?)
                order by d.version desc,d.id limit 1
                """, (rs, row) -> {
                    LocalDate effectiveFrom = rs.getObject("effective_from", LocalDate.class);
                    String eventStatus = rs.getString("event_type");
                    String status = effectiveFrom == null ? "SEM_VIGENCIA_DEFINIDA"
                            : eventStatus.equals("ATIVADA") ? "ATIVADA_PARA_FUTURO" : "AGUARDANDO_ATIVACAO";
                    return new PlannedLoadPolicy(rs.getObject("id", UUID.class), rs.getInt("version"),
                            rs.getBigDecimal("presential_hours"), rs.getBigDecimal("remote_hours"), effectiveFrom,
                            rs.getString("intended_semester"), rs.getString("effective_window") == null ? null
                                    : LoadPolicyEffectiveWindow.valueOf(rs.getString("effective_window")), status);
                }, today).stream().findFirst();
    }

    private List<LoadPolicyEventView> events(UUID id) {
        return jdbc.query("""
                select id,draft_id,event_type,actor_id,occurred_at,reason,source_reference
                from nite_connect.participation_load_policy_events
                where draft_id=? order by sequence
                """, EVENT, id);
    }

    public Optional<LoadPolicyEventView> approval(UUID id) {
        return jdbc.query("""
                select id,draft_id,event_type,actor_id,occurred_at,reason,source_reference
                from nite_connect.participation_load_policy_events
                where draft_id=? and event_type='APROVADA'
                """, EVENT, id).stream().findFirst();
    }

    public void createDraft(Draft draft) {
        jdbc.update("""
                insert into nite_connect.participation_load_policy_drafts(
                  id,version,presential_hours,remote_hours,effective_from,intended_semester,effective_window,
                  created_by,created_at,creation_reason,supersedes_id)
                values (?,?,?,?,?,?,?,?,?,?,?)
                """, draft.id(), draft.version(), draft.presentialHours(), draft.remoteHours(), draft.effectiveFrom(),
                draft.intendedSemester(), draft.effectiveWindow() == null ? null : draft.effectiveWindow().name(),
                draft.createdBy(), Timestamp.from(draft.createdAt()), draft.creationReason(), draft.supersedesId());
    }

    public void appendEvent(UUID id, UUID draftId, String type, UUID actor, Instant occurredAt,
                            String reason, String sourceReference) {
        jdbc.update("""
                insert into nite_connect.participation_load_policy_events(
                  id,draft_id,event_type,actor_id,occurred_at,reason,source_reference)
                values (?,?,?,?,?,?,?)
                """, id, draftId, type, actor, Timestamp.from(occurredAt), reason, sourceReference);
    }

    public Optional<LoadPolicyVersion> latestActivated() {
        return jdbc.query("""
                select * from nite_connect.participation_load_policy_versions
                order by effective_from desc,version desc,id limit 1
                """, ParticipationLoadPolicyRepository::map).stream().findFirst();
    }

    public void closeVersion(UUID id, LocalDate effectiveUntil) {
        int changed = jdbc.update("""
                update nite_connect.participation_load_policy_versions
                set effective_until=? where id=? and effective_until is null
                """, effectiveUntil, id);
        if (changed != 1) throw ApiException.conflict("A vigência da política foi alterada; consulte o histórico atual");
    }

    public void activate(Draft draft, LoadPolicyEventView approval, Instant activatedAt) {
        jdbc.update("""
                insert into nite_connect.participation_load_policy_versions(
                  id,version,effective_from,effective_until,presential_hours,remote_hours,source_reference,
                  approved_by,approved_at,created_at)
                values (?,?,?,null,?,?,?,?,?,?)
                """, draft.id(), draft.version(), draft.effectiveFrom(), draft.presentialHours(), draft.remoteHours(),
                approval.sourceReference(), approval.actorId(), Timestamp.from(approval.occurredAt()), Timestamp.from(activatedAt));
    }

    public PageResult<LoadPolicyAdministrationView> history(int page, int size) {
        PageResult.validate(page, size);
        long total = jdbc.queryForObject("select count(*) from nite_connect.participation_load_policy_drafts", Long.class);
        List<Object> args = List.of(size, (long) page * size);
        List<LoadPolicyAdministrationView> rows = jdbc.query("""
                select d.*,
                  (select event_type from nite_connect.participation_load_policy_events e
                   where e.draft_id=d.id order by e.sequence desc limit 1) as status
                from nite_connect.participation_load_policy_drafts d
                order by d.version desc,d.id limit ? offset ?
                """, (rs, row) -> new LoadPolicyAdministrationView(
                rs.getObject("id", UUID.class), rs.getInt("version"), rs.getBigDecimal("presential_hours"),
                rs.getBigDecimal("remote_hours"), rs.getObject("effective_from", LocalDate.class),
                rs.getString("intended_semester"), rs.getString("effective_window") == null ? null
                        : LoadPolicyEffectiveWindow.valueOf(rs.getString("effective_window")),
                rs.getObject("created_by", UUID.class), rs.getTimestamp("created_at").toInstant(),
                rs.getString("creation_reason"), rs.getString("status"), List.of()), args.toArray());
        if (rows.isEmpty()) return new PageResult<>(rows, page, size, total);

        String placeholders = String.join(",", java.util.Collections.nCopies(rows.size(), "?"));
        Object[] ids = rows.stream().map(LoadPolicyAdministrationView::id).toArray();
        Map<UUID, List<LoadPolicyEventView>> events = new HashMap<>();
        jdbc.query("""
                select id,draft_id,event_type,actor_id,occurred_at,reason,source_reference
                from nite_connect.participation_load_policy_events
                where draft_id in (%s) order by draft_id,sequence
                """.formatted(placeholders), EVENT, ids).forEach(event ->
                events.computeIfAbsent(event.draftId(), ignored -> new ArrayList<>()).add(event));
        List<LoadPolicyAdministrationView> withEvents = rows.stream().map(row -> new LoadPolicyAdministrationView(
                row.id(), row.version(), row.presentialHours(), row.remoteHours(), row.effectiveFrom(),
                row.intendedSemester(), row.effectiveWindow(),
                row.createdBy(), row.createdAt(), row.creationReason(), row.status(),
                List.copyOf(events.getOrDefault(row.id(), List.of())))).toList();
        return new PageResult<>(withEvents, page, size, total);
    }

}
