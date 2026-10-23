package br.edu.unijorge.niteconnect.academic.application;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.unijorge.niteconnect.academic.api.AcademicContracts.Command;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicStates.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicCalculator;
import br.edu.unijorge.niteconnect.academic.infrastructure.AcademicRepository;
import br.edu.unijorge.niteconnect.shared.api.*;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import static br.edu.unijorge.niteconnect.academic.domain.AcademicRules.*;

@Service
public class AcademicHomologationService {
    private final AcademicRepository repo;private final AcademicAuthorizationPolicy auth;private final AdministrativeTransactions tx;private final AcademicEvidenceService evidence;
    public AcademicHomologationService(AcademicRepository repo,AcademicAuthorizationPolicy auth,AdministrativeTransactions tx,AcademicEvidenceService evidence) { this.repo=repo;this.auth=auth;this.tx=tx;this.evidence=evidence; }
    @Transactional public Homologation homologate(UUID actor,UUID calculationId,Command c) {
        tx.lock();auth.requireCoordinator(actor);reason(c.reason());confirmed(c.confirmed());Calculation calc=repo.calculation(calculationId);Enrollment e=repo.enrollment(calc.enrollmentId());version(e.version(),c.expectedVersion());auth.requireActive(e.memberId());
        if(e.status()!=EnrollmentStatus.CALCULADA || repo.pendingReview(e.id())) throw ApiException.conflict("Avaliação não está pronta para homologação");
        if(repo.latestCalculation(e.id()).filter(v->v.id().equals(calc.id())).isEmpty()) throw ApiException.conflict("Cálculo não é a revisão atual");
        Policy p=repo.policy(e.policyId());
        if(!p.status().equals("CONFIRMED") || !p.equals(calc.data().policy())) throw ApiException.conflict("Política incompatível com o cálculo");
        if(Boolean.TRUE.equals(p.rules().planRequired()) && repo.latestPlan(e.id()).filter(v->v.status()==PlanStatus.APROVADO).isEmpty()) throw ApiException.conflict("Plano aprovado obrigatório");
        var inputs=repo.latestAssessments(e.id());
        if(!inputs.equals(calc.data().assessments()) || AcademicCalculator.calculate(p.rules(),inputs).compareTo(calc.data().result())!=0) throw ApiException.conflict("Insumos alterados; calcule nova revisão");
        for(Evidence item:calc.data().evidence()) evidence.requireCurrent(actor,item);
        UUID previous=repo.currentHomologation(e.id()).map(Homologation::id).orElse(null);
        Homologation h=repo.homologate(actor,e,calc.id(),previous,c.reason());repo.updateEnrollment(e,EnrollmentStatus.HOMOLOGADA,e.disciplines());repo.event(actor,e.id(),"HOMOLOGATION",h.id(),"HOMOLOGATED",null,h,c.reason());return h;
    }
    @Transactional(readOnly=true) public Homologation current(UUID actor,UUID enrollmentId) {
        auth.requireAdministrator(actor);repo.enrollment(enrollmentId);return repo.currentHomologation(enrollmentId).orElseThrow(ApiException::notFound);
    }
}
