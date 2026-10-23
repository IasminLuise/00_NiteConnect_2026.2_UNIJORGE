package br.edu.unijorge.niteconnect.academic.application;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.unijorge.niteconnect.academic.api.AcademicContracts.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicStates.EnrollmentStatus;
import br.edu.unijorge.niteconnect.academic.infrastructure.AcademicRepository;
import br.edu.unijorge.niteconnect.shared.api.*;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import static br.edu.unijorge.niteconnect.academic.domain.AcademicRules.*;

@Service
public class AcademicForwardingService {
    private final AcademicRepository repo;private final AcademicAuthorizationPolicy auth;private final AdministrativeTransactions tx;
    public AcademicForwardingService(AcademicRepository repo,AcademicAuthorizationPolicy auth,AdministrativeTransactions tx) { this.repo=repo;this.auth=auth;this.tx=tx; }
    @Transactional public Forwarding forward(UUID actor,UUID homologationId,ForwardAssessment c) {
        tx.lock();auth.requireCoordinator(actor);reason(c.reason());confirmed(c.confirmed());text(c.receiptReference(),1000,"Comprovante de encaminhamento");Homologation h=repo.homologation(homologationId);Enrollment e=repo.enrollment(h.enrollmentId());version(e.version(),c.expectedVersion());
        if(e.status()!=EnrollmentStatus.HOMOLOGADA || repo.pendingReview(e.id()) || repo.currentHomologation(e.id()).filter(v->v.id().equals(h.id())).isEmpty()) throw ApiException.conflict("Encaminhamento exige homologação vigente sem revisão pendente");
        String procedure=repo.policy(e.policyId()).rules().forwardingProcedureReference();
        if(procedure==null || procedure.isBlank()) throw ApiException.conflict("Procedimento institucional de encaminhamento não confirmado");
        Forwarding f=repo.forward(actor,h,procedure,c.receiptReference());repo.updateEnrollment(e,e.status(),e.disciplines());repo.event(actor,e.id(),"FORWARDING",f.id(),"FORWARDED",null,f,c.reason());return f;
    }
    @Transactional(readOnly=true) public PageResult<Forwarding> list(UUID actor,UUID homologationId,int page,int size) { auth.requireAdministrator(actor);repo.homologation(homologationId);return repo.forwardings(homologationId,page,size); }
    @Transactional(readOnly=true) public PageResult<AcademicEvent> history(UUID actor,UUID enrollmentId,int page,int size) { auth.requireCoordinator(actor);repo.enrollment(enrollmentId);return repo.history(enrollmentId,page,size); }
}
