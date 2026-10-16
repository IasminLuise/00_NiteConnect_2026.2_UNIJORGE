package br.edu.unijorge.niteconnect.attendance.infrastructure;

import br.edu.unijorge.niteconnect.attendance.domain.Participation.*;
import br.edu.unijorge.niteconnect.shared.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.Instant;
import java.util.*;

@Repository
public class AttendanceRepository {
    private final JdbcTemplate jdbc;
    public AttendanceRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static UUID uuid(ResultSet r,String n) throws SQLException { return r.getObject(n,UUID.class); }
    private static Instant instant(ResultSet r,String n) throws SQLException { Timestamp t=r.getTimestamp(n);return t==null?null:t.toInstant(); }
    public static final RowMapper<Event> EVENT=(r,i)->new Event(uuid(r,"id"),r.getString("title"),r.getString("description"),
            EventKind.valueOf(r.getString("kind")),Modality.valueOf(r.getString("modality")),instant(r,"starts_at"),instant(r,"ends_at"),EventStatus.valueOf(r.getString("status")),r.getLong("version"));
    public static final RowMapper<Period> PERIOD=(r,i)->new Period(uuid(r,"id"),uuid(r,"member_id"),uuid(r,"attendance_id"),PeriodKind.valueOf(r.getString("kind")),
            Modality.valueOf(r.getString("modality")),instant(r,"starts_at"),instant(r,"ends_at"),r.getString("description"),uuid(r,"activity_id"),uuid(r,"evidence_id"),r.getString("origin"),
            ReviewStatus.valueOf(r.getString("review_status")),uuid(r,"reviewed_by"),instant(r,"reviewed_at"),r.getString("review_reason"),r.getLong("version"));
    public static final RowMapper<Attendance> ATTENDANCE=(r,i)->new Attendance(uuid(r,"id"),uuid(r,"event_id"),uuid(r,"member_id"),r.getLong("version"));
    public static final RowMapper<Justification> JUSTIFICATION=(r,i)->new Justification(uuid(r,"id"),uuid(r,"member_id"),uuid(r,"event_id"),r.getString("reason"),r.getString("status"),
            r.getString("decision_reason"),uuid(r,"decided_by"),instant(r,"decided_at"),r.getLong("version"));
    public static final RowMapper<Correction> CORRECTION=(r,i)->new Correction(uuid(r,"id"),uuid(r,"member_id"),uuid(r,"period_id"),uuid(r,"event_id"),instant(r,"proposed_start"),instant(r,"proposed_end"),
            r.getString("reason"),r.getString("status"),r.getString("decision_reason"),uuid(r,"decided_by"),instant(r,"decided_at"),r.getLong("version"));
    public static final RowMapper<History> HISTORY=(r,i)->new History(uuid(r,"id"),r.getString("entity_type"),uuid(r,"entity_id"),uuid(r,"member_id"),uuid(r,"actor_id"),
            r.getString("action"),r.getLong("entity_version"),r.getString("before_value"),r.getString("after_value"),r.getString("reason"),instant(r,"occurred_at"));
    public Event event(UUID id) { return one("select * from nite_connect.calendar_events where id=?",EVENT,id); }
    public Period period(UUID id) { return one("select * from nite_connect.participation_periods where id=?",PERIOD,id); }
    public Attendance attendance(UUID id) { return one("select * from nite_connect.attendance_records where id=?",ATTENDANCE,id); }
    public Justification justification(UUID id) { return one("select * from nite_connect.absence_justifications where id=?",JUSTIFICATION,id); }
    public Correction correction(UUID id) { return one("select * from nite_connect.correction_requests where id=?",CORRECTION,id); }
    private <T> T one(String sql,RowMapper<T> mapper,Object... args) { return jdbc.query(sql,mapper,args).stream().findFirst().orElseThrow(ApiException::notFound); }
    public int update(String sql,Object... args) { return jdbc.update(sql,args); }
    public static Timestamp ts(Instant time) { return time==null?null:Timestamp.from(time); }
    public void changed(int count) { if(count!=1) throw ApiException.conflict("Registro alterado; consulte a versão atual"); }
    public void version(long actual,long expected) { if(actual!=expected) throw ApiException.conflict("Registro alterado; consulte a versão atual"); }
    public List<UUID> expected(UUID event) { return jdbc.query("select member_id from nite_connect.event_participants where event_id=? order by member_id",(r,i)->uuid(r,"member_id"),event); }
    public boolean expected(UUID event,UUID member) { return Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from nite_connect.event_participants where event_id=? and member_id=?)",Boolean.class,event,member)); }
    public Optional<Attendance> attendance(UUID event,UUID member) { return jdbc.query("select * from nite_connect.attendance_records where event_id=? and member_id=?",ATTENDANCE,event,member).stream().findFirst(); }
    public List<Period> periods(UUID attendance) { return jdbc.query("select * from nite_connect.participation_periods where attendance_id=? order by starts_at,id",PERIOD,attendance); }
    public Optional<Period> open(UUID member) { return jdbc.query("select * from nite_connect.participation_periods where member_id=? and ends_at is null",PERIOD,member).stream().findFirst(); }
    public void qr(UUID event,String hash,Instant now) {
        boolean valid=Boolean.TRUE.equals(jdbc.queryForObject("""
            select exists(select 1 from nite_connect.calendar_events where id=? and status='ABERTO' and qr_hash=?
             and qr_expires_at>? and starts_at<=? and ends_at>?)
            """,Boolean.class,event,hash,ts(now),ts(now),ts(now)));
        if(!valid) throw ApiException.conflict("Sessão de presença indisponível ou QR expirado");
    }
    public UUID createAttendance(UUID event,UUID member) {
        UUID id=UUID.randomUUID();update("insert into nite_connect.attendance_records(id,event_id,member_id) values (?,?,?)",id,event,member);return id;
    }
    public UUID createPeriod(UUID member,UUID attendance,PeriodKind kind,Modality modality,Instant start,Instant end,String description,UUID activity,UUID evidence,String origin) {
        UUID id=UUID.randomUUID();update("""
            insert into nite_connect.participation_periods(id,member_id,attendance_id,kind,modality,starts_at,ends_at,description,activity_id,evidence_id,origin)
            values (?,?,?,?,?,?,?,?,?,?,?)
            """,id,member,attendance,kind.name(),modality.name(),ts(start),ts(end),description,activity,evidence,origin);return id;
    }
    public void touchAttendance(UUID id,long version) { changed(update("update nite_connect.attendance_records set version=version+1 where id=? and version=?",id,version)); }
    public <T> PageResult<T> page(String columns,String from,List<Object> args,String order,RowMapper<T> mapper,int page,int size) {
        PageResult.validate(page,size);
        long total=jdbc.queryForObject("select count(*) "+from,Long.class,args.toArray());
        var paged=new ArrayList<>(args);paged.add(size);paged.add((long)page*size);
        return new PageResult<>(jdbc.query("select "+columns+" "+from+" order by "+order+" limit ? offset ?",mapper,paged.toArray()),page,size,total);
    }
    public List<Period> overlapping(UUID member,Instant from,Instant to) {
        return jdbc.query("""
            select p.* from nite_connect.participation_periods p
            where p.member_id=? and p.starts_at<? and (p.ends_at is null or p.ends_at>?)
            and (p.attendance_id is null or exists(select 1 from nite_connect.attendance_records a
             join nite_connect.calendar_events e on e.id=a.event_id where a.id=p.attendance_id and e.status<>'CANCELADO'))
            order by p.starts_at,p.id
            """,PERIOD,member,ts(to),ts(from));
    }
    public long count(String sql,Object... args) { return jdbc.queryForObject(sql,Long.class,args); }
}
