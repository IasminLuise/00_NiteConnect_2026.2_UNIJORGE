package br.edu.unijorge.niteconnect.documents.application;

import java.time.*;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.audit.application.AuditTrail;
import br.edu.unijorge.niteconnect.members.application.MemberDirectory;
import br.edu.unijorge.niteconnect.projects.application.ProjectDirectory;
import br.edu.unijorge.niteconnect.attendance.application.AgendaService;
import br.edu.unijorge.niteconnect.shared.security.AdministrativeTransactions;
import br.edu.unijorge.niteconnect.shared.api.*;
import br.edu.unijorge.niteconnect.documents.domain.DocumentRecords.*;
import br.edu.unijorge.niteconnect.documents.infrastructure.DocumentRepository;
import static br.edu.unijorge.niteconnect.documents.infrastructure.DocumentRepository.ts;

@Service
public class DocumentService {
    private final DocumentRepository repo;private final AccessService access;private final MemberDirectory members;
    private final ProjectDirectory projects;private final AgendaService agenda;private final AdministrativeTransactions transactions;
    private final AuditTrail audit;private final DocumentObjectStore store;private final DocxIntegrity guard;private final Clock clock;
    public DocumentService(DocumentRepository repo,AccessService access,MemberDirectory members,ProjectDirectory projects,AgendaService agenda,
            AdministrativeTransactions transactions,AuditTrail audit,DocumentObjectStore store,DocxIntegrity guard,Clock clock){
        this.repo=repo;this.access=access;this.members=members;this.projects=projects;this.agenda=agenda;
        this.transactions=transactions;this.audit=audit;this.store=store;this.guard=guard;this.clock=clock;
    }
    public record CreateTemplate(@NotBlank @Size(max=200) String title,@NotNull Boolean official,@NotBlank @Size(max=1000) String reason){}
    public record CreateDocument(String title,String context,UUID ownerId,UUID projectId,UUID eventId,String demandReference,UUID templateVersionId,String reason){}
    public record Command(@NotNull @Min(0) Long expectedVersion,String reason,Boolean confirmed){}
    public record GrantCommand(@NotNull UUID memberId,boolean canEdit,@NotNull @Min(0) Long expectedVersion,String reason){}
    public record Restore(@NotNull UUID versionId,@NotNull @Min(0) Long expectedVersion,String reason,Boolean confirmed){}
    public record TemplateVersionView(UUID id,int revision,UUID author,Instant createdAt){}
    public static void text(String value,int max,String field){if(value==null||value.isBlank()||value.length()>max||value.indexOf('\0')>=0)throw ApiException.invalid(field+" inválido");}
    public static void reason(String value){text(value,1000,"Motivo");}
    public static void version(long actual,long expected){if(expected<0||actual!=expected)throw ApiException.conflict("Documento alterado; consulte a versão atual");}
    public void requireStore(){if(!store.enabled())throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Armazenamento documental local indisponível");}
    public void lock(){transactions.lock();}
    @Transactional public Template createTemplate(UUID actor,CreateTemplate c){
        lock();access.requireAdministrator(actor);text(c.title(),200,"Título");reason(c.reason());UUID id=UUID.randomUUID();
        repo.update("insert into nite_connect.document_templates(id,title,official,created_by) values (?,?,?,?)",id,c.title().strip(),c.official(),actor);
        audit.recordEntity(actor,"DOCUMENT_TEMPLATE",id,"CREATED",null,"ACTIVE",c.reason());return repo.template(id);
    }
    @Transactional(readOnly=true) public PageResult<Template> templates(UUID actor,int page,int size){access.requireAdministrator(actor);return repo.templates(page,size);}
    @Transactional(readOnly=true) public PageResult<TemplateVersionView> templateVersions(UUID actor,UUID id,int page,int size){
        access.requireAdministrator(actor);repo.template(id);var result=repo.templateVersions(id,page,size);
        return new PageResult<>(result.items().stream().map(v->new TemplateVersionView(v.id(),v.revision(),v.author(),v.createdAt())).toList(),result.page(),result.size(),result.total());
    }
    @Transactional public TemplateVersionView templateVersion(UUID actor,UUID id,long expected,byte[] bytes,String reason){
        lock();access.requireAdministrator(actor);reason(reason);requireStore();Template t=repo.template(id);version(t.version(),expected);
        if(t.retiredAt()!=null)throw ApiException.conflict("Modelo retirado");guard.validate(bytes);var object=store.put(bytes);UUID v=UUID.randomUUID();
        int revision=(int)repo.count("select coalesce(max(revision),0)+1 from nite_connect.document_template_versions where template_id=?",id);
        repo.update("insert into nite_connect.document_template_versions(id,template_id,revision,object_id,size,sha256,created_by) values (?,?,?,?,?,?,?)",v,id,revision,object.key(),object.size(),object.sha256(),actor);
        repo.update("update nite_connect.document_templates set version=version+1 where id=?",id);audit.recordEntity(actor,"DOCUMENT_TEMPLATE",id,"VERSION_CREATED",null,v.toString(),reason);
        var result=repo.templateVersion(v);return new TemplateVersionView(v,revision,actor,result.createdAt());
    }
    @Transactional public Template retireTemplate(UUID actor,UUID id,Command c){
        lock();access.requireAdministrator(actor);reason(c.reason());confirmed(c.confirmed());Template t=repo.template(id);version(t.version(),c.expectedVersion());
        repo.update("update nite_connect.document_templates set retired_at=?,version=version+1 where id=?",ts(clock.instant()),id);
        audit.recordEntity(actor,"DOCUMENT_TEMPLATE",id,"RETIRED",null,"RETIRED",c.reason());return repo.template(id);
    }
    @Transactional public Doc create(UUID actor,CreateDocument c){
        lock();access.requireAdministrator(actor);reason(c.reason());text(c.title(),200,"Título");requireStore();
        if(c.templateVersionId()==null||!Set.of("PERSONAL","PROJECT","EVENT","ADMINISTRATIVE").contains(Objects.toString(c.context(),"")))throw ApiException.invalid("Contexto e versão de modelo obrigatórios");
        if(c.demandReference()!=null)text(c.demandReference(),200,"Referência da demanda");
        switch(c.context()){
            case "PERSONAL" -> {if(c.ownerId()==null||c.projectId()!=null||c.eventId()!=null)throw ApiException.invalid("Contexto pessoal inválido");access.requireActive(c.ownerId());}
            case "PROJECT" -> {if(c.projectId()==null||c.ownerId()!=null||c.eventId()!=null)throw ApiException.invalid("Contexto de projeto inválido");projects.require(c.projectId());}
            case "EVENT" -> {if(c.eventId()==null||c.ownerId()!=null||c.projectId()!=null)throw ApiException.invalid("Contexto de evento inválido");agenda.read(actor,c.eventId());}
            default -> {if(c.ownerId()!=null||c.projectId()!=null||c.eventId()!=null)throw ApiException.invalid("Contexto administrativo inválido");}
        }
        TemplateVersion source=repo.templateVersion(c.templateVersionId());Template model=repo.template(source.templateId());if(model.retiredAt()!=null)throw ApiException.conflict("Modelo retirado");
        // Read and validate before publishing a reference. Storage paths are never client input.
        guard.validate(store.read(source.objectId(),source.size(),source.sha256()));
        UUID id=UUID.randomUUID(),initial=UUID.randomUUID();
        repo.update("insert into nite_connect.documents(id,title,context,owner_id,project_id,event_id,demand_reference,template_version_id,official,created_by) values (?,?,?,?,?,?,?,?,?,?)",
            id,c.title().strip(),c.context(),c.ownerId(),c.projectId(),c.eventId(),c.demandReference(),source.id(),model.official(),actor);
        repo.update("insert into nite_connect.document_versions(id,document_id,revision,object_id,size,sha256,status,created_by) values (?,?,1,?,?,?,'AVAILABLE',?)",initial,id,source.objectId(),source.size(),source.sha256(),actor);
        repo.update("update nite_connect.documents set current_version_id=? where id=?",initial,id);event(actor,id,"CREATED",initial,c.reason());return repo.document(id);
    }
    public void authorize(UUID actor,Doc d,boolean write){
        access.requireActive(actor);if(access.isAdministrator(actor))return;
        boolean projectMember=d.projectId()==null||projects.participates(d.projectId(),actor);
        boolean leader=d.projectId()!=null&&projectMember&&actor.equals(projects.leader(d.projectId()));
        if(!projectMember||!(actor.equals(d.ownerId())||leader||repo.grant(d.id(),actor,write)))throw ApiException.forbidden();
    }
    public Doc require(UUID actor,UUID id,boolean write){Doc d=repo.document(id);authorize(actor,d,write);return d;}
    public Doc editorGuard(UUID actor,UUID id,boolean write){
        Doc d=require(actor,id,write);if(d.official())throw ApiException.conflict("Edição e conversão de modelos oficiais bloqueadas pelo gate de fidelidade");
        if(d.archivedAt()!=null)throw ApiException.conflict("Documento arquivado");return d;
    }
    @Transactional(readOnly=true) public Doc read(UUID actor,UUID id){return require(actor,id,false);}
    @Transactional(readOnly=true) public PageResult<Doc> list(UUID actor,int page,int size){
        access.requireActive(actor);boolean admin=access.isAdministrator(actor);var current=admin?List.<UUID>of():projects.currentProjectIds(actor);
        var leads=current.stream().filter(id->actor.equals(projects.leader(id))).toList();return repo.list(actor,admin,leads,current,page,size);
    }
    @Transactional(readOnly=true) public PageResult<VersionView> versions(UUID actor,UUID id,int page,int size){require(actor,id,false);return repo.versions(id,page,size);}
    @Transactional(readOnly=true) public PageResult<Event> history(UUID actor,UUID id,int page,int size){access.requireAdministrator(actor);require(actor,id,false);return repo.events(id,page,size);}
    public Version boundVersion(UUID document,UUID id){Version v=repo.version(id);if(!v.documentId().equals(document))throw ApiException.notFound();return v;}
    @Transactional public byte[] download(UUID actor,UUID id,UUID version){
        require(actor,id,false);Version v=boundVersion(id,version);if(!v.status().equals("AVAILABLE")&&!access.isAdministrator(actor))throw ApiException.forbidden();
        byte[] bytes=store.read(v.objectId(),v.size(),v.sha256());event(actor,id,"DOWNLOADED",version,"Leitura autorizada de versão imutável");return bytes;
    }
    @Transactional public VersionView upload(UUID actor,UUID id,long expected,byte[] bytes,String reason){
        lock();reason(reason);Doc d=editorGuard(actor,id,true);version(d.version(),expected);requireStore();
        Version current=boundVersion(id,d.currentVersionId());var verdict=guard.assess(store.read(current.objectId(),current.size(),current.sha256()),bytes);var object=store.put(bytes);
        Version result=append(actor,d,object,current.id(),verdict.accepted(),verdict.accepted()?null:"OOXML_PROTECTED_CHANGE");repo.revokeCycles(id);event(actor,id,"VERSION_RECEIVED",result.id(),reason);return DocumentRepository.view(result);
    }
    /** Must run under lock and transaction; bytes already exist immutably before the DB reference commits. */
    public Version append(UUID actor,Doc d,DocumentObjectStore.Stored object,UUID basedOn,boolean accepted,String code){
        UUID id=UUID.randomUUID();repo.update("insert into nite_connect.document_versions(id,document_id,revision,object_id,size,sha256,status,based_on,rejection_code,created_by) values (?,?,?,?,?,?,?,?,?,?)",
            id,d.id(),repo.nextRevision(d.id()),object.key(),object.size(),object.sha256(),accepted?"AVAILABLE":"QUARANTINED",basedOn,code,actor);
        if(repo.update("update nite_connect.documents set current_version_id=?,version=version+1 where id=? and version=?",accepted?id:d.currentVersionId(),d.id(),d.version())!=1)throw ApiException.conflict("Documento alterado");
        return repo.version(id);
    }
    @Transactional public Doc grant(UUID actor,UUID id,GrantCommand c){
        lock();access.requireAdministrator(actor);Doc d=require(actor,id,true);version(d.version(),c.expectedVersion());reason(c.reason());access.requireActive(c.memberId());
        if(d.projectId()!=null&&!projects.participates(d.projectId(),c.memberId()))throw ApiException.invalid("Destinatário sem vínculo ativo ao projeto");
        repo.update("update nite_connect.document_grants set revoked_at=? where document_id=? and member_id=? and revoked_at is null",ts(clock.instant()),id,c.memberId());
        UUID grant=UUID.randomUUID();repo.update("insert into nite_connect.document_grants(id,document_id,member_id,can_edit,granted_by) values (?,?,?,?,?)",grant,id,c.memberId(),c.canEdit(),actor);
        touch(d);repo.revokeCycles(id);event(actor,id,"ACCESS_GRANTED",grant,c.reason());return repo.document(id);
    }
    @Transactional public Doc revoke(UUID actor,UUID id,UUID member,Command c){
        lock();access.requireAdministrator(actor);Doc d=require(actor,id,true);version(d.version(),c.expectedVersion());reason(c.reason());
        int changed=repo.update("update nite_connect.document_grants set revoked_at=? where document_id=? and member_id=? and revoked_at is null",ts(clock.instant()),id,member);
        if(changed==0)throw ApiException.notFound();touch(d);repo.revokeCycles(id);event(actor,id,"ACCESS_REVOKED",member,c.reason());return repo.document(id);
    }
    @Transactional public Doc archive(UUID actor,UUID id,Command c){
        lock();access.requireAdministrator(actor);Doc d=require(actor,id,true);version(d.version(),c.expectedVersion());reason(c.reason());confirmed(c.confirmed());
        repo.update("update nite_connect.documents set archived_at=?,version=version+1 where id=?",ts(clock.instant()),id);repo.revokeCycles(id);event(actor,id,"ARCHIVED",null,c.reason());return repo.document(id);
    }
    @Transactional public Doc restore(UUID actor,UUID id,Restore c){
        lock();access.requireAdministrator(actor);Doc d=require(actor,id,true);version(d.version(),c.expectedVersion());reason(c.reason());confirmed(c.confirmed());Version v=boundVersion(id,c.versionId());
        if(!v.status().equals("AVAILABLE"))throw ApiException.conflict("Candidata em quarentena não pode ser publicada");store.read(v.objectId(),v.size(),v.sha256());
        repo.update("update nite_connect.documents set current_version_id=?,version=version+1 where id=?",v.id(),id);repo.revokeCycles(id);event(actor,id,"VERSION_RESTORED",v.id(),c.reason());return repo.document(id);
    }
    public void event(UUID actor,UUID doc,String action,UUID ref,String reason){repo.event(actor,doc,action,ref,reason);audit.recordEntity(actor,"DOCUMENT",doc,action,null,ref==null?null:ref.toString(),reason);}
    private void touch(Doc d){if(repo.update("update nite_connect.documents set version=version+1 where id=? and version=?",d.id(),d.version())!=1)throw ApiException.conflict("Documento alterado");}
    private static void confirmed(Boolean c){if(!Boolean.TRUE.equals(c))throw ApiException.invalid("Confirmação obrigatória");}
}
