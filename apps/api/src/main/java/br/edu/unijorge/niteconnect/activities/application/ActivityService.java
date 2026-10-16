package br.edu.unijorge.niteconnect.activities.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.access.application.ProjectAccess;
import br.edu.unijorge.niteconnect.activities.api.ActivityContracts.*;
import br.edu.unijorge.niteconnect.activities.domain.*;
import br.edu.unijorge.niteconnect.activities.infrastructure.ActivityRepository;
import br.edu.unijorge.niteconnect.activities.infrastructure.ActivityRepository.*;
import br.edu.unijorge.niteconnect.audit.application.AuditTrail;
import br.edu.unijorge.niteconnect.members.application.MemberDirectory;
import br.edu.unijorge.niteconnect.members.domain.MemberStatus;
import br.edu.unijorge.niteconnect.projects.application.ProjectDirectory;
import br.edu.unijorge.niteconnect.projects.domain.ProjectStatus;
import br.edu.unijorge.niteconnect.shared.api.*;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class ActivityService {
    private final ActivityRepository repo; private final ActivityPolicy policy; private final AccessService access;
    private final ProjectAccess projectAccess; private final ProjectDirectory projects; private final MemberDirectory members;
    private final AdministrativeTransactions transactions; private final AuditTrail audit; private final Clock clock;
    private final tools.jackson.databind.ObjectMapper json;
    public ActivityService(ActivityRepository repo,ActivityPolicy policy,AccessService access,ProjectAccess projectAccess,
                           ProjectDirectory projects,MemberDirectory members,AdministrativeTransactions transactions,AuditTrail audit,Clock clock,
                           tools.jackson.databind.ObjectMapper json) {
        this.repo=repo;this.policy=policy;this.access=access;this.projectAccess=projectAccess;this.projects=projects;
        this.members=members;this.transactions=transactions;this.audit=audit;this.clock=clock;
        this.json=json;
    }
    @Transactional public View create(UUID actor,Create c) {
        transactions.lock();access.requireActive(actor);reason(c.reason());
        if(c.projectId()!=null) projectAccess.requireManage(actor,c.projectId());
        else if(!access.isAdministrator(actor) && !actor.equals(c.primaryExecutorId())) throw ApiException.forbidden();
        policy.eligible(c.primaryExecutorId(),c.projectId());
        if(c.projectId()!=null && projects.require(c.projectId()).status()==ProjectStatus.CONCLUIDO) throw ApiException.conflict("Projeto concluído");
        Activity a=repo.create(actor,c.title(),c.description(),c.projectId(),c.dueOn(),c.primaryExecutorId(),c.reason());
        record(actor,a,"CREATED",null,snapshot(a),c.reason());return view(a,LocalDate.now(clock));
    }
    @Transactional(readOnly=true) public View read(UUID actor,UUID id) { Activity a=load(actor,id);policy.read(actor,a);return view(a,LocalDate.now(clock)); }
    @Transactional public View update(UUID actor,UUID id,Update c) {
        Activity a=write(actor,id,c.expectedVersion(),c.reason());policy.manage(actor,a);policy.editable(a);
        if(c.projectId()!=null) throw ApiException.invalid("projectId é imutável");
        repo.update(id,c.title(),c.description(),c.dueOn(),c.expectedVersion());
        Activity next=repo.require(id);record(actor,next,"UPDATED",snapshot(a),snapshot(next),c.reason());return view(next,LocalDate.now(clock));
    }
    @Transactional public View status(UUID actor,UUID id,StatusChange c) {
        Activity a=write(actor,id,c.expectedVersion(),c.reason());
        boolean critical=c.status()==ActivityStatus.CANCELADA || a.status().terminal();
        if(critical) { policy.manage(actor,a);policy.context(a,a.status().terminal());confirm(c.confirmed()); }
        else policy.execute(actor,a);
        if(!a.status().canTransitionTo(c.status())) throw ApiException.conflict("Transição de atividade não permitida");
        repo.status(id,c.status(),c.expectedVersion(),clock.instant());Activity next=repo.require(id);
        record(actor,next,"STATUS_CHANGED",snapshot(a),snapshot(next),c.reason());return view(next,LocalDate.now(clock));
    }
    @Transactional public Assignee assign(UUID actor,UUID id,Assign c,boolean executor) {
        Activity a=write(actor,id,c.expectedVersion(),c.reason());policy.manage(actor,a);policy.editable(a);
        // Personal owners may maintain their own work, never distribute it to other people.
        if(a.projectId()==null) access.requireAdministrator(actor);
        policy.eligible(c.memberId(),a.projectId());
        var previous=repo.assigned(id,c.memberId());
        if(executor) {
            confirm(c.confirmed());
            if(a.primaryExecutorId().equals(c.memberId())) throw ApiException.conflict("Executor já atribuído");
            repo.end(actor,repo.assigned(id,a.primaryExecutorId()).orElseThrow(),c.reason());
            previous.ifPresent(x->repo.end(actor,x,c.reason()));
        } else if(previous.isPresent()) throw ApiException.conflict("Integrante já atribuído");
        Assignment result=repo.assign(actor,id,c.memberId(),executor?"EXECUTOR":"COLLABORATOR",c.reason());
        repo.touch(id,c.expectedVersion());record(actor,repo.require(id),executor?"EXECUTOR_CHANGED":"COLLABORATOR_ADDED",
                executor?a.primaryExecutorId().toString():null,c.memberId().toString(),c.reason());return assignee(result);
    }
    @Transactional public void end(UUID actor,UUID id,UUID member,Command c) {
        Activity a=write(actor,id,c.expectedVersion(),c.reason());policy.manage(actor,a);policy.editable(a);
        if(a.projectId()==null) access.requireAdministrator(actor);
        Assignment x=repo.assigned(id,member).orElseThrow(ApiException::notFound);
        if(x.role().equals("EXECUTOR")) throw ApiException.conflict("Substitua o executor principal");
        repo.end(actor,x,c.reason());repo.touch(id,c.expectedVersion());record(actor,repo.require(id),"COLLABORATOR_REMOVED",member.toString(),null,c.reason());
    }
    @Transactional(readOnly=true) public PageResult<View> list(UUID actor,boolean own,Filter f,int page,int size) {
        access.requireActive(actor);f.validate();LocalDate today=LocalDate.now(clock);
        var result=repo.list(actor,access.isAdministrator(actor),projects.currentProjectIds(actor),own,f,today,page,size);
        var names=members.names(result.items().stream().map(Activity::primaryExecutorId).distinct().toList());
        return new PageResult<>(result.items().stream().map(a->view(a,today,names.get(a.primaryExecutorId()))).toList(),page,size,result.total());
    }
    @Transactional(readOnly=true) public PageResult<View> projectList(UUID actor,UUID project,Filter f,int page,int size) {
        projectAccess.requireRead(actor,project);return list(actor,false,new Filter(f.status(),project,f.executorId(),f.dueFrom(),f.dueTo(),f.overdue()),page,size);
    }
    @Transactional(readOnly=true) public PageResult<Assignee> assignees(UUID actor,UUID id,int page,int size) {
        Activity a=load(actor,id);policy.read(actor,a);var p=repo.assignments(id,false,page,size);
        var names=members.names(p.items().stream().map(Assignment::memberId).toList());
        return new PageResult<>(p.items().stream().map(x->new Assignee(x.id(),x.memberId(),names.get(x.memberId()),x.role(),x.startedAt(),available(x.memberId(),a.projectId()))).toList(),page,size,p.total());
    }
    @Transactional(readOnly=true) public PageResult<HistoricalAssignee> assignmentHistory(UUID actor,UUID id,int page,int size) {
        Activity a=load(actor,id);policy.manage(actor,a);var p=repo.assignments(id,true,page,size);
        var names=members.names(p.items().stream().map(Assignment::memberId).distinct().toList());
        return new PageResult<>(p.items().stream().map(x->new HistoricalAssignee(x.id(),x.activityId(),x.memberId(),names.get(x.memberId()),x.role(),
                x.startedAt(),available(x.memberId(),a.projectId()),x.startedBy(),x.startReason(),x.endedAt(),x.endedBy(),x.endReason())).toList(),page,size,p.total());
    }
    @Transactional(readOnly=true) public PageResult<Event> history(UUID actor,UUID id,int page,int size) { policy.manage(actor,load(actor,id));return repo.history(id,page,size); }
    @Transactional(readOnly=true) public PageResult<PersonalPeriod> personalHistory(UUID actor,int page,int size) { access.requireActive(actor);return repo.personalHistory(actor,page,size); }
    @Transactional(readOnly=true) public PageResult<MemberDirectory.Candidate> candidates(UUID actor,UUID id,UUID project,String query,int page,int size) {
        access.requireActive(actor);List<UUID> excluded=List.of();
        if(id!=null) { Activity a=repo.require(id);policy.manage(actor,a);project=a.projectId();excluded=repo.current(id).stream().map(Assignment::memberId).toList(); }
        if(project!=null) projectAccess.requireManage(actor,project);else access.requireAdministrator(actor);
        return members.activityCandidates(project,query,excluded,page,size);
    }
    public Activity write(UUID actor,UUID id,long version,String reason) {
        transactions.lock();reason(reason);Activity a=load(actor,id);policy.read(actor,a);
        if(a.version()!=version) throw ApiException.conflict("Atividade alterada; consulte a versão atual");return a;
    }
    public void record(UUID actor,Activity a,String action,String before,String after,String reason) {
        repo.event(actor,a.id(),action,a.version(),before,after,reason);
        audit.recordEntity(actor,"ACTIVITY",a.id(),"ACTIVITY_"+action,before,after,reason);
    }
    public static void reason(String reason) { if(reason==null || reason.isBlank() || reason.length()>1000) throw ApiException.invalid("Motivo obrigatório até 1000 caracteres"); }
    public static void confirm(Boolean confirmed) { if(!Boolean.TRUE.equals(confirmed)) throw ApiException.invalid("Confirmação explícita obrigatória"); }
    private Activity load(UUID actor,UUID id) { access.requireActive(actor);return repo.require(id); }
    private String snapshot(Activity a) { return json.writeValueAsString(a); }
    private boolean available(UUID member,UUID project) { return members.require(member).status()==MemberStatus.ATIVO && (project==null || projects.participates(project,member)); }
    private Assignee assignee(Assignment x) { return new Assignee(x.id(),x.memberId(),members.names(List.of(x.memberId())).get(x.memberId()),x.role(),x.startedAt(),available(x.memberId(),repo.require(x.activityId()).projectId())); }
    private View view(Activity a,LocalDate today) {
        return view(a,today,members.names(List.of(a.primaryExecutorId())).get(a.primaryExecutorId()));
    }
    private View view(Activity a,LocalDate today,String executorName) {
        boolean passed=a.dueOn()!=null && a.dueOn().isBefore(today);boolean overdue=a.overdue(today);
        return new View(a.id(),a.title(),a.description(),a.projectId(),a.status(),a.dueOn(),a.primaryExecutorId(),executorName,
                available(a.primaryExecutorId(),a.projectId()),a.createdBy(),a.version(),a.createdAt(),a.updatedAt(),a.completedAt(),passed,overdue,
                overdue && (a.projectId()==null || projects.require(a.projectId()).status()==ProjectStatus.ATIVO));
    }
    public record View(UUID id,String title,String description,UUID projectId,ActivityStatus status,LocalDate dueOn,UUID primaryExecutorId,
                       String primaryExecutorName,boolean executorAvailable,UUID createdBy,long version,Instant createdAt,Instant updatedAt,
                       Instant completedAt,boolean deadlinePassed,boolean overdue,boolean attentionRequired) {}
    public record Assignee(UUID id,UUID memberId,String memberName,String role,Instant startedAt,boolean available) {}
    public record HistoricalAssignee(UUID id,UUID activityId,UUID memberId,String memberName,String role,Instant startedAt,boolean available,
                                     UUID startedBy,String startReason,Instant endedAt,UUID endedBy,String endReason) {}
}
