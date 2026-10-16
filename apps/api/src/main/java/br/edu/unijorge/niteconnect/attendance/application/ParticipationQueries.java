package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.*;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.Period;
import br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository;
import br.edu.unijorge.niteconnect.shared.api.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository.ts;

@Service
@Transactional(readOnly=true)
public class ParticipationQueries {
    private final AttendanceRepository repo; private final AccessService access; private final ParticipationService participation; private final Clock clock;
    public ParticipationQueries(AttendanceRepository repo,AccessService access,ParticipationService participation,Clock clock) { this.repo=repo;this.access=access;this.participation=participation;this.clock=clock; }
    public record Range(Instant from,Instant to) {}
    public record Summary(String zone,Instant from,Instant to,long registeredSeconds,long validatedSeconds,long pendingSeconds,
                          long refusedSeconds,long openPeriods,long pendingRecords,long expectedMeetings,long attendedMeetings,long absentMeetings) {}
    public Range range(LocalDate from,LocalDate to) {
        if(from==null || to==null || from.isAfter(to) || to.equals(LocalDate.MAX)) throw ApiException.invalid("Informe from/to em ordem válida");
        return new Range(from.atStartOfDay(clock.getZone()).toInstant(),to.plusDays(1).atStartOfDay(clock.getZone()).toInstant());
    }
    public PageResult<AttendanceView> attendance(UUID actor,LocalDate from,LocalDate to,int page,int size) {
        access.requireActive(actor);var args=new ArrayList<Object>();args.add(actor);
        String where="from nite_connect.attendance_records a where a.member_id=?";
        if(from!=null || to!=null) { Range r=range(from,to);where+=" and exists(select 1 from nite_connect.participation_periods p where p.attendance_id=a.id and p.starts_at<? and (p.ends_at is null or p.ends_at>?))";args.add(ts(r.to()));args.add(ts(r.from())); }
        var result=repo.page("a.*",where,args,"a.created_at desc,a.id",AttendanceRepository.ATTENDANCE,page,size);
        return new PageResult<>(result.items().stream().map(participation::view).toList(),page,size,result.total());
    }
    public PageResult<Period> periods(UUID actor,boolean admin,UUID member,PeriodKind kind,LocalDate from,LocalDate to,int page,int size) {
        access.requireActive(actor);if(admin) access.requireAdministrator(actor);else member=actor;
        var args=new ArrayList<Object>();String where="from nite_connect.participation_periods p where true";
        if(member!=null) { where+=" and p.member_id=?";args.add(member); }
        if(kind!=null) { where+=" and p.kind=?";args.add(kind.name()); }
        if(from!=null || to!=null) { Range r=range(from,to);where+=" and p.starts_at<? and (p.ends_at is null or p.ends_at>?)";args.add(ts(r.to()));args.add(ts(r.from())); }
        return repo.page("p.*",where,args,"p.starts_at desc,p.id",AttendanceRepository.PERIOD,page,size);
    }
    public PageResult<Justification> justifications(UUID actor,boolean admin,UUID member,int page,int size) {
        access.requireActive(actor);if(admin) access.requireAdministrator(actor);else member=actor;
        String where="from nite_connect.absence_justifications"+(member==null?"":" where member_id=?");
        return repo.page("*",where,member==null?List.of():List.of(member),"created_at desc,id",AttendanceRepository.JUSTIFICATION,page,size);
    }
    public PageResult<Correction> corrections(UUID actor,boolean admin,UUID member,int page,int size) {
        access.requireActive(actor);if(admin) access.requireAdministrator(actor);else member=actor;
        return repo.page("*","from nite_connect.correction_requests"+(member==null?"":" where member_id=?"),member==null?List.of():List.of(member),"created_at desc,id",AttendanceRepository.CORRECTION,page,size);
    }
    public PageResult<History> history(UUID actor,boolean admin,UUID member,LocalDate from,LocalDate to,int page,int size) {
        access.requireActive(actor);if(admin) access.requireAdministrator(actor);else member=actor;
        String where="from nite_connect.participation_events where true";var args=new ArrayList<Object>();
        if(member!=null) { where+=" and member_id=?";args.add(member); }
        if(from!=null || to!=null) { Range r=range(from,to);where+=" and occurred_at>=? and occurred_at<?";args.add(ts(r.from()));args.add(ts(r.to())); }
        return repo.page("*",where,args,"occurred_at desc,id",AttendanceRepository.HISTORY,page,size);
    }
    public Summary summary(UUID actor,UUID member,boolean admin,LocalDate from,LocalDate to) {
        access.requireActive(actor);if(admin) access.requireAdministrator(actor);else member=actor;
        if(member==null) throw ApiException.invalid("memberId obrigatório");Range r=range(from,to);
        String eligible="""
             from nite_connect.participation_periods p where p.member_id=? and p.starts_at<? and (p.ends_at is null or p.ends_at>?)
             and (p.attendance_id is null or exists(select 1 from nite_connect.attendance_records a
              join nite_connect.calendar_events e on e.id=a.event_id where a.id=p.attendance_id and e.status<>'CANCELADO'))
            """;
        String duration="coalesce(floor(sum(extract(epoch from (least(p.ends_at,?)-greatest(p.starts_at,?))))),0)";
        long registered=repo.count("select "+duration+eligible+" and p.ends_at is not null",ts(r.to()),ts(r.from()),member,ts(r.to()),ts(r.from()));
        long validated=seconds(eligible,duration,member,r,"APROVADA"),pending=seconds(eligible,duration,member,r,"PENDENTE"),refused=seconds(eligible,duration,member,r,"RECUSADA");
        long open=repo.count("select count(*)"+eligible+" and p.ends_at is null",member,ts(r.to()),ts(r.from()));
        long pendingRecords=repo.count("select count(*)"+eligible+" and p.review_status='PENDENTE'",member,ts(r.to()),ts(r.from()));
        String meetings="""
             from nite_connect.calendar_events e join nite_connect.event_participants ep on ep.event_id=e.id
             where ep.member_id=? and e.status='ENCERRADO' and e.starts_at>=? and e.starts_at<?
            """;
        long expected=repo.count("select count(*)"+meetings,member,ts(r.from()),ts(r.to()));
        long attended=repo.count("select count(*)"+meetings+" and exists(select 1 from nite_connect.attendance_records a where a.event_id=e.id and a.member_id=ep.member_id)",member,ts(r.from()),ts(r.to()));
        return new Summary(clock.getZone().getId(),r.from(),r.to(),registered,validated,pending,refused,open,pendingRecords,expected,attended,expected-attended);
    }
    private long seconds(String eligible,String duration,UUID member,Range r,String status) {
        return repo.count("select "+duration+eligible+" and p.ends_at is not null and p.review_status=?",ts(r.to()),ts(r.from()),member,ts(r.to()),ts(r.from()),status);
    }
}
