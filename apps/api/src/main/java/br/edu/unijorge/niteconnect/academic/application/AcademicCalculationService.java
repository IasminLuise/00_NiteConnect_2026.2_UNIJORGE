package br.edu.unijorge.niteconnect.academic.application;

import java.time.Clock;
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
public class AcademicCalculationService {
    private final AcademicRepository repo;private final AcademicAuthorizationPolicy auth;private final AdministrativeTransactions tx;private final AcademicEvidenceService evidence;private final PillarAssessmentService assessments;private final Clock clock;
    public AcademicCalculationService(AcademicRepository repo,AcademicAuthorizationPolicy auth,AdministrativeTransactions tx,AcademicEvidenceService evidence,PillarAssessmentService assessments,Clock clock) { this.repo=repo;this.auth=auth;this.tx=tx;this.evidence=evidence;this.assessments=assessments;this.clock=clock; }
    @Transactional public Calculation calculate(UUID actor,UUID enrollmentId,Command c) {
        tx.lock();auth.requireAdministrator(actor);reason(c.reason());Enrollment e=repo.enrollment(enrollmentId);AcademicEvidenceService.editable(e);version(e.version(),c.expectedVersion());
        Policy policy=repo.policy(e.policyId());if(!policy.status().equals("CONFIRMED")) throw ApiException.conflict("Política não confirmada");
        var inputs=repo.latestAssessments(e.id());var value=AcademicCalculator.calculate(policy.rules(),inputs);
        Map<UUID,Evidence> sources=new LinkedHashMap<>();
        for(var a:inputs) for(UUID id:a.evidenceIds()) {
            Evidence item=repo.evidence(id);if(!e.id().equals(item.enrollmentId())) throw ApiException.conflict("Referência fora da avaliação");evidence.requireCurrent(actor,item);sources.put(item.id(),item);
        }
        // All operational mutations and this capture acquire the existing administrative transaction lock.
        // Reads occur after acquisition under READ COMMITTED, so a prior writer's committed state is visible.
        // Later writers cannot alter any local source until this capture commits.
        var updated=repo.updateEnrollment(e,EnrollmentStatus.CALCULADA,e.disciplines());
        var data=new CalculationData(policy,List.copyOf(inputs),List.copyOf(sources.values()),value,AcademicCalculator.ALGORITHM,clock.instant());
        var result=repo.calculate(updated,data);repo.event(actor,e.id(),"CALCULATION",result.id(),"CALCULATED",null,result,c.reason());return auth.isCoordinator(actor)?result:summary(result);
    }
    private Calculation summary(Calculation c) {
        var data=new CalculationData(null,List.of(),List.of(),c.data().result(),c.data().algorithm(),c.data().cutAt());
        return new Calculation(c.id(),c.enrollmentId(),c.policyId(),c.enrollmentVersion(),data,c.createdAt());
    }
    @Transactional(readOnly=true) public PageResult<Calculation> list(UUID actor,UUID enrollmentId,boolean own,int page,int size) {
        auth.requireActive(actor);Enrollment e=repo.enrollment(enrollmentId);assessments.requireVisible(actor,e,own);var result=repo.calculations(enrollmentId,own,page,size);
        if(!own && auth.isCoordinator(actor)) return result;
        return new PageResult<>(result.items().stream().map(this::summary).toList(),result.page(),result.size(),result.total());
    }
}
