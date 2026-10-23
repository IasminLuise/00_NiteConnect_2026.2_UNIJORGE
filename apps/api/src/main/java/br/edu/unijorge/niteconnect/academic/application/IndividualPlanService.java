package br.edu.unijorge.niteconnect.academic.application;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.unijorge.niteconnect.academic.api.AcademicContracts.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicStates.*;
import br.edu.unijorge.niteconnect.academic.infrastructure.AcademicRepository;
import br.edu.unijorge.niteconnect.shared.api.*;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import static br.edu.unijorge.niteconnect.academic.domain.AcademicRules.*;

@Service
public class IndividualPlanService {
    private final AcademicRepository repo;private final AcademicAuthorizationPolicy auth;private final AcademicEnrollmentService enrollments;private final AdministrativeTransactions tx;
    public IndividualPlanService(AcademicRepository repo,AcademicAuthorizationPolicy auth,AcademicEnrollmentService enrollments,AdministrativeTransactions tx) { this.repo=repo;this.auth=auth;this.enrollments=enrollments;this.tx=tx; }
    @Transactional public Plan save(UUID actor,UUID enrollmentId,SavePlan c) {
        tx.lock();auth.requireActive(actor);reason(c.reason());text(c.content(),20000,"Plano");Enrollment e=enrollments.own(actor,enrollmentId);version(e.version(),c.expectedVersion());
        if(e.status()==EnrollmentStatus.HOMOLOGADA || e.status()==EnrollmentStatus.INDEFERIDA) throw ApiException.conflict("Situação da adesão impede novo plano");
        var latest=repo.latestPlan(enrollmentId);
        if(latest.isPresent() && (latest.get().status()==PlanStatus.SUBMETIDO || latest.get().status()==PlanStatus.EM_ANALISE)) throw ApiException.conflict("Plano em análise");
        Plan p=repo.createPlan(enrollmentId,c.content());repo.updateEnrollment(e,e.status(),e.disciplines());repo.event(actor,enrollmentId,"PLAN",p.id(),"CREATED",null,p,c.reason());return p;
    }
    @Transactional public Plan submit(UUID actor,UUID id,Command c) {
        tx.lock();auth.requireActive(actor);reason(c.reason());Plan p=repo.plan(id);Enrollment e=enrollments.own(actor,p.enrollmentId());version(p.version(),c.expectedVersion());
        if(p.status()!=PlanStatus.RASCUNHO || !repo.latestPlan(e.id()).orElseThrow().id().equals(id)) throw ApiException.conflict("Submeta a revisão atual em rascunho");
        Plan result=repo.planState(p,PlanStatus.SUBMETIDO);repo.updateEnrollment(e,e.status(),e.disciplines());repo.event(actor,e.id(),"PLAN",id,"SUBMITTED",p,result,c.reason());return result;
    }
    @Transactional public Plan decide(UUID actor,UUID id,DecidePlan c) {
        tx.lock();auth.requireCoordinator(actor);reason(c.reason());Plan p=repo.plan(id);version(p.version(),c.expectedVersion());Enrollment e=repo.enrollment(p.enrollmentId());
        if(!repo.latestPlan(e.id()).orElseThrow().id().equals(id)) throw ApiException.conflict("Decida a revisão atual");
        PlanStatus next;
        if(p.status()==PlanStatus.SUBMETIDO && c.decision()==PlanDecision.ANALYZE) next=PlanStatus.EM_ANALISE;
        else if(p.status()==PlanStatus.EM_ANALISE && c.decision()==PlanDecision.APPROVE) next=PlanStatus.APROVADO;
        else if(p.status()==PlanStatus.EM_ANALISE && c.decision()==PlanDecision.REQUEST_CORRECTION) next=PlanStatus.CORRECAO_SOLICITADA;
        else throw ApiException.conflict("Transição inválida");
        if(next==PlanStatus.CORRECAO_SOLICITADA || c.feedback()!=null) text(c.feedback(),1000,"Orientação ao integrante");
        var result=repo.planState(p,next,c.feedback());repo.updateEnrollment(e,e.status(),e.disciplines());repo.event(actor,e.id(),"PLAN",id,"DECIDED",p,result,c.reason());return result;
    }
    @Transactional(readOnly=true) public PageResult<Plan> list(UUID actor,UUID enrollmentId,boolean own,int page,int size) {
        enrollments.read(actor,enrollmentId,own);return repo.plans(enrollmentId,page,size);
    }
}
