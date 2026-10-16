package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.activities.application.ActivityParticipation;
import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.*;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.*;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.Period;
import br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository;
import br.edu.unijorge.niteconnect.shared.api.*;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.UUID;
import static br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository.ts;
import static br.edu.unijorge.niteconnect.attendance.application.ParticipationRules.*;

@Service
public class ParticipationService {
    private final AttendanceRepository repo; private final AccessService access; private final AdministrativeTransactions tx;
    private final ParticipationJournal journal; private final Clock clock; private final ActivityParticipation activities;
    public ParticipationService(AttendanceRepository repo,AccessService access,AdministrativeTransactions tx,ParticipationJournal journal,Clock clock,ActivityParticipation activities) {
        this.repo=repo;this.access=access;this.tx=tx;this.journal=journal;this.clock=clock;this.activities=activities;
    }
    @Transactional public AttendanceView checkIn(UUID actor,CheckIn c) {
        tx.lock();access.requireActive(actor);Instant now=clock.instant();repo.qr(c.eventId(),AgendaService.hash(c.payload()),now);
        if(repo.attendance(c.eventId(),actor).isPresent()) throw ApiException.conflict("Presença já registrada; utilize retorno");
        UUID id=repo.createAttendance(c.eventId(),actor);Event event=repo.event(c.eventId());
        UUID period=repo.createPeriod(actor,id,PeriodKind.ATTENDANCE,event.modality(),now,null,"",null,null,"QR");
        journal.record(actor,actor,"PERIOD",period,0,"CHECKED_IN",null,repo.period(period),"Entrada por sessão temporária");
        journal.record(actor,actor,"ATTENDANCE",id,0,"CREATED",null,repo.attendance(id),"Presença em encontro");return view(repo.attendance(id));
    }
    @Transactional public AttendanceView checkOut(UUID actor,CheckOut c) {
        tx.lock();access.requireActive(actor);Period before=repo.open(actor).orElseThrow(()->ApiException.conflict("Nenhuma presença aberta"));
        Attendance attendance=repo.attendance(before.attendanceId());repo.version(attendance.version(),c.expectedVersion());
        Instant now=clock.instant();if(!now.isAfter(before.startsAt())) throw ApiException.conflict("Saída deve ser posterior à entrada");
        repo.changed(repo.update("update nite_connect.participation_periods set ends_at=?,version=version+1 where id=? and version=?",ts(now),before.id(),before.version()));
        repo.touchAttendance(attendance.id(),attendance.version());
        journal.record(actor,actor,"PERIOD",before.id(),before.version()+1,"CHECKED_OUT",before,repo.period(before.id()),"Saída registrada pelo integrante");
        journal.record(actor,actor,"ATTENDANCE",attendance.id(),attendance.version()+1,"DEPARTURE",attendance,repo.attendance(attendance.id()),"Saída do encontro");
        return view(repo.attendance(attendance.id()));
    }
    @Transactional public AttendanceView returnTo(UUID actor,UUID id,Return c) {
        tx.lock();access.requireActive(actor);Attendance before=own(actor,id);repo.version(before.version(),c.expectedVersion());
        Instant now=clock.instant();repo.qr(before.eventId(),AgendaService.hash(c.payload()),now);
        if(repo.open(actor).isPresent()) throw ApiException.conflict("Já existe presença aberta");
        UUID period=repo.createPeriod(actor,id,PeriodKind.ATTENDANCE,repo.event(before.eventId()).modality(),now,null,"",null,null,"QR");
        repo.touchAttendance(id,before.version());journal.record(actor,actor,"PERIOD",period,0,"RETURNED",null,repo.period(period),"Retorno ao encontro");
        journal.record(actor,actor,"ATTENDANCE",id,before.version()+1,"RETURNED",before,repo.attendance(id),"Retorno ao encontro");return view(repo.attendance(id));
    }
    @Transactional public Period workLog(UUID actor,WorkLog c) {
        tx.lock();access.requireActive(actor);past(c.startsAt(),c.endsAt(),clock);activities.requireOwnContext(actor,c.activityId(),c.evidenceId());
        UUID id=repo.createPeriod(actor,null,PeriodKind.WORK_LOG,Modality.REMOTA,c.startsAt(),c.endsAt(),c.description(),c.activityId(),c.evidenceId(),"DECLARATION");
        Period period=repo.period(id);journal.record(actor,actor,"PERIOD",id,0,"WORK_DECLARED",null,period,"Período remoto declarado");return period;
    }
    public Attendance own(UUID actor,UUID id) { Attendance a=repo.attendance(id);if(!actor.equals(a.memberId())) throw ApiException.notFound();return a; }
    public AttendanceView view(Attendance a) { return new AttendanceView(a.id(),a.eventId(),a.memberId(),a.version(),repo.periods(a.id())); }
}
