package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.attendance.domain.Attendance.View;
import br.edu.unijorge.niteconnect.attendance.domain.Agenda.ExpectedEvent;
import br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository;
import br.edu.unijorge.niteconnect.attendance.infrastructure.FrequencyRepository;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.api.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import static br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository.ts;

@Service
@Transactional(readOnly = true)
public class AttendanceQueries {
    private final AttendanceRepository repo;
    private final AccessService access;
    private final AttendanceService attendance;
    private final FrequencyRepository frequency;
    private final Clock clock;

    public AttendanceQueries(AttendanceRepository repo, AccessService access, AttendanceService attendance,
                             FrequencyRepository frequency, Clock clock) {
        this.repo = repo;
        this.access = access;
        this.attendance = attendance;
        this.frequency = frequency;
        this.clock = clock;
    }

    public PageResult<View> attendance(UUID actor, LocalDate from, LocalDate to, int page, int size) {
        access.requireActive(actor);
        var args = new ArrayList<Object>();
        args.add(actor);
        String where = "from nite_connect.attendance_records a where a.member_id=? and a.created_at<?";
        args.add(ts(frequency.activatedAt()));
        if (from != null || to != null) {
            Range range = range(from, to);
            where += " and exists(select 1 from nite_connect.attendance_segments p where p.attendance_id=a.id and p.starts_at<? and (p.ends_at is null or p.ends_at>?))";
            args.add(ts(range.to()));
            args.add(ts(range.from()));
        }
        var result = repo.page("a.*", where, args, "a.created_at desc,a.id", AttendanceRepository.ATTENDANCE, page, size);
        return new PageResult<>(result.items().stream().map(attendance::view).toList(), page, size, result.total());
    }

    public PageResult<ExpectedEvent> expectedEvents(UUID actor, LocalDate from, LocalDate to, int page, int size) {
        access.requireActive(actor);
        if ((from == null) != (to == null)) throw ApiException.invalid("Informe from e to juntos");
        Range range = from == null ? null : range(from, to);
        return repo.expectedEvents(actor, range == null ? null : range.from(), range == null ? null : range.to(), page, size);
    }

    private Range range(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to) || to.equals(LocalDate.MAX)) {
            throw ApiException.invalid("Informe from/to em ordem válida");
        }
        return new Range(from.atStartOfDay(clock.getZone()).toInstant(), to.plusDays(1).atStartOfDay(clock.getZone()).toInstant());
    }

    private record Range(java.time.Instant from, java.time.Instant to) {}
}
