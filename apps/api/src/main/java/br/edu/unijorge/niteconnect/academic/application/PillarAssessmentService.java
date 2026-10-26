package br.edu.unijorge.niteconnect.academic.application;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.unijorge.niteconnect.academic.api.AcademicContracts.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicStates.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicCalculator;
import br.edu.unijorge.niteconnect.academic.infrastructure.AcademicRepository;
import br.edu.unijorge.niteconnect.shared.api.*;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import static br.edu.unijorge.niteconnect.academic.domain.AcademicRules.*;

@Service
public class PillarAssessmentService {
    private final AcademicRepository repo;private final AcademicAuthorizationPolicy auth;private final AdministrativeTransactions tx;private final AcademicEvidenceService evidence;
    public PillarAssessmentService(AcademicRepository repo,AcademicAuthorizationPolicy auth,AdministrativeTransactions tx,AcademicEvidenceService evidence) { this.repo=repo;this.auth=auth;this.tx=tx;this.evidence=evidence; }
    @Transactional public Assessment assess(UUID actor,UUID enrollmentId,AssessPillar c) {
        tx.lock();auth.requireCoordinator(actor);reason(c.reason());Enrollment e=repo.enrollment(enrollmentId);AcademicEvidenceService.editable(e);version(e.version(),c.expectedVersion());
        Policy p=repo.policy(e.policyId());if(!p.status().equals("CONFIRMED")) throw ApiException.conflict("Política não confirmada");AcademicCalculator.score(p.rules(),c.score());
        if(Boolean.TRUE.equals(p.rules().planRequired()) && repo.latestPlan(e.id()).filter(v->v.status()==PlanStatus.APROVADO).isEmpty()) throw ApiException.conflict("Plano aprovado exigido pela política");
        if(c.pillar()==null || c.evidenceIds()==null || c.evidenceIds().isEmpty() || c.evidenceIds().size()>100 || new HashSet<>(c.evidenceIds()).size()!=c.evidenceIds().size()) throw ApiException.invalid("Referências de evidência inválidas");
        List<Evidence> refs=new ArrayList<>();
        for(UUID id:c.evidenceIds()) { Evidence item=repo.evidence(id);if(!e.id().equals(item.enrollmentId())) throw ApiException.forbidden();evidence.requireCurrent(actor,item);refs.add(item); }
        boolean supports=refs.stream().anyMatch(item->supports(c.pillar(),item.snapshot()));
        if(!supports) throw ApiException.conflict("Pilar sem evidência pertinente validada");
        Assessment result=repo.assess(actor,e,c.pillar(),c.score(),List.copyOf(c.evidenceIds()),c.reason());repo.updateEnrollment(e,EnrollmentStatus.EM_ACOMPANHAMENTO,e.disciplines());repo.event(actor,e.id(),"ASSESSMENT",result.id(),"ASSESSED",null,result,c.reason());return result;
    }
    public static boolean supports(Pillar pillar,SourceSnapshot s) {
        return switch(pillar) {
            case DOCUMENTATION -> s.source()==Source.DOCUMENT && s.validatedBy()!=null && "APPROVED".equals(s.validationStatus());
            case PRESENTATIONS -> s.source()==Source.PRESENTATION && s.validatedBy()!=null && "APPROVED".equals(s.validationStatus());
            case DELIVERIES -> s.source()==Source.ACTIVITY && "CONCLUIDA".equals(s.validationStatus());
        };
    }
    public void requireVisible(UUID actor,Enrollment e,boolean own) {
        if(own) {
            auth.requireActive(actor);if(!actor.equals(e.memberId())) throw ApiException.forbidden();
            if(repo.currentHomologation(e.id()).isEmpty() || !Boolean.TRUE.equals(repo.policy(e.policyId()).rules().discloseResults())) throw ApiException.conflict("Resultado ainda não disponível para divulgação");
        } else auth.requireAdministrator(actor);
    }
    @Transactional(readOnly=true) public PageResult<Assessment> list(UUID actor,UUID id,boolean own,int page,int size) {
        auth.requireActive(actor);Enrollment e=repo.enrollment(id);requireVisible(actor,e,own);var result=repo.assessments(id,own,page,size);
        if(!own && auth.isCoordinator(actor)) return result;
        var items=result.items().stream().map(a->new Assessment(a.id(),a.enrollmentId(),a.pillar(),own?a.score():null,List.of(),null,a.revision(),null,a.createdAt())).toList();
        return new PageResult<>(items,result.page(),result.size(),result.total());
    }
}
