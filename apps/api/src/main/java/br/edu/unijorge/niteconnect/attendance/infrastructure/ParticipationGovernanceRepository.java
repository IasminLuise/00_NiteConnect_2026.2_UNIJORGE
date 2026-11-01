package br.edu.unijorge.niteconnect.attendance.infrastructure;

import br.edu.unijorge.niteconnect.attendance.api.ParticipationGovernanceContracts.*;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.Modality;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.PeriodKind;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

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
    private static PersonalCalendarVersion personalCalendar(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        Timestamp until=rs.getTimestamp("valid_until");
        return new PersonalCalendarVersion(rs.getObject("id",UUID.class),rs.getObject("week_start",LocalDate.class),
                rs.getObject("week_end",LocalDate.class),rs.getInt("version"),CalendarExceptionType.valueOf(rs.getString("exception_type")),
                rs.getBigDecimal("presential_hours_relieved"),rs.getBigDecimal("remote_hours_relieved"),
                rs.getObject("affected_event_id",UUID.class),rs.getString("affected_event_title"),
                rs.getObject("replacement_event_id",UUID.class),rs.getString("replacement_event_title"),
                rs.getTimestamp("valid_from").toInstant(),until==null?null:until.toInstant());
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
    public Optional<PersonalCalendarVersion> currentPersonalCalendar(LocalDate from,LocalDate to) {
        return jdbc.query(personalCalendarSelect()+" where c.week_start=? and c.week_end=? and c.valid_until is null order by c.version desc limit 1",
                ParticipationGovernanceRepository::personalCalendar,from,to).stream().findFirst();
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
    public PageResult<PersonalCalendarVersion> personalCalendarHistory(LocalDate from,LocalDate to,int page,int size) {
        PageResult.validate(page,size);
        String fromSql=" from nite_connect.participation_calendar_versions c left join nite_connect.calendar_events a on a.id=c.affected_event_id left join nite_connect.calendar_events r on r.id=c.replacement_event_id";
        String where=" where true";
        var args=new ArrayList<Object>();
        if(from!=null){where+=" and c.week_end>=?";args.add(from);}
        if(to!=null){where+=" and c.week_start<=?";args.add(to);}
        long total=jdbc.queryForObject("select count(*)"+fromSql+where,Long.class,args.toArray());
        var pageArgs=new ArrayList<>(args);pageArgs.add(size);pageArgs.add((long)page*size);
        return new PageResult<>(jdbc.query(personalCalendarSelect()+where+" order by c.week_start desc,c.version desc,c.id limit ? offset ?",
                ParticipationGovernanceRepository::personalCalendar,pageArgs.toArray()),page,size,total);
    }
    private static String personalCalendarSelect() {
        return "select c.id,c.week_start,c.week_end,c.version,c.exception_type,c.presential_hours_relieved,c.remote_hours_relieved, "+
                "c.affected_event_id,a.title affected_event_title,c.replacement_event_id,r.title replacement_event_title,c.valid_from,c.valid_until "+
                "from nite_connect.participation_calendar_versions c left join nite_connect.calendar_events a on a.id=c.affected_event_id "+
                "left join nite_connect.calendar_events r on r.id=c.replacement_event_id";
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

    public PageResult<ReplacementView> replacements(UUID member,int page,int size) {
        PageResult.validate(page,size);
        long total=jdbc.queryForObject("select count(*) from nite_connect.participation_adjustments where member_id=? and kind='REPOSICAO'",Long.class,member);
        List<AdjustmentView> adjustments=jdbc.query("select * from nite_connect.participation_adjustments where member_id=? and kind='REPOSICAO' order by requested_at desc,id limit ? offset ?",
                ParticipationGovernanceRepository::adjustment,member,size,(long)page*size);
        if(adjustments.isEmpty()) return new PageResult<>(List.of(),page,size,total);
        List<UUID> ids=adjustments.stream().map(AdjustmentView::id).toList();
        String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
        String sql="""
                select x.adjustment_id,p.id period_id,p.kind,p.modality,p.review_status,p.starts_at,p.ends_at,e.title event_title,
                       p.description,case when p.review_status='APROVADA'
                         then floor(extract(epoch from (p.ends_at-p.starts_at)))::bigint else 0 end recognized_seconds,x.assigned_at
                from nite_connect.participation_replacement_allocations x
                join nite_connect.participation_periods p on p.id=x.period_id
                left join nite_connect.attendance_records a on a.id=p.attendance_id
                left join nite_connect.calendar_events e on e.id=a.event_id
                where x.adjustment_id in (
                """+placeholders+") order by x.adjustment_id,x.assigned_at,x.period_id";
        var periods=jdbc.query(sql,(rs,row)->new AbstractMap.SimpleImmutableEntry<>(rs.getObject("adjustment_id",UUID.class),
                new ReplacementPeriod(rs.getObject("period_id",UUID.class),PeriodKind.valueOf(rs.getString("kind")),
                        Modality.valueOf(rs.getString("modality")),rs.getString("review_status"),rs.getTimestamp("starts_at").toInstant(),
                        rs.getTimestamp("ends_at").toInstant(),rs.getString("event_title"),rs.getString("description"),
                        rs.getLong("recognized_seconds"),rs.getTimestamp("assigned_at").toInstant())),ids.toArray());
        Map<UUID,List<ReplacementPeriod>> byAdjustment=new HashMap<>();
        for(var item:periods) byAdjustment.computeIfAbsent(item.getKey(),ignored->new ArrayList<>()).add(item.getValue());
        List<ReplacementView> items=adjustments.stream().map(a->{
            List<ReplacementPeriod> assigned=List.copyOf(byAdjustment.getOrDefault(a.id(),List.of()));
            long recognized=assigned.stream().mapToLong(ReplacementPeriod::recognizedSeconds).reduce(0,Math::addExact);
            Long remaining=null;
            if("AUTORIZADA".equals(a.status())) {
                long authorized=a.authorizedHours().multiply(BigDecimal.valueOf(3600)).longValueExact();
                remaining=Math.max(0,authorized-recognized);
            }
            return new ReplacementView(a,assigned,recognized,remaining);
        }).toList();
        return new PageResult<>(items,page,size,total);
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
                  and p.review_status='APROVADA'
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
        long total=jdbc.queryForObject("select count(*) from nite_connect.participation_review_batch_items i join nite_connect.participation_periods p on p.id=i.period_id where p.member_id=? and i.action='CORRECAO_SOLICITADA'",Long.class,member);
        return new PageResult<>(jdbc.query(reviewRequestSelect()+" where p.member_id=? and i.action='CORRECAO_SOLICITADA' order by i.created_at desc,i.id limit ? offset ?",
                ParticipationGovernanceRepository::reviewRequest,member,size,(long)page*size),page,size,total);
    }

    public PageResult<AdministrativeReviewRequest> reviewRequestsAdmin(UUID member,int page,int size) {
        PageResult.validate(page,size);
        String from=" from nite_connect.participation_review_batch_items i join nite_connect.participation_periods p on p.id=i.period_id "+
                "join nite_connect.members m on m.id=p.member_id join nite_connect.participation_review_batches b on b.id=i.batch_id "+
                "left join nite_connect.work_log_correction_responses r on r.review_request_id=i.id "+
                "left join nite_connect.work_log_correction_decisions d on d.response_id=r.id where i.action='CORRECAO_SOLICITADA'";
        var args=new ArrayList<Object>();
        if(member!=null){from+=" and p.member_id=?";args.add(member);}
        long total=jdbc.queryForObject("select count(*)"+from,Long.class,args.toArray());
        var pageArgs=new ArrayList<>(args);pageArgs.add(size);pageArgs.add((long)page*size);
        return new PageResult<>(jdbc.query("select p.member_id,m.name member_name,i.id,i.period_id,b.week_start,b.week_end,i.reviewer_reason,i.created_at, "+
                        "p.starts_at current_start,p.ends_at current_end,p.description current_description,p.modality current_modality, "+
                        "p.activity_id current_activity_id,p.evidence_id current_evidence_id,p.review_status current_review_status,p.version current_version, "+
                        "r.id response_id,r.proposed_start,r.proposed_end,r.proposed_description,r.proposed_activity_id,r.proposed_evidence_id, "+
                        "r.reason response_reason,r.responded_at,r.expected_period_version,r.version response_version,d.decision,d.reason decision_reason,d.decided_at"+from+
                        " order by i.created_at desc,i.id limit ? offset ?",
                (rs,row)->new AdministrativeReviewRequest(rs.getObject("member_id",UUID.class),rs.getString("member_name"),reviewRequest(rs,row)),pageArgs.toArray()),
                page,size,total);
    }

    public ReviewRequest reviewRequest(UUID member,UUID requestId) {
        return jdbc.query(reviewRequestSelect()+" where p.member_id=? and i.id=? and i.action='CORRECAO_SOLICITADA'",
                ParticipationGovernanceRepository::reviewRequest,member,requestId).stream().findFirst().orElseThrow(ApiException::notFound);
    }

    public ReviewRequest reviewRequest(UUID requestId) {
        return jdbc.query(reviewRequestSelect()+" where i.id=? and i.action='CORRECAO_SOLICITADA'",
                ParticipationGovernanceRepository::reviewRequest,requestId).stream().findFirst().orElseThrow(ApiException::notFound);
    }

    public void createWorkLogCorrectionResponse(UUID requestId,UUID periodId,UUID memberId,
                                                SubmitWorkLogCorrection response,Instant now) {
        jdbc.update("""
                insert into nite_connect.work_log_correction_responses(
                  id,review_request_id,period_id,member_id,expected_period_version,proposed_start,proposed_end,
                  proposed_description,proposed_activity_id,proposed_evidence_id,reason,responded_at)
                values (?,?,?,?,?,?,?,?,?,?,?,?)
                """,UUID.randomUUID(),requestId,periodId,memberId,response.expectedPeriodVersion(),
                Timestamp.from(response.startsAt()),Timestamp.from(response.endsAt()),response.description().strip(),
                response.activityId(),response.evidenceId(),response.reason().strip(),Timestamp.from(now));
    }

    public void createWorkLogCorrectionDecision(UUID responseId,WorkLogCorrectionDecisionType decision,
                                                long expectedResponseVersion,Long resultingPeriodVersion,
                                                UUID actor,String reason,Instant now) {
        jdbc.update("""
                insert into nite_connect.work_log_correction_decisions(
                  id,response_id,decision,expected_response_version,resulting_period_version,decided_by,decided_at,reason)
                values (?,?,?,?,?,?,?,?)
                """,UUID.randomUUID(),responseId,decision.name(),expectedResponseVersion,resultingPeriodVersion,
                actor,Timestamp.from(now),reason.strip());
    }

    private static String reviewRequestSelect() {
        return """
                select p.member_id,m.name member_name,i.id,i.period_id,b.week_start,b.week_end,i.reviewer_reason,i.created_at,
                       p.starts_at current_start,p.ends_at current_end,p.description current_description,p.modality current_modality,
                       p.activity_id current_activity_id,p.evidence_id current_evidence_id,p.review_status current_review_status,p.version current_version,
                       r.id response_id,r.proposed_start,r.proposed_end,r.proposed_description,r.proposed_activity_id,r.proposed_evidence_id,
                       r.reason response_reason,r.responded_at,r.expected_period_version,r.version response_version,
                       d.decision,d.reason decision_reason,d.decided_at
                from nite_connect.participation_review_batch_items i
                join nite_connect.participation_periods p on p.id=i.period_id
                join nite_connect.members m on m.id=p.member_id
                join nite_connect.participation_review_batches b on b.id=i.batch_id
                left join nite_connect.work_log_correction_responses r on r.review_request_id=i.id
                left join nite_connect.work_log_correction_decisions d on d.response_id=r.id
                """;
    }

    private static ReviewRequest reviewRequest(java.sql.ResultSet rs,int row) throws java.sql.SQLException {
        UUID periodId=rs.getObject("period_id",UUID.class);
        UUID responseId=rs.getObject("response_id",UUID.class);
        String decisionValue=rs.getString("decision");
        WorkLogCorrectionResponseView response=null;
        String status="AGUARDANDO_RESPOSTA";
        if(responseId!=null) {
            WorkLogCorrectionDecisionType decision=decisionValue==null?null:WorkLogCorrectionDecisionType.valueOf(decisionValue);
            response=new WorkLogCorrectionResponseView(responseId,rs.getTimestamp("proposed_start").toInstant(),
                    rs.getTimestamp("proposed_end").toInstant(),rs.getString("proposed_description"),
                    rs.getObject("proposed_activity_id",UUID.class),rs.getObject("proposed_evidence_id",UUID.class),
                    rs.getString("response_reason"),rs.getTimestamp("responded_at").toInstant(),
                    rs.getLong("expected_period_version"),rs.getLong("response_version"),decision,
                    rs.getString("decision_reason"),rs.getTimestamp("decided_at")==null?null:rs.getTimestamp("decided_at").toInstant());
            status=decision==null?"EM_REVISAO":decision==WorkLogCorrectionDecisionType.ACEITA?"CORRECAO_ACEITA":"CORRECAO_RECUSADA";
        }
        WorkLogSnapshot current=new WorkLogSnapshot(periodId,rs.getTimestamp("current_start").toInstant(),
                rs.getTimestamp("current_end").toInstant(),rs.getString("current_description"),
                Modality.valueOf(rs.getString("current_modality")),rs.getObject("current_activity_id",UUID.class),
                rs.getObject("current_evidence_id",UUID.class),rs.getString("current_review_status"),rs.getLong("current_version"));
        return new ReviewRequest(rs.getObject("id",UUID.class),periodId,rs.getObject("week_start",LocalDate.class),
                rs.getObject("week_end",LocalDate.class),rs.getString("reviewer_reason"),rs.getTimestamp("created_at").toInstant(),status,current,response);
    }
}
