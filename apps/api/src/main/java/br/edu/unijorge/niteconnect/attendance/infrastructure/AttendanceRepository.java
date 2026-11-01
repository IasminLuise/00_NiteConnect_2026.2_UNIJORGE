package br.edu.unijorge.niteconnect.attendance.infrastructure;

import br.edu.unijorge.niteconnect.attendance.domain.Agenda.*;
import br.edu.unijorge.niteconnect.attendance.domain.Attendance.Period;
import br.edu.unijorge.niteconnect.attendance.domain.Attendance.Presence;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AttendanceRepository {
    private final JdbcTemplate jdbc;

    public AttendanceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private static UUID uuid(ResultSet result, String column) throws SQLException {
        return result.getObject(column, UUID.class);
    }

    private static Instant instant(ResultSet result, String column) throws SQLException {
        Timestamp value = result.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    public static final RowMapper<Event> EVENT = (r, i) -> new Event(uuid(r, "id"), r.getString("title"),
            r.getString("description"), EventKind.valueOf(r.getString("kind")), Modality.valueOf(r.getString("modality")),
            instant(r, "starts_at"), instant(r, "ends_at"), EventStatus.valueOf(r.getString("status")), r.getLong("version"));

    /** Reads only the presence fields needed from the mixed historical table. */
    public static final RowMapper<Period> PERIOD = (r, i) -> new Period(uuid(r, "id"), uuid(r, "member_id"),
            uuid(r, "attendance_id"), instant(r, "starts_at"), instant(r, "ends_at"), r.getLong("version"));

    public static final RowMapper<Presence> ATTENDANCE = (r, i) -> new Presence(uuid(r, "id"),
            uuid(r, "event_id"), uuid(r, "member_id"), r.getLong("version"));

    public static final RowMapper<ExpectedEvent> EXPECTED_EVENT = (r, i) -> new ExpectedEvent(uuid(r, "id"),
            r.getString("title"), r.getString("description"), EventKind.valueOf(r.getString("kind")),
            Modality.valueOf(r.getString("modality")), instant(r, "starts_at"), instant(r, "ends_at"),
            EventStatus.valueOf(r.getString("status")), r.getBoolean("expected"), r.getBoolean("checked_in"),
            uuid(r, "attendance_id"));

    public Event event(UUID id) { return one("select * from nite_connect.calendar_events where id=?", EVENT, id); }
    public Period period(UUID id) { return one("select * from nite_connect.participation_periods where id=? and attendance_id is not null and kind='ATTENDANCE'", PERIOD, id); }
    public Presence attendance(UUID id) { return one("select * from nite_connect.attendance_records where id=?", ATTENDANCE, id); }

    private <T> T one(String sql, RowMapper<T> mapper, Object... args) {
        return jdbc.query(sql, mapper, args).stream().findFirst().orElseThrow(ApiException::notFound);
    }

    public int update(String sql, Object... args) { return jdbc.update(sql, args); }
    public static Timestamp ts(Instant time) { return time == null ? null : Timestamp.from(time); }
    public void changed(int count) { if (count != 1) throw ApiException.conflict("Registro alterado; consulte a versão atual"); }
    public void version(long actual, long expected) { if (actual != expected) throw ApiException.conflict("Registro alterado; consulte a versão atual"); }

    public List<UUID> expected(UUID event) {
        return jdbc.query("select member_id from nite_connect.event_participants where event_id=? order by member_id",
                (r, i) -> uuid(r, "member_id"), event);
    }

    public PageResult<ExpectedEvent> expectedEvents(UUID member, Instant from, Instant to, int page, int size) {
        var args = new ArrayList<Object>();
        args.add(member);
        String where = "from nite_connect.event_participants ep join nite_connect.calendar_events e on e.id=ep.event_id " +
                "left join nite_connect.attendance_records a on a.event_id=e.id and a.member_id=ep.member_id where ep.member_id=?";
        if (from != null) { where += " and e.starts_at>=?"; args.add(ts(from)); }
        if (to != null) { where += " and e.starts_at<?"; args.add(ts(to)); }
        return page("e.id,e.title,e.description,e.kind,e.modality,e.starts_at,e.ends_at,e.status,true as expected,(a.id is not null) as checked_in,a.id as attendance_id",
                where, args, "e.starts_at,e.id", EXPECTED_EVENT, page, size);
    }

    public Optional<Presence> attendance(UUID event, UUID member) {
        return jdbc.query("select * from nite_connect.attendance_records where event_id=? and member_id=?", ATTENDANCE, event, member)
                .stream().findFirst();
    }

    public List<Period> periods(UUID attendance) {
        return jdbc.query("select * from nite_connect.participation_periods where attendance_id=? and kind='ATTENDANCE' order by starts_at,id",
                PERIOD, attendance);
    }

    public Optional<Period> open(UUID member) {
        return jdbc.query("select * from nite_connect.participation_periods where member_id=? and attendance_id is not null and kind='ATTENDANCE' and ends_at is null",
                PERIOD, member).stream().findFirst();
    }

    public void qr(UUID event, String hash, Instant now) {
        boolean valid = Boolean.TRUE.equals(jdbc.queryForObject("""
                select exists(select 1 from nite_connect.calendar_events where id=? and status='ABERTO' and qr_hash=?
                 and qr_expires_at>? and starts_at<=? and ends_at>?)
                """, Boolean.class, event, hash, ts(now), ts(now), ts(now)));
        if (!valid) throw ApiException.conflict("Sessão de presença indisponível ou QR expirado");
    }

    public UUID createAttendance(UUID event, UUID member) {
        UUID id = UUID.randomUUID();
        update("insert into nite_connect.attendance_records(id,event_id,member_id) values (?,?,?)", id, event, member);
        return id;
    }

    public UUID createPeriod(UUID member, UUID attendance, Modality modality, Instant start) {
        UUID id = UUID.randomUUID();
        update("""
                insert into nite_connect.participation_periods(id,member_id,attendance_id,kind,modality,starts_at,ends_at,description,activity_id,evidence_id,origin)
                values (?,?,?,?,?,?,?,?,?,?,?)
                """, id, member, attendance, "ATTENDANCE", modality.name(), ts(start), null, "", null, null, "QR");
        return id;
    }

    public void touchAttendance(UUID id, long version) {
        changed(update("update nite_connect.attendance_records set version=version+1 where id=? and version=?", id, version));
    }

    public <T> PageResult<T> page(String columns, String from, List<Object> args, String order,
                                  RowMapper<T> mapper, int page, int size) {
        PageResult.validate(page, size);
        long total = jdbc.queryForObject("select count(*) " + from, Long.class, args.toArray());
        var paged = new ArrayList<>(args);
        paged.add(size);
        paged.add((long) page * size);
        return new PageResult<>(jdbc.query("select " + columns + " " + from + " order by " + order + " limit ? offset ?",
                mapper, paged.toArray()), page, size, total);
    }

    public long count(String sql, Object... args) { return jdbc.queryForObject(sql, Long.class, args); }
}
