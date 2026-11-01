package br.edu.unijorge.niteconnect.attendance.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.activities.application.ActivityParticipation;
import br.edu.unijorge.niteconnect.attendance.api.AttendanceContracts.*;
import br.edu.unijorge.niteconnect.attendance.api.ParticipationGovernanceContracts.*;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.*;
import br.edu.unijorge.niteconnect.attendance.domain.Participation.Period;
import br.edu.unijorge.niteconnect.attendance.infrastructure.AttendanceRepository;
import br.edu.unijorge.niteconnect.attendance.infrastructure.ParticipationGovernanceRepository;
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
    private final AttendanceRepository repo; private final ParticipationGovernanceRepository governance;
    private final AccessService access; private final AdministrativeTransactions tx;
    private final ParticipationJournal journal; private final Clock clock;
    private final ActivityParticipation activities;
    public ParticipationReviewService(AttendanceRepository repo,ParticipationGovernanceRepository governance,AccessService access,
                                      AdministrativeTransactions tx,ParticipationJournal journal,Clock clock,ActivityParticipation activities) {
        this.repo=repo;this.governance=governance;this.access=access;this.tx=tx;this.journal=journal;this.clock=clock;this.activities=activities;
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
    @Transactional public ReviewRequest respondToWorkLogCorrection(UUID actor,UUID requestId,SubmitWorkLogCorrection c) {
        tx.lock();access.requireActive(actor);
        ReviewRequest request=governance.reviewRequest(actor,requestId);
        if(!request.status().equals("AGUARDANDO_RESPOSTA")) throw ApiException.conflict("Este pedido já recebeu resposta");
        Period period=repo.period(request.periodId());
        if(period.kind()!=PeriodKind.WORK_LOG || period.endsAt()==null || period.reviewStatus()!=ReviewStatus.PENDENTE)
            throw ApiException.conflict("O pedido não está vinculado a um lançamento remoto pendente e fechado");
        repo.version(period.version(),c.expectedPeriodVersion());past(c.startsAt(),c.endsAt(),clock);
        activities.requireOwnContext(actor,c.activityId(),c.evidenceId());
        if(repo.overlapsOther(actor,period.id(),c.startsAt(),c.endsAt())) throw ApiException.conflict("O intervalo proposto sobrepõe outro registro");
        governance.createWorkLogCorrectionResponse(requestId,period.id(),actor,c,clock.instant());
        ReviewRequest after=governance.reviewRequest(actor,requestId);
        journal.record(actor,actor,"WORK_LOG_CORRECTION_RESPONSE",after.response().id(),after.response().version(),
                "SUBMITTED",null,after.response(),c.reason());
        return after;
    }
    @Transactional public ReviewRequest decideWorkLogCorrection(UUID actor,UUID requestId,DecideWorkLogCorrection c) {
        admin(actor);confirm(c.confirmed());
        ReviewRequest before=governance.reviewRequest(requestId);
        other(actor,repo.period(before.periodId()).memberId());
        WorkLogCorrectionResponseView response=before.response();
        if(response==null || response.decision()!=null) throw ApiException.conflict("Resposta inexistente ou já decidida");
        repo.version(response.version(),c.expectedResponseVersion());
        Period period=repo.period(before.periodId());
        repo.version(period.version(),response.expectedPeriodVersion());
        if(period.kind()!=PeriodKind.WORK_LOG || period.endsAt()==null || period.reviewStatus()!=ReviewStatus.PENDENTE)
            throw ApiException.conflict("O lançamento não está mais pendente para correção");
        Long resultingVersion=null;
        if(c.decision()==WorkLogCorrectionDecisionType.ACEITA) {
            past(response.proposedStart(),response.proposedEnd(),clock);
            activities.requireOwnContext(period.memberId(),response.proposedActivityId(),response.proposedEvidenceId());
            if(repo.overlapsOther(period.memberId(),period.id(),response.proposedStart(),response.proposedEnd()))
                throw ApiException.conflict("O intervalo proposto sobrepõe outro registro");
            repo.changed(repo.update("""
                    update nite_connect.participation_periods set starts_at=?,ends_at=?,description=?,activity_id=?,evidence_id=?,
                      origin='CORRECTION',version=version+1 where id=? and version=? and review_status='PENDENTE'
                    """,ts(response.proposedStart()),ts(response.proposedEnd()),response.proposedDescription(),
                    response.proposedActivityId(),response.proposedEvidenceId(),period.id(),period.version()));
            Period corrected=repo.period(period.id());
            journal.record(actor,period.memberId(),"PERIOD",period.id(),corrected.version(),"WORK_LOG_CORRECTED_BY_RESPONSE",
                    period,corrected,c.reason());
            resultingVersion=corrected.version();
        }
        governance.createWorkLogCorrectionDecision(response.id(),c.decision(),c.expectedResponseVersion(),resultingVersion,actor,c.reason(),clock.instant());
        ReviewRequest after=governance.reviewRequest(requestId);
        journal.record(actor,period.memberId(),"WORK_LOG_CORRECTION_RESPONSE",response.id(),response.version(),"DECIDED",
                response,after.response(),c.reason());
        return after;
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
