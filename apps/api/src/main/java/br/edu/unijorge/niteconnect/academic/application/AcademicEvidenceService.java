package br.edu.unijorge.niteconnect.academic.application;

import java.time.Clock;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.unijorge.niteconnect.academic.api.AcademicContracts.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.*;
import br.edu.unijorge.niteconnect.academic.domain.AcademicStates.*;
import br.edu.unijorge.niteconnect.academic.infrastructure.AcademicRepository;
import br.edu.unijorge.niteconnect.academic.integration.DocumentEvidenceReader;
import br.edu.unijorge.niteconnect.activities.application.ActivityEvidenceReader;
import br.edu.unijorge.niteconnect.attendance.application.AttendanceAcademicReader;
import br.edu.unijorge.niteconnect.members.application.MemberAcademicReader;
import br.edu.unijorge.niteconnect.shared.api.*;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import static br.edu.unijorge.niteconnect.academic.domain.AcademicRules.*;

@Service
public class AcademicEvidenceService {
    private final AcademicRepository repo;private final AcademicAuthorizationPolicy auth;private final AcademicEnrollmentService enrollments;
    private final AdministrativeTransactions tx;private final Clock clock;private final ActivityEvidenceReader activities;
    private final AttendanceAcademicReader attendance;private final MemberAcademicReader members;private final ObjectProvider<DocumentEvidenceReader> documents;
    public AcademicEvidenceService(AcademicRepository repo,AcademicAuthorizationPolicy auth,AcademicEnrollmentService enrollments,AdministrativeTransactions tx,Clock clock,
                                   ActivityEvidenceReader activities,AttendanceAcademicReader attendance,MemberAcademicReader members,ObjectProvider<DocumentEvidenceReader> documents) {
        this.repo=repo;this.auth=auth;this.enrollments=enrollments;this.tx=tx;this.clock=clock;this.activities=activities;this.attendance=attendance;this.members=members;this.documents=documents;
    }
    public static void editable(Enrollment e) {
        if(!Set.of(EnrollmentStatus.DEFERIDA,EnrollmentStatus.EM_ACOMPANHAMENTO,EnrollmentStatus.CALCULADA).contains(e.status())) throw ApiException.conflict("Adesão não permite alteração acadêmica");
    }
    public SourceSnapshot current(UUID actor,Enrollment e,Source source,UUID sourceId) {
        auth.requireRead(actor,e.memberId());members.read(actor,e.memberId());Term t=repo.term(e.termId());var from=t.data().startsAt();var to=t.data().endsAt();
        String scope=actor.equals(e.memberId())?"SELF":"ADMINISTRATIVE";
        if(source==null || sourceId==null) throw ApiException.invalid("Origem obrigatória");
        return switch(source) {
            case ACTIVITY -> { var s=activities.read(actor,e.memberId(),sourceId,from,to);
                yield new SourceSnapshot(1,source,s.id(),e.memberId(),s.projectId(),s.revision(),s.authorId(),null,s.status(),s.occurredAt(),null,from,to,s.attributes(),actor,scope); }
            case ATTENDANCE -> { var s=attendance.read(actor,e.memberId(),sourceId,from,to);
                yield new SourceSnapshot(1,source,s.id(),e.memberId(),null,s.revision(),s.authorId(),s.validatedBy(),s.status(),s.occurredAt(),s.validatedAt(),from,to,s.attributes(),actor,scope); }
            case DOCUMENT -> {
                var provider=documents.getIfAvailable();if(provider==null) throw ApiException.conflict("Integração documental B05 indisponível");
                SourceSnapshot s=provider.read(actor,e.memberId(),sourceId,from,to);
                if(s==null || s.contractVersion()!=1 || s.source()!=source || !sourceId.equals(s.sourceId()) || !e.memberId().equals(s.memberId()) ||
                        !from.equals(s.from()) || !to.equals(s.to()) || !actor.equals(s.readBy()) || s.sourceRevision()==null || s.sourceRevision().isBlank() || s.attributes()==null)
                    throw ApiException.conflict("Contrato documental incompatível");
                yield s;
            }
            case PRESENTATION -> throw ApiException.invalid("Apresentações exigem atestação da coordenação");
        };
    }
    @Transactional public Evidence capture(UUID actor,UUID enrollmentId,CaptureEvidence c,boolean own) {
        tx.lock();auth.requireActive(actor);reason(c.reason());Enrollment e=enrollments.read(actor,enrollmentId,own);editable(e);version(e.version(),c.expectedVersion());
        SourceSnapshot snapshot=current(actor,e,c.source(),c.sourceId());Evidence result=repo.capture(e.id(),actor,snapshot);
        repo.updateEnrollment(e,EnrollmentStatus.EM_ACOMPANHAMENTO,e.disciplines());repo.event(actor,e.id(),"EVIDENCE",result.id(),"CAPTURED",null,result,c.reason());return result;
    }
    @Transactional public Evidence presentation(UUID actor,UUID enrollmentId,Presentation c) {
        tx.lock();auth.requireCoordinator(actor);reason(c.reason());text(c.title(),1000,"Título");text(c.reference(),1000,"Referência");
        Enrollment e=repo.enrollment(enrollmentId);editable(e);version(e.version(),c.expectedVersion());Term t=repo.term(e.termId());
        if(c.occurredAt()==null || c.occurredAt().isBefore(t.data().startsAt()) || !c.occurredAt().isBefore(t.data().endsAt()) || c.occurredAt().isAfter(clock.instant())) throw ApiException.invalid("Data da apresentação inválida");
        var s=new SourceSnapshot(1,Source.PRESENTATION,UUID.randomUUID(),e.memberId(),null,"1",actor,actor,"APPROVED",c.occurredAt(),clock.instant(),t.data().startsAt(),t.data().endsAt(),Map.of("title",c.title(),"reference",c.reference()),actor,"COORDINATOR_ATTESTATION");
        Evidence result=repo.capture(e.id(),actor,s);repo.updateEnrollment(e,EnrollmentStatus.EM_ACOMPANHAMENTO,e.disciplines());repo.event(actor,e.id(),"EVIDENCE",result.id(),"PRESENTATION_ATTESTED",null,result,c.reason());return result;
    }
    public record Comparison(UUID evidenceId,boolean changed,String capturedRevision,String currentRevision) {}
    public Comparison compareCurrent(UUID actor,Evidence evidence) {
        Enrollment e=repo.enrollment(evidence.enrollmentId());auth.requireRead(actor,e.memberId());SourceSnapshot old=evidence.snapshot();
        if(old.source()==Source.PRESENTATION) return new Comparison(evidence.id(),false,old.sourceRevision(),old.sourceRevision());
        SourceSnapshot now=current(actor,e,old.source(),old.sourceId());return new Comparison(evidence.id(),!old.sourceRevision().equals(now.sourceRevision()),old.sourceRevision(),now.sourceRevision());
    }
    public void requireCurrent(UUID actor,Evidence evidence) {
        if(compareCurrent(actor,evidence).changed()) throw ApiException.conflict("Origem alterada; capture nova evidência antes de consolidar");
    }
    @Transactional(readOnly=true) public Comparison compare(UUID actor,UUID id) { auth.requireAdministrator(actor);return compareCurrent(actor,repo.evidence(id)); }
    @Transactional(readOnly=true) public PageResult<Evidence> list(UUID actor,UUID enrollmentId,boolean own,int page,int size) { enrollments.read(actor,enrollmentId,own);return repo.evidence(enrollmentId,page,size); }
}
