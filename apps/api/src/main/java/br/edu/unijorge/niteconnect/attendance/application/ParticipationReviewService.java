package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.*;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.*;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.Period;
import br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import static br.edu.unijorge.niteconnect.attendance.application.ParticipationRules.*;
import static br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository.ts;

@Service
public class ParticipationReviewService {
    private final AttendanceRepository repo; private final AccessService access; private final AdministrativeTransactions tx;
    private final ParticipationJournal journal; private final Clock clock;
    public ParticipationReviewService(AttendanceRepository repo,AccessService access,AdministrativeTransactions tx,ParticipationJournal journal,Clock clock) {
        this.repo=repo;this.access=access;this.tx=tx;this.journal=journal;this.clock=clock;
    }
    @Transactional public Period review(UUID actor,UUID id,Review c) {
        admin(actor);Period before=repo.period(id);other(actor,before.memberId());repo.version(before.version(),c.expectedVersion());
        if(before.endsAt()==null) throw ApiException.conflict("Corrija ou encerre o período aberto antes de revisar");
        if(c.status()==ReviewStatus.PENDENTE || c.status()==before.reviewStatus()) throw ApiException.conflict("Decisão de revisão inválida");
        if(c.status()==ReviewStatus.APROVADA) validatePeriodContext(before,before.startsAt(),before.endsAt(),false);
        repo.changed(repo.update("""
            update nite_connect.participation_periods set review_status=?,reviewed_by=?,reviewed_at=?,review_reason=?,version=version+1 where id=? and version=?
            """,c.status().name(),actor,ts(clock.instant()),c.reason(),id,c.expectedVersion()));
        Period after=repo.period(id);journal.record(actor,before.memberId(),"PERIOD",id,after.version(),"REVIEWED",before,after,c.reason());return after;
    }
    @Transactional public Justification justify(UUID actor,Justify c) {
        tx.lock();access.requireActive(actor);Event event=repo.event(c.eventId());
        if(!repo.expected(event.id(),actor) || event.status()==EventStatus.CANCELADO || event.kind()==EventKind.FERIADO
                || (clock.instant().isBefore(event.endsAt()) && event.status()!=EventStatus.ENCERRADO))
            throw ApiException.conflict("Justificativa exige encontro ocorrido e participação esperada");
        UUID id=UUID.randomUUID();repo.update("insert into nite_connect.absence_justifications(id,member_id,event_id,reason) values (?,?,?,?)",id,actor,c.eventId(),c.reason());
        Justification after=repo.justification(id);journal.record(actor,actor,"JUSTIFICATION",id,0,"SUBMITTED",null,after,c.reason());return after;
    }
    @Transactional public Justification complement(UUID actor,UUID id,Complement c) {
        tx.lock();access.requireActive(actor);Justification before=repo.justification(id);own(actor,before.memberId());repo.version(before.version(),c.expectedVersion());
        if(!before.status().equals("COMPLEMENTACAO_SOLICITADA")) throw ApiException.conflict("Complementação não solicitada");
        repo.changed(repo.update("""
            update nite_connect.absence_justifications set reason=?,status='PENDENTE',decision_reason=null,decided_by=null,decided_at=null,version=version+1 where id=? and version=?
            """,c.reason(),id,c.expectedVersion()));
        Justification after=repo.justification(id);journal.record(actor,actor,"JUSTIFICATION",id,after.version(),"COMPLEMENTED",before,after,c.reason());return after;
    }
    @Transactional public Justification decide(UUID actor,UUID id,Decide c) {
        admin(actor);Justification before=repo.justification(id);other(actor,before.memberId());repo.version(before.version(),c.expectedVersion());
        if(!before.status().equals("PENDENTE")) throw ApiException.conflict("Justificativa não está pendente");
        repo.changed(repo.update("""
            update nite_connect.absence_justifications set status=?,decision_reason=?,decided_by=?,decided_at=?,version=version+1 where id=? and version=?
            """,c.decision().name(),c.reason(),actor,ts(clock.instant()),id,c.expectedVersion()));
        Justification after=repo.justification(id);journal.record(actor,before.memberId(),"JUSTIFICATION",id,after.version(),"DECIDED",before,after,c.reason());return after;
    }
    @Transactional public Correction request(UUID actor,RequestCorrection c) {
        tx.lock();access.requireActive(actor);past(c.startsAt(),c.endsAt(),clock);
        if((c.periodId()==null)==(c.eventId()==null)) throw ApiException.invalid("Informe periodId ou eventId, exclusivamente");
        if(c.periodId()!=null) { Period p=repo.period(c.periodId());own(actor,p.memberId());validatePeriodContext(p,c.startsAt(),c.endsAt(),true); }
        else {
            Event e=repo.event(c.eventId());validateAttendanceTimes(e,c.startsAt(),c.endsAt(),false);
            if(!repo.expected(c.eventId(),actor) || repo.attendance(c.eventId(),actor).isPresent()) throw ApiException.conflict("Solicite correção do período existente ou encontro esperado");
        }
        UUID id=UUID.randomUUID();repo.update("""
            insert into nite_connect.correction_requests(id,member_id,period_id,event_id,proposed_start,proposed_end,reason) values (?,?,?,?,?,?,?)
            """,id,actor,c.periodId(),c.eventId(),ts(c.startsAt()),ts(c.endsAt()),c.reason());
        Correction after=repo.correction(id);journal.record(actor,actor,"CORRECTION",id,0,"REQUESTED",null,after,c.reason());return after;
    }
    @Transactional public Period correct(UUID actor,UUID id,Correct c) {
        admin(actor);confirm(c.confirmed());Period before=repo.period(id);other(actor,before.memberId());repo.version(before.version(),c.expectedVersion());
        return applyCorrection(actor,before,c.startsAt(),c.endsAt(),c.reason());
    }
    @Transactional public Correction decideCorrection(UUID actor,UUID id,DecideCorrection c) {
        admin(actor);confirm(c.confirmed());Correction before=repo.correction(id);other(actor,before.memberId());repo.version(before.version(),c.expectedVersion());
        if(!before.status().equals("PENDENTE") || c.decision()==Decision.COMPLEMENTACAO_SOLICITADA) throw ApiException.conflict("Decisão de correção inválida");
        if(c.decision()==Decision.ACEITA) {
            if(before.periodId()!=null) {
                Period p=repo.period(before.periodId());
                if(c.expectedPeriodVersion()==null) throw ApiException.invalid("expectedPeriodVersion obrigatório");
                repo.version(p.version(),c.expectedPeriodVersion());applyCorrection(actor,p,before.proposedStart(),before.proposedEnd(),c.reason());
            } else {
                Event event=repo.event(before.eventId());validateAttendanceTimes(event,before.proposedStart(),before.proposedEnd(),false);
                if(repo.attendance(event.id(),before.memberId()).isPresent()) throw ApiException.conflict("Presença já registrada");
                UUID attendance=repo.createAttendance(event.id(),before.memberId());
                UUID period=repo.createPeriod(before.memberId(),attendance,PeriodKind.ATTENDANCE,event.modality(),before.proposedStart(),before.proposedEnd(),"",null,null,"CORRECTION");
                journal.record(actor,before.memberId(),"ATTENDANCE",attendance,0,"CREATED_BY_CORRECTION",null,repo.attendance(attendance),c.reason());
                journal.record(actor,before.memberId(),"PERIOD",period,0,"CREATED_BY_CORRECTION",null,repo.period(period),c.reason());
            }
        }
        repo.changed(repo.update("""
            update nite_connect.correction_requests set status=?,decision_reason=?,decided_by=?,decided_at=?,version=version+1 where id=? and version=?
            """,c.decision().name(),c.reason(),actor,ts(clock.instant()),id,c.expectedVersion()));
        Correction after=repo.correction(id);journal.record(actor,before.memberId(),"CORRECTION",id,after.version(),"DECIDED",before,after,c.reason());return after;
    }
    private Period applyCorrection(UUID actor,Period before,Instant start,Instant end,String reason) {
        past(start,end,clock);validatePeriodContext(before,start,end,true);
        repo.changed(repo.update("""
            update nite_connect.participation_periods set starts_at=?,ends_at=?,origin='CORRECTION',review_status='PENDENTE',
             reviewed_by=null,reviewed_at=null,review_reason=null,version=version+1 where id=? and version=?
            """,ts(start),ts(end),before.id(),before.version()));
        if(before.attendanceId()!=null) {
            Attendance a=repo.attendance(before.attendanceId());repo.touchAttendance(a.id(),a.version());
            journal.record(actor,before.memberId(),"ATTENDANCE",a.id(),a.version()+1,"CORRECTED",a,repo.attendance(a.id()),reason);
        }
        Period after=repo.period(before.id());journal.record(actor,before.memberId(),"PERIOD",before.id(),after.version(),"CORRECTED",before,after,reason);return after;
    }
    private void validatePeriodContext(Period p,Instant start,Instant end,boolean correction) {
        if(p.attendanceId()!=null) validateAttendanceTimes(repo.event(repo.attendance(p.attendanceId()).eventId()),start,end,correction);
    }
    private void validateAttendanceTimes(Event e,Instant start,Instant end,boolean correction) {
        if(e.status()==EventStatus.AGENDADO || e.status()==EventStatus.CANCELADO && !correction
                || e.kind()==EventKind.FERIADO || start.isBefore(e.startsAt()) || end.isAfter(e.endsAt()))
            throw ApiException.conflict("Intervalo incompatível com encontro");
    }
    private void admin(UUID actor) { tx.lock();access.requireAdministrator(actor); }
    private void own(UUID actor,UUID member) { if(!actor.equals(member)) throw ApiException.notFound(); }
    private void other(UUID actor,UUID member) { if(actor.equals(member)) throw ApiException.forbidden(); }
}
