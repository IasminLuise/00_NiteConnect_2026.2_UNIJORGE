package br.edu.unijorge.niteconnect.academic.application;

import java.time.Clock;
import java.time.Duration;
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
public class AcademicReviewService {
    private final AcademicRepository repo;private final AcademicAuthorizationPolicy auth;private final AdministrativeTransactions tx;private final PillarAssessmentService visibility;private final Clock clock;
    public AcademicReviewService(AcademicRepository repo,AcademicAuthorizationPolicy auth,AdministrativeTransactions tx,PillarAssessmentService visibility,Clock clock) { this.repo=repo;this.auth=auth;this.tx=tx;this.visibility=visibility;this.clock=clock; }
    @Transactional public Review request(UUID actor,UUID enrollmentId,RequestReview c,boolean own) {
        tx.lock();auth.requireActive(actor);reason(c.reason());Enrollment e=repo.enrollment(enrollmentId);
        if(own) visibility.requireVisible(actor,e,true);else auth.requireCoordinator(actor);
        Calculation calc=repo.calculation(c.calculationId());
        if(!e.id().equals(calc.enrollmentId())) throw ApiException.forbidden();
        if(repo.latestCalculation(e.id()).filter(v->v.id().equals(calc.id())).isEmpty()) throw ApiException.conflict("Revisão deve referenciar o cálculo atual");
        var h=repo.currentHomologation(e.id());
        if(e.status()!=EnrollmentStatus.CALCULADA && e.status()!=EnrollmentStatus.HOMOLOGADA) throw ApiException.conflict("Avaliação não está disponível para revisão");
        Long seconds=repo.policy(e.policyId()).rules().reviewWindowSeconds();
        if(own && (seconds==null || Duration.between(h.filter(v->v.calculationId().equals(calc.id())).map(Homologation::createdAt).orElse(calc.createdAt()),clock.instant()).compareTo(Duration.ofSeconds(seconds))>0)) throw ApiException.conflict("Rito ou prazo de revisão indisponível");
        Review r=repo.requestReview(actor,e,calc,h.filter(v->v.calculationId().equals(calc.id())).map(Homologation::id).orElse(null),c.reason());
        if(e.status()==EnrollmentStatus.CALCULADA) repo.updateEnrollment(e,EnrollmentStatus.EM_REVISAO,e.disciplines());
        repo.event(actor,e.id(),"REVIEW",r.id(),"REQUESTED",null,r,c.reason());return r;
    }
    @Transactional public Review decide(UUID actor,UUID id,DecideReview c) {
        tx.lock();auth.requireCoordinator(actor);reason(c.reason());confirmed(c.confirmed());Review r=repo.review(id);version(r.version(),c.expectedVersion());
        if(!r.status().equals("REQUESTED")) throw ApiException.conflict("Revisão já decidida");
        Enrollment e=repo.enrollment(r.enrollmentId());Review result=repo.decideReview(r,actor,c.accepted(),c.reason());
        EnrollmentStatus next=c.accepted()?EnrollmentStatus.EM_ACOMPANHAMENTO:(r.homologationId()!=null?EnrollmentStatus.HOMOLOGADA:EnrollmentStatus.CALCULADA);
        repo.updateEnrollment(e,next,e.disciplines());repo.event(actor,e.id(),"REVIEW",id,"DECIDED",r,result,c.reason());return result;
    }
    @Transactional public Review reopen(UUID actor,UUID homologationId,Command c) {
        tx.lock();auth.requireCoordinator(actor);reason(c.reason());confirmed(c.confirmed());Homologation h=repo.homologation(homologationId);Enrollment e=repo.enrollment(h.enrollmentId());version(e.version(),c.expectedVersion());
        if(e.status()!=EnrollmentStatus.HOMOLOGADA || repo.currentHomologation(e.id()).filter(v->v.id().equals(h.id())).isEmpty() || repo.pendingReview(e.id())) throw ApiException.conflict("Homologação não pode ser reaberta neste estado");
        Review r=repo.requestReview(actor,e,repo.calculation(h.calculationId()),h.id(),c.reason());Review result=repo.decideReview(r,actor,true,c.reason());
        repo.updateEnrollment(e,EnrollmentStatus.EM_ACOMPANHAMENTO,e.disciplines());repo.event(actor,e.id(),"REVIEW",r.id(),"HOMOLOGATION_REOPENED",h,result,c.reason());return result;
    }
    @Transactional(readOnly=true) public PageResult<Review> list(UUID actor,UUID enrollmentId,boolean own,int page,int size) {
        auth.requireActive(actor);Enrollment e=repo.enrollment(enrollmentId);if(own) { if(!actor.equals(e.memberId())) throw ApiException.forbidden(); } else auth.requireCoordinator(actor);
        var result=repo.reviews(enrollmentId,page,size);
        if(!own) return result;
        var items=result.items().stream().map(r->actor.equals(r.requestedBy())?r:new Review(r.id(),r.enrollmentId(),r.calculationId(),r.homologationId(),r.requestedBy(),null,r.status(),r.decidedBy(),null,r.version(),r.createdAt())).toList();
        return new PageResult<>(items,result.page(),result.size(),result.total());
    }
}
