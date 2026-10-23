package br.edu.unijorge.niteconnect.academic.application;

import java.time.Clock;
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
public class AcademicEnrollmentService {
    private final AcademicRepository repo;private final AcademicAuthorizationPolicy auth;private final AdministrativeTransactions tx;private final Clock clock;
    public AcademicEnrollmentService(AcademicRepository repo,AcademicAuthorizationPolicy auth,AdministrativeTransactions tx,Clock clock) { this.repo=repo;this.auth=auth;this.tx=tx;this.clock=clock; }
    @Transactional public Enrollment request(UUID actor,RequestEnrollment c) {
        tx.lock();auth.requireActive(actor);reason(c.reason());Policy p=repo.policy(c.policyId());var ds=disciplines(c.disciplines());enrollment(p,ds,clock.instant());
        Enrollment e=repo.createEnrollment(actor,p,ds);repo.event(actor,e.id(),"ENROLLMENT",e.id(),"REQUESTED",null,e,c.reason());return e;
    }
    @Transactional public Enrollment revise(UUID actor,UUID id,ReviseEnrollment c) {
        tx.lock();auth.requireActive(actor);reason(c.reason());Enrollment e=own(actor,id);version(e.version(),c.expectedVersion());
        if(e.status()!=EnrollmentStatus.CORRECAO_SOLICITADA) throw ApiException.conflict("Adesão não está em correção");
        var ds=disciplines(c.disciplines());enrollment(repo.policy(e.policyId()),ds,clock.instant());
        var result=repo.updateEnrollment(e,EnrollmentStatus.SOLICITADA,ds);repo.event(actor,id,"ENROLLMENT",id,"REVISED",e,result,c.reason());return result;
    }
    @Transactional public Enrollment decide(UUID actor,UUID id,DecideEnrollment c) {
        tx.lock();auth.requireCoordinator(actor);reason(c.reason());Enrollment e=repo.enrollment(id);version(e.version(),c.expectedVersion());auth.requireActive(e.memberId());
        if(c.decision()==null) throw ApiException.invalid("Decisão obrigatória");
        EnrollmentStatus next;
        if(c.decision()==EnrollmentDecision.ANALYZE && e.status()==EnrollmentStatus.SOLICITADA) next=EnrollmentStatus.EM_ANALISE;
        else if(e.status()==EnrollmentStatus.EM_ANALISE) {
            next=switch(c.decision()) { case REQUEST_CORRECTION -> EnrollmentStatus.CORRECAO_SOLICITADA;case APPROVE -> EnrollmentStatus.DEFERIDA;case REJECT -> EnrollmentStatus.INDEFERIDA;default -> throw ApiException.conflict("Transição inválida"); };
        } else throw ApiException.conflict("Transição inválida");
        if(next==EnrollmentStatus.DEFERIDA && !repo.policy(e.policyId()).status().equals("CONFIRMED")) throw ApiException.conflict("Política não confirmada");
        var result=repo.updateEnrollment(e,next,e.disciplines());repo.event(actor,id,"ENROLLMENT",id,"DECIDED",e,result,c.reason());return result;
    }
    public Enrollment own(UUID actor,UUID id) {
        auth.requireActive(actor);Enrollment e=repo.enrollment(id);if(!actor.equals(e.memberId())) throw ApiException.forbidden();return e;
    }
    @Transactional(readOnly=true) public Enrollment read(UUID actor,UUID id,boolean own) {
        if(own) return own(actor,id);auth.requireAdministrator(actor);return repo.enrollment(id);
    }
    @Transactional(readOnly=true) public PageResult<Enrollment> list(UUID actor,boolean own,int page,int size) {
        auth.requireActive(actor);if(!own) auth.requireAdministrator(actor);return repo.enrollments(own?actor:null,page,size);
    }
}
