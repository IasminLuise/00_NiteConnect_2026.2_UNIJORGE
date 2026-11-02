package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.*;
import br.edu.unijorge.niteconnect.attendance.domain.Agenda.*;
import br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository;
import br.edu.unijorge.niteconnect.members.application.MemberDirectory;
import br.edu.unijorge.niteconnect.shared.api.*;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import static br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository.ts;
import static br.edu.unijorge.niteconnect.attendance.application.AgendaRules.*;

@Service
public class AgendaService {
    private final AttendanceRepository repo; private final AccessService access; private final MemberDirectory members;
    private final AdministrativeTransactions tx; private final AttendanceJournal journal; private final Clock clock;
    private final FrequencyService frequency;
    public AgendaService(AttendanceRepository repo,AccessService access,MemberDirectory members,AdministrativeTransactions tx,AttendanceJournal journal,Clock clock,FrequencyService frequency) {
        this.repo=repo;this.access=access;this.members=members;this.tx=tx;this.journal=journal;this.clock=clock;this.frequency=frequency;
    }
    @Transactional public Event create(UUID actor,EventCreate c) {
        tx.lock();access.requireAdministrator(actor);interval(c.startsAt(),c.endsAt());validateExpected(c.expectedMemberIds(),c.kind());
        UUID id=UUID.randomUUID();repo.update("""
            insert into nite_connect.calendar_events(id,title,description,kind,modality,starts_at,ends_at,created_by) values (?,?,?,?,?,?,?,?)
            """,id,c.title().strip(),c.description(),c.kind().name(),c.modality().name(),ts(c.startsAt()),ts(c.endsAt()),actor);
        participants(id,c.expectedMemberIds());Event event=repo.event(id);
        journal.record(actor,null,"CALENDAR_EVENT",id,0,"CREATED",null,snapshot(event),c.reason());return event;
    }
    @Transactional public Event update(UUID actor,UUID id,EventUpdate c) {
        Event before=write(actor,id,c.expectedVersion());
        if(before.status()!=EventStatus.AGENDADO || repo.count(
                "select count(*) from nite_connect.attendance_records where event_id=?", id) > 0) {
            throw ApiException.conflict("Programação já possui presença; preserve o encontro original");
        }
        interval(c.startsAt(),c.endsAt());validateExpected(c.expectedMemberIds(),c.kind());Object previous=snapshot(before);
        repo.changed(repo.update("""
            update nite_connect.calendar_events set title=?,description=?,kind=?,modality=?,starts_at=?,ends_at=?,version=version+1 where id=? and version=?
            """,c.title().strip(),c.description(),c.kind().name(),c.modality().name(),ts(c.startsAt()),ts(c.endsAt()),id,c.expectedVersion()));
        repo.update("delete from nite_connect.event_participants where event_id=?",id);participants(id,c.expectedMemberIds());
        Event after=repo.event(id);journal.record(actor,null,"CALENDAR_EVENT",id,after.version(),"UPDATED",previous,snapshot(after),c.reason());return after;
    }
    @Transactional public SessionPayload session(UUID actor,UUID id,Command c) {
        tx.lock();access.requireAdministrator(actor);
        throw ApiException.gone("LEGACY_SESSION_DISABLED");
    }
    @Transactional public Event close(UUID actor,UUID id,Command c,boolean cancel) {
        Event before=write(actor,id,c.expectedVersion());
        if(frequency.hasListForEvent(id)) {
            if(!cancel) throw ApiException.conflict("Encontro vinculado à Frequência; use a finalização da lista");
            return frequency.cancelForAgenda(actor,id,c.expectedVersion(),c.reason(),c.confirmed()).orElseThrow(ApiException::notFound);
        }
        if(cancel) { confirm(c.confirmed());if(before.status()==EventStatus.CANCELADO || before.status()==EventStatus.ENCERRADO) throw ApiException.conflict("Encontro já encerrado"); }
        else if(before.status()!=EventStatus.ABERTO && !(before.status()==EventStatus.AGENDADO && !clock.instant().isBefore(before.endsAt())))
            throw ApiException.conflict("Encontro não possui sessão aberta e ainda não terminou");
        repo.changed(repo.update("update nite_connect.calendar_events set status=?,qr_hash=null,qr_expires_at=null,version=version+1 where id=? and version=?",
                cancel?"CANCELADO":"ENCERRADO",id,c.expectedVersion()));
        Event after=repo.event(id);journal.record(actor,null,"CALENDAR_EVENT",id,after.version(),cancel?"CANCELLED":"SESSION_CLOSED",before,after,c.reason());return after;
    }
    @Transactional(readOnly=true) public Event read(UUID actor,UUID id) { access.requireActive(actor);return repo.event(id); }
    @Transactional(readOnly=true) public PageResult<Event> list(UUID actor,LocalDate from,LocalDate to,int page,int size) {
        access.requireActive(actor);if(from!=null && to!=null && from.isAfter(to)) throw ApiException.invalid("Período invertido");
        if(LocalDate.MAX.equals(to)) throw ApiException.invalid("Data final fora do intervalo permitido");
        String where="from nite_connect.calendar_events where true";var args=new ArrayList<Object>();
        if(from!=null) { where+=" and (ends_at is null or ends_at>?)";args.add(ts(from.atStartOfDay(clock.getZone()).toInstant())); }
        if(to!=null) { where+=" and starts_at<?";args.add(ts(to.plusDays(1).atStartOfDay(clock.getZone()).toInstant())); }
        return repo.page("*",where,args,"starts_at,id",AttendanceRepository.EVENT,page,size);
    }
    @Transactional(readOnly=true) public PageResult<ExpectedParticipant> participants(UUID actor,UUID id,int page,int size) {
        access.requireAdministrator(actor);repo.event(id);
        var result=repo.page("p.member_id,ep.member_id is not null as expected,a.id as attendance_id",
                """
                from (select member_id from nite_connect.event_participants where event_id=?
                 union select member_id from nite_connect.attendance_records where event_id=?) p
                left join nite_connect.event_participants ep on ep.event_id=? and ep.member_id=p.member_id
                left join nite_connect.attendance_records a on a.event_id=? and a.member_id=p.member_id
                """,List.of(id,id,id,id),"p.member_id",
                (r,i)->new ExpectedParticipant(r.getObject("member_id",UUID.class),null,r.getBoolean("expected"),r.getObject("attendance_id")!=null,r.getObject("attendance_id",UUID.class)),page,size);
        var names=members.names(result.items().stream().map(ExpectedParticipant::memberId).toList());
        return new PageResult<>(result.items().stream().map(p->new ExpectedParticipant(p.memberId(),names.get(p.memberId()),p.expected(),p.checkedIn(),p.attendanceId())).toList(),page,size,result.total());
    }
    private Event write(UUID actor,UUID id,long version) { tx.lock();access.requireAdministrator(actor);Event e=repo.event(id);repo.version(e.version(),version);return e; }
    private void validateExpected(List<UUID> ids,EventKind kind) {
        if(new HashSet<>(ids).size()!=ids.size()) throw ApiException.invalid("Participante repetido");
        if(kind==EventKind.FERIADO && !ids.isEmpty()) throw ApiException.invalid("Feriado não admite presença esperada");
        ids.forEach(access::requireActive);
    }
    private void participants(UUID id,List<UUID> members) { for(UUID member:members) repo.update("insert into nite_connect.event_participants(event_id,member_id) values (?,?)",id,member); }
    private Object snapshot(Event event) { return Map.of("event",event,"expectedMemberIds",repo.expected(event.id())); }
}
