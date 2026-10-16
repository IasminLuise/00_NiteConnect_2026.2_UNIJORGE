package br.edu.unijorge.niteconnect.activities.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.activities.api.ActivityContracts.Command;
import br.edu.unijorge.niteconnect.activities.api.EvidenceContracts.*;
import br.edu.unijorge.niteconnect.activities.domain.Activity;
import br.edu.unijorge.niteconnect.activities.infrastructure.ActivityRepository;
import br.edu.unijorge.niteconnect.activities.infrastructure.EvidenceRepository;
import br.edu.unijorge.niteconnect.activities.infrastructure.EvidenceRepository.*;
import br.edu.unijorge.niteconnect.audit.application.AuditTrail;
import br.edu.unijorge.niteconnect.members.application.MemberDirectory;
import br.edu.unijorge.niteconnect.projects.application.ProjectDirectory;
import br.edu.unijorge.niteconnect.shared.api.*;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.*;
import java.time.Instant;
import java.util.*;

@Service
public class EvidenceService {
    private final ActivityService activities; private final ActivityRepository activityRepo; private final ActivityPolicy policy;
    private final EvidenceRepository repo; private final MemberDirectory members; private final AccessService access;
    private final ProjectDirectory projects; private final AuditTrail audit; private final AdministrativeTransactions transactions;
    public EvidenceService(ActivityService activities,ActivityRepository activityRepo,ActivityPolicy policy,EvidenceRepository repo,
                           MemberDirectory members,AccessService access,ProjectDirectory projects,AuditTrail audit,AdministrativeTransactions transactions) {
        this.activities=activities;this.activityRepo=activityRepo;this.policy=policy;this.repo=repo;this.members=members;
        this.access=access;this.projects=projects;this.audit=audit;this.transactions=transactions;
    }
    @Transactional public View create(UUID actor,UUID id,Create c) {
        Activity a=activities.write(actor,id,c.expectedVersion(),c.reason());policy.contribute(actor,a);
        String content=c.content()==null?null:c.content().strip();String url=c.url();
        if(c.type()==Type.LINK) { validateUrl(url); }
        else if(content==null || content.isBlank() || url!=null) throw ApiException.invalid("Conteúdo obrigatório e URL incompatível com o tipo");
        correction(id,c.correctsEvidenceId());
        Evidence e=repo.create(actor,id,assignment(actor,id),c.type().name(),content,url,c.correctsEvidenceId(),null);
        changed(actor,a,e,"CREATED",c.reason());return view(e,canInspectWithdrawn(actor,a));
    }
    @Transactional public View withdraw(UUID actor,UUID id,UUID evidence,Command c) {
        Activity a=activities.write(actor,id,c.expectedVersion(),c.reason());policy.context(a,false);ActivityService.confirm(c.confirmed());
        Evidence e=inActivity(id,evidence);
        if(!policy.manages(actor,a)) {
            policy.contribute(actor,a);if(!actor.equals(e.authorId())) throw ApiException.forbidden();
        }
        repo.withdraw(actor,evidence,c.reason());changed(actor,a,repo.require(evidence),"WITHDRAWN",c.reason());
        return view(repo.require(evidence),canInspectWithdrawn(actor,a));
    }
    @Transactional(readOnly=true) public View read(UUID actor,UUID activity,UUID evidence) {
        access.requireActive(actor);Activity a=activityRepo.require(activity);policy.read(actor,a);return view(inActivity(activity,evidence),canInspectWithdrawn(actor,a));
    }
    @Transactional(readOnly=true) public PageResult<View> list(UUID actor,UUID id,int page,int size) {
        access.requireActive(actor);Activity a=activityRepo.require(id);policy.read(actor,a);var p=repo.list(id,null,page,size);boolean full=canInspectWithdrawn(actor,a);
        return views(p,full);
    }
    @Transactional(readOnly=true) public PageResult<View> personalList(UUID actor,int page,int size) {
        access.requireActive(actor);return views(repo.list(null,actor,page,size),false);
    }
    @Transactional(readOnly=true) public View personal(UUID actor,UUID id) {
        access.requireActive(actor);Evidence e=repo.require(id);if(!actor.equals(e.authorId())) throw ApiException.notFound();return view(e,false);
    }
    @Transactional(readOnly=true) public Download download(UUID actor,UUID activity,UUID evidence,boolean personal) {
        access.requireActive(actor);Evidence e=repo.require(evidence);boolean full=false;
        if(personal) { if(!e.authorId().equals(actor)) throw ApiException.notFound(); }
        else { Activity a=activityRepo.require(activity);policy.read(actor,a);inActivity(activity,evidence);full=canInspectWithdrawn(actor,a); }
        if(e.objectId()==null || !"AVAILABLE".equals(e.objectState()) || e.withdrawnAt()!=null && !full) throw ApiException.notFound();
        return new Download(e.objectKey(),e.filename(),e.contentType(),e.size(),e.sha256());
    }
    /** First short transaction. Bytes are validated and written only after this returns. */
    @Transactional public Receipt prepare(UUID actor,UUID activity,Upload c,String filename,boolean enabled) {
        Activity a=activities.write(actor,activity,c.expectedVersion(),c.reason());policy.contribute(actor,a);correction(activity,c.correctsEvidenceId());
        if(!enabled) throw new ApiException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"Armazenamento privado indisponível");
        return repo.begin(actor,activity,filename);
    }
    /** Second transaction revalidates revocation, participation, project state and the original version. */
    @Transactional public View finish(UUID actor,UUID activity,Upload c,UUID receipt,EvidenceObjectStore.Stored stored) {
        Activity a=activities.write(actor,activity,c.expectedVersion(),c.reason());policy.contribute(actor,a);correction(activity,c.correctsEvidenceId());
        Receipt r=repo.receipt(receipt);
        if(!r.activityId().equals(activity) || !r.authorId().equals(actor)) throw ApiException.notFound();
        repo.available(receipt,stored);
        Evidence e=repo.create(actor,activity,assignment(actor,activity),stored.evidenceType(),null,null,c.correctsEvidenceId(),receipt);
        changed(actor,a,e,"CREATED",c.reason());return view(e,canInspectWithdrawn(actor,a));
    }
    @Transactional public boolean fail(UUID receipt) { transactions.lock();return repo.fail(receipt); }
    @Transactional(readOnly=true) public List<UUID> stalePendingKeys(UUID actor,Instant cutoff) {
        access.requireAdministrator(actor);return repo.stalePendingKeys(cutoff);
    }
    @Transactional public boolean claimOrphan(UUID actor,UUID key,Instant cutoff,boolean dryRun) {
        transactions.lock();access.requireAdministrator(actor);var receipt=repo.byKey(key);
        if(receipt.isEmpty()) return true;
        Receipt r=receipt.get();if(r.state().equals("AVAILABLE") || !r.updatedAt().isBefore(cutoff)) return false;
        if(!dryRun && r.state().equals("PENDING")) repo.fail(r.id());return true;
    }
    private boolean canInspectWithdrawn(UUID actor,Activity a) {
        return access.isAdministrator(actor) || a.projectId()!=null && actor.equals(projects.leader(a.projectId()));
    }
    private void correction(UUID activity,UUID correction) { if(correction!=null) inActivity(activity,correction); }
    private Evidence inActivity(UUID activity,UUID id) { Evidence e=repo.require(id);if(!e.activityId().equals(activity)) throw ApiException.notFound();return e; }
    private UUID assignment(UUID actor,UUID activity) { return activityRepo.assigned(activity,actor).map(ActivityRepository.Assignment::id).orElse(null); }
    private void changed(UUID actor,Activity before,Evidence e,String action,String reason) {
        activityRepo.touch(before.id(),before.version());Activity after=activityRepo.require(before.id());
        activities.record(actor,after,"EVIDENCE_"+action,null,e.id().toString(),reason);
        audit.recordEntity(actor,"EVIDENCE",e.id(),"EVIDENCE_"+action,action.equals("WITHDRAWN")?"AVAILABLE":null,
                action.equals("WITHDRAWN")?"WITHDRAWN":"AVAILABLE",reason);
    }
    private PageResult<View> views(PageResult<Evidence> p,boolean full) {
        var names=members.names(p.items().stream().map(Evidence::authorId).toList());
        return new PageResult<>(p.items().stream().map(e->view(e,full,names.get(e.authorId()))).toList(),p.page(),p.size(),p.total());
    }
    private View view(Evidence e,boolean full) { return view(e,full,members.names(List.of(e.authorId())).get(e.authorId())); }
    private View view(Evidence e,boolean full,String name) {
        boolean show=e.withdrawnAt()==null || full;
        return new View(e.id(),e.activityId(),e.authorId(),name,e.assignmentId(),e.type(),show?e.content():null,show?e.url():null,
                e.correctsEvidenceId(),e.createdAt(),e.withdrawnAt(),show && e.objectId()!=null?new ObjectInfo(e.filename(),e.contentType(),e.size(),e.sha256()):null);
    }
    private static void validateUrl(String url) {
        if(url==null || url.length()>2048 || url.chars().anyMatch(c->Character.isISOControl(c) || Character.isWhitespace(c))) throw ApiException.invalid("URL inválida");
        try {
            URI uri=new URI(url);
            if(!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost()==null || uri.getRawUserInfo()!=null) throw ApiException.invalid("URL HTTP(S) absoluta e sem credenciais obrigatória");
        } catch(URISyntaxException e) { throw ApiException.invalid("URL inválida"); }
    }
    public record ObjectInfo(String filename,String contentType,long size,String sha256) {}
    public record View(UUID id,UUID activityId,UUID authorId,String authorName,UUID assignmentId,String type,String content,String url,
                       UUID correctsEvidenceId,Instant createdAt,Instant withdrawnAt,ObjectInfo object) {}
    public record Download(UUID key,String filename,String contentType,long size,String sha256) {}
}
