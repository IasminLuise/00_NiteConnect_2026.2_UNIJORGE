package br.edu.unijorge.niteconnect.attendance.infrastructure;

import br.edu.unijorge.niteconnect.attendance.api.ParticipationGovernanceContracts.*;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.Modality;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ParticipationGovernanceRepository {
    private final JdbcTemplate jdbc;
    public ParticipationGovernanceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static CalendarVersion calendar(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        Timestamp until = rs.getTimestamp("valid_until");
        return new CalendarVersion(rs.getObject("id", UUID.class), rs.getObject("week_start", LocalDate.class),
                rs.getObject("week_end", LocalDate.class), rs.getInt("version"),
                CalendarExceptionType.valueOf(rs.getString("exception_type")), rs.getBigDecimal("presential_hours_relieved"),
                rs.getBigDecimal("remote_hours_relieved"), rs.getObject("affected_event_id", UUID.class),
                rs.getObject("replacement_event_id", UUID.class), rs.getTimestamp("valid_from").toInstant(),
                until == null ? null : until.toInstant(), rs.getObject("created_by", UUID.class), rs.getString("reason"));
    }
    private static AdjustmentView adjustment(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new AdjustmentView(rs.getObject("id", UUID.class), rs.getObject("member_id", UUID.class),
                rs.getObject("source_week_start", LocalDate.class), rs.getObject("source_week_end", LocalDate.class),
                rs.getString("kind"), Modality.valueOf(rs.getString("requested_modality")), rs.getBigDecimal("requested_hours"),
                rs.getString("authorized_modality") == null ? null : Modality.valueOf(rs.getString("authorized_modality")),
                rs.getBigDecimal("authorized_hours"), rs.getObject("due_on", LocalDate.class), rs.getString("status"),
                rs.getObject("requested_by", UUID.class), rs.getTimestamp("requested_at").toInstant(),
                rs.getObject("decided_by", UUID.class), rs.getTimestamp("decided_at") == null ? null : rs.getTimestamp("decided_at").toInstant(),
                rs.getString("decision_reason"), rs.getString("reason"), rs.getLong("version"));
    }

    public Optional<CalendarVersion> currentCalendar(LocalDate from, LocalDate to) {
        return jdbc.query("select * from nite_connect.participation_calendar_versions where week_start=? and week_end=? and valid_until is null",
                ParticipationGovernanceRepository::calendar, from, to).stream().findFirst();
    }
    public int currentCalendarVersion(LocalDate from, LocalDate to) {
        return currentCalendar(from, to).map(CalendarVersion::version).orElse(0);
    }
    public boolean overlapsCalendar(LocalDate from, LocalDate to) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists(select 1 from nite_connect.participation_calendar_versions
                  where valid_until is null and week_start<=? and week_end>=?
                    and (week_start<>? or week_end<>?))
                """, Boolean.class, to, from, from, to));
    }
    public CalendarVersion saveCalendar(UUID actor, CalendarChange change, Instant now) {
        currentCalendar(change.weekStart(), change.weekEnd()).ifPresent(current -> jdbc.update(
                "update nite_connect.participation_calendar_versions set valid_until=? where id=? and valid_until is null",
                Timestamp.from(now), current.id()));
        UUID id = UUID.randomUUID();
        int version = change.expectedVersion() + 1;
        jdbc.update("""
                insert into nite_connect.participation_calendar_versions(
                 id,week_start,week_end,version,exception_type,presential_hours_relieved,remote_hours_relieved,
                 affected_event_id,replacement_event_id,valid_from,created_by,reason)
                values (?,?,?,?,?,?,?,?,?,?,?,?)
                """, id, change.weekStart(), change.weekEnd(), version, change.type().name(),
                change.presentialHoursRelieved(), change.remoteHoursRelieved(), change.affectedEventId(),
                change.replacementEventId(), Timestamp.from(now), actor, change.reason().strip());
        return currentCalendar(change.weekStart(), change.weekEnd()).orElseThrow();
    }
    public PageResult<CalendarVersion> calendarHistory(LocalDate from, LocalDate to, int page, int size) {
        PageResult.validate(page, size);
        List<Object> args = new ArrayList<>();
        String where = " from nite_connect.participation_calendar_versions where true";
        if (from != null) { where += " and week_end>=?"; args.add(from); }
        if (to != null) { where += " and week_start<=?"; args.add(to); }
        long total = jdbc.queryForObject("select count(*)" + where, Long.class, args.toArray());
        args.add(size); args.add((long) page * size);
        return new PageResult<>(jdbc.query("select *" + where + " order by week_start desc,version desc limit ? offset ?",
                ParticipationGovernanceRepository::calendar, args.toArray()), page, size, total);
    }

    public AdjustmentView requestReplacement(UUID member, ReplacementRequest request, Instant now) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into nite_connect.participation_adjustments(
                 id,member_id,source_week_start,source_week_end,kind,requested_modality,requested_hours,status,
                 requested_by,requested_at,reason)
                values (?,?,?,?,'REPOSICAO',?,?,'PENDENTE',?,?,?)
                """, id, member, request.weekStart(), request.weekEnd(), request.modality().name(),
                request.hours(), member, Timestamp.from(now), request.reason().strip());
        adjustmentEvent(id, member, "SOLICITADA", null, 0, request.reason());
        return adjustment(id);
    }
    public AdjustmentView grantDispensation(UUID actor, DispensationGrant grant, Instant now) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into nite_connect.participation_adjustments(
                 id,member_id,source_week_start,source_week_end,kind,requested_modality,requested_hours,
                 authorized_modality,authorized_hours,status,requested_by,requested_at,decided_by,decided_at,
                 decision_reason,reason)
                values (?,?,?,?,'DISPENSA',?,?,?,?,'AUTORIZADA',?,?,?, ?,?,?)
                """, id, grant.memberId(), grant.weekStart(), grant.weekEnd(), grant.modality().name(), grant.hours(),
                grant.modality().name(), grant.hours(), actor, Timestamp.from(now), actor, Timestamp.from(now),
                grant.reason().strip(), grant.reason().strip());
        adjustmentEvent(id, actor, "DISPENSA_AUTORIZADA", null, 0, grant.reason());
        return adjustment(id);
    }
    public AdjustmentView adjustment(UUID id) {
        return jdbc.query("select * from nite_connect.participation_adjustments where id=?",
                ParticipationGovernanceRepository::adjustment, id).stream().findFirst().orElseThrow(ApiException::notFound);
    }
    public AdjustmentView decideReplacement(UUID id, long expectedVersion, AdjustmentDecisionType decision,
                                            Modality modality, BigDecimal hours, LocalDate dueOn,
                                            UUID actor, String reason, Instant now) {
        AdjustmentView before = adjustment(id);
        String status = decision == AdjustmentDecisionType.ACEITAR ? "AUTORIZADA" : "RECUSADA";
        int changed;
        if (decision == AdjustmentDecisionType.ACEITAR) {
            changed = jdbc.update("""
                    update nite_connect.participation_adjustments set status=?,authorized_modality=?,authorized_hours=?,due_on=?,
                     decided_by=?,decided_at=?,decision_reason=?,version=version+1 where id=? and version=? and status='PENDENTE'
                    """, status, modality.name(), hours, dueOn, actor, Timestamp.from(now), reason.strip(), id, expectedVersion);
        } else {
            changed = jdbc.update("""
                    update nite_connect.participation_adjustments set status=?,decided_by=?,decided_at=?,decision_reason=?,
                     version=version+1 where id=? and version=? and status='PENDENTE'
                    """, status, actor, Timestamp.from(now), reason.strip(), id, expectedVersion);
        }
        if (changed != 1) throw ApiException.conflict("Solicitação alterada ou já decidida");
        adjustmentEvent(id, actor, "DECIDIDA", before.status(), expectedVersion + 1, reason);
        return adjustment(id);
    }
    private void adjustmentEvent(UUID id, UUID actor, String action, String before, long version, String reason) {
        jdbc.update("""
                insert into nite_connect.participation_adjustment_events(id,adjustment_id,actor_id,action,before_value,after_value,reason)
                values (?,?,?,?,?,?,?)
                """, UUID.randomUUID(), id, actor, action, before,
                "{\"version\":" + version + ",\"action\":\"" + action + "\"}", reason.strip());
    }
    public PageResult<AdjustmentView> adjustments(UUID member, int page, int size) {
        PageResult.validate(page, size);
        String where = member == null ? "" : " where member_id=?";
        Object[] countArgs = member == null ? new Object[0] : new Object[]{member};
        Object[] args = member == null ? new Object[]{size, (long) page * size} : new Object[]{member, size, (long) page * size};
        return new PageResult<>(jdbc.query("select * from nite_connect.participation_adjustments" + where
                + " order by requested_at desc,id limit ? offset ?", ParticipationGovernanceRepository::adjustment, args),
                page, size, jdbc.queryForObject("select count(*) from nite_connect.participation_adjustments" + where, Long.class, countArgs));
    }
    public BigDecimal dispensationHours(UUID member, LocalDate from, LocalDate to, Modality modality) {
        return jdbc.queryForObject("""
                select coalesce(sum(authorized_hours),0) from nite_connect.participation_adjustments
                where member_id=? and source_week_start=? and source_week_end=? and kind='DISPENSA'
                  and status='AUTORIZADA' and authorized_modality=?
                """, BigDecimal.class, member, from, to, modality.name());
    }
    public List<AdjustmentView> adjustmentsForWeek(UUID member, LocalDate from, LocalDate to) {
        return jdbc.query("""
                select * from nite_connect.participation_adjustments
                where member_id=? and source_week_start=? and source_week_end=?
                order by requested_at,id
                """, ParticipationGovernanceRepository::adjustment, member, from, to);
    }

    public void allocateReplacement(UUID adjustment, UUID period, UUID actor) {
        jdbc.update("insert into nite_connect.participation_replacement_allocations(id,adjustment_id,period_id,assigned_by) values (?,?,?,?)",
                UUID.randomUUID(), adjustment, period, actor);
    }
    public long allocatedSeconds(UUID adjustment) {
        return jdbc.queryForObject("""
                select coalesce(floor(sum(extract(epoch from (p.ends_at-p.starts_at)))),0)::bigint
                from nite_connect.participation_replacement_allocations a
                join nite_connect.participation_periods p on p.id=a.period_id where a.adjustment_id=?
                """, Long.class, adjustment);
    }
    public List<ReplacementAllocation> allocations(UUID adjustment) {
        return jdbc.query("select * from nite_connect.participation_replacement_allocations where adjustment_id=? order by assigned_at,id",
                (rs, row) -> new ReplacementAllocation(rs.getObject("adjustment_id", UUID.class), rs.getObject("period_id", UUID.class),
                        rs.getObject("assigned_by", UUID.class), rs.getTimestamp("assigned_at").toInstant()), adjustment);
    }

    public WeeklyReviewBatchResult createBatch(UUID id, LocalDate from, LocalDate to, UUID reviewer,
                                               String reason, Instant now, List<WeeklyReviewItemResult> items) {
        jdbc.update("insert into nite_connect.participation_review_batches(id,week_start,week_end,reviewed_by,reason,created_at) values (?,?,?,?,?,?)",
                id, from, to, reviewer, reason.strip(), Timestamp.from(now));
        for (WeeklyReviewItemResult item : items) {
            jdbc.update("""
                    insert into nite_connect.participation_review_batch_items(
                      id,batch_id,period_id,action,reviewer_reason,previous_version,resulting_version)
                    values (?,?,?,?,?,?,?)
                    """, UUID.randomUUID(), id, item.periodId(), item.action().name(), item.reason().strip(),
                    item.previousVersion(), item.resultingVersion());
        }
        return new WeeklyReviewBatchResult(id, from, to, reviewer, now, List.copyOf(items));
    }

    public PageResult<ReviewRequest> reviewRequests(UUID member, int page, int size) {
        PageResult.validate(page, size);
        String from = " from nite_connect.participation_review_batch_items i join nite_connect.participation_periods p on p.id=i.period_id join nite_connect.participation_review_batches b on b.id=i.batch_id where p.member_id=? and i.action='CORRECAO_SOLICITADA'";
        long total = jdbc.queryForObject("select count(*)" + from, Long.class, member);
        return new PageResult<>(jdbc.query("select i.id,i.period_id,b.week_start,b.week_end,i.reviewer_reason,i.created_at" + from
                + " order by i.created_at desc,i.id limit ? offset ?",
                (rs, row) -> new ReviewRequest(rs.getObject("id", UUID.class), rs.getObject("period_id", UUID.class),
                        rs.getObject("week_start", LocalDate.class), rs.getObject("week_end", LocalDate.class),
                        rs.getString("reviewer_reason"), rs.getTimestamp("created_at").toInstant()), member, size, (long) page * size),
                page, size, total);
    }
}
