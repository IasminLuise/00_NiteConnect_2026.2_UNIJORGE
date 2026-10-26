package br.edu.unijorge.niteconnect.documents.application;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import br.edu.unijorge.niteconnect.documents.domain.DocumentRecords.*;
import br.edu.unijorge.niteconnect.documents.infrastructure.DocumentRepository;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import static br.edu.unijorge.niteconnect.documents.infrastructure.DocumentRepository.ts;

@Service
public class DocumentEditorService {
    private final DocumentRepository repo;private final DocumentService documents;private final DocumentEditorProvider provider;
    private final DocumentObjectStore store;private final DocxIntegrity guard;private final Clock clock;private final TransactionTemplate tx;
    private final String publicBase;private final byte[] secret;
    public DocumentEditorService(DocumentRepository repo,DocumentService documents,DocumentEditorProvider provider,DocumentObjectStore store,
            DocxIntegrity guard,Clock clock,PlatformTransactionManager manager,
            @Value("${nite.documents.public-base-url:}") String publicBase,@Value("${nite.documents.capability-secret:}") String secret){
        this.repo=repo;this.documents=documents;this.provider=provider;this.store=store;this.guard=guard;this.clock=clock;this.tx=new TransactionTemplate(manager);
        this.publicBase=publicBase;this.secret=secret.getBytes(StandardCharsets.UTF_8);
    }
    public record Open(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(0) Long expectedVersion,boolean readOnly){}
    public record Session(UUID id,Instant expiresAt,DocumentEditorProvider.EditorConfiguration editor){
        @Override public String toString(){return "Session[id="+id+",editor=REDACTED]";}
    }
    private record Prepared(Cycle cycle,SourceGrant grant){}
    private record CallbackState(Cycle cycle,Doc doc,Version head,UUID author,boolean replay){}
    public record PdfBytes(UUID versionId,byte[] bytes){@Override public String toString(){return "PdfBytes[versionId="+versionId+",bytes=REDACTED]";}}
    private URI base(){
        if(secret.length<32||!provider.enabled()||!store.enabled())throw unavailable();
        try{URI b=URI.create(publicBase);if(!Set.of("http","https").contains(b.getScheme())||b.getHost()==null||b.getRawUserInfo()!=null||b.getRawQuery()!=null||b.getRawFragment()!=null||!(b.getPath().isEmpty()||b.getPath().equals("/")))throw unavailable();return b;}
        catch(IllegalArgumentException e){throw unavailable();}
    }
    public Session open(UUID actor,UUID doc,Open command){
        // Official-template veto is evaluated even if the optional engine is unavailable.
        documents.editorGuard(actor,doc,!command.readOnly());URI base=base();
        Prepared p=tx.execute(status->{
            documents.lock();Doc d=documents.editorGuard(actor,doc,!command.readOnly());DocumentService.version(d.version(),command.expectedVersion());
            repo.update("update nite_connect.document_cycles set status='CLOSED' where document_id=? and status='OPEN' and expires_at<=?",doc,ts(clock.instant()));
            Cycle cycle=repo.activeCycle(doc,command.readOnly()).orElse(null);
            if(cycle!=null&&!cycle.headVersionId().equals(d.currentVersionId())){repo.revokeCycles(doc);cycle=null;}
            if(cycle==null){UUID id=UUID.randomUUID();repo.update("insert into nite_connect.document_cycles(id,document_id,source_version_id,head_version_id,editor_key,actor_id,read_only,status,expires_at) values (?,?,?,?,?,?,?,'OPEN',?)",
                id,doc,d.currentVersionId(),d.currentVersionId(),"nite-"+id,actor,command.readOnly(),ts(clock.instant().plusSeconds(900)));cycle=repo.cycle(id);}
            SourceGrant grant=grant(actor,d.id(),cycle.sourceVersionId(),cycle.id(),cycle.expiresAt());documents.event(actor,doc,"EDITOR_SESSION_REQUESTED",cycle.id(),"Sessão sintética de desenvolvimento");return new Prepared(cycle,grant);
        });
        if(p==null)throw unavailable();Doc d=documents.read(actor,doc);
        var config=provider.open(p.cycle().key(),d.title(),sourceUrl(base,p.grant()),base.resolve("/api/v1/document-provider/cycles/"+p.cycle().id()+"/callback"),command.readOnly(),actor);
        return new Session(p.cycle().id(),p.cycle().expiresAt(),config);
    }
    private SourceGrant grant(UUID actor,UUID doc,UUID version,UUID cycle,Instant expiry){
        UUID id=UUID.randomUUID();repo.update("insert into nite_connect.document_source_grants(id,document_id,version_id,actor_id,cycle_id,expires_at) values (?,?,?,?,?,?)",id,doc,version,actor,cycle,ts(expiry));return repo.sourceGrant(id);
    }
    private URI sourceUrl(URI base,SourceGrant grant){return base.resolve("/api/v1/document-provider/sources/"+grant.id()+"?token="+token(grant));}
    private String token(SourceGrant grant){
        if(secret.length<32)throw unavailable();
        try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(secret,"HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(("source|"+grant.id()+"|"+grant.versionId()+"|"+grant.actor()+"|"+grant.expiresAt().getEpochSecond()).getBytes(StandardCharsets.UTF_8)));}
        catch(GeneralSecurityException e){throw unavailable();}
    }
    public byte[] source(UUID id,String capability){return tx.execute(status->{
        documents.lock();SourceGrant grant=repo.sourceGrant(id);
        if(capability==null||capability.length()>128||!MessageDigest.isEqual(token(grant).getBytes(StandardCharsets.US_ASCII),capability.getBytes(StandardCharsets.US_ASCII))||!grant.expiresAt().isAfter(clock.instant()))throw ApiException.forbidden();
        documents.editorGuard(grant.actor(),grant.documentId(),false);
        if(grant.cycleId()!=null){Cycle cycle=repo.cycle(grant.cycleId());requireOpen(cycle);if(!repo.document(cycle.documentId()).currentVersionId().equals(cycle.headVersionId()))throw ApiException.forbidden();}
        Version v=documents.boundVersion(grant.documentId(),grant.versionId());if(!v.status().equals("AVAILABLE"))throw ApiException.forbidden();return store.read(v.objectId(),v.size(),v.sha256());
    });}
    public void close(UUID actor,UUID doc,UUID id){tx.executeWithoutResult(status->{
        documents.lock();documents.require(actor,doc,true);Cycle cycle=repo.cycle(id);if(!cycle.documentId().equals(doc))throw ApiException.notFound();
        // Any currently authorized editor can revoke the shared cycle; no bytes are discarded.
        repo.update("update nite_connect.document_cycles set status='REVOKED' where id=?",id);documents.event(actor,doc,"EDITOR_SESSION_REVOKED",id,"Encerramento explícito");
    });}
    private void requireOpen(Cycle cycle){if(!cycle.status().equals("OPEN")||!cycle.expiresAt().isAfter(clock.instant()))throw ApiException.forbidden();}
    private static String hash(byte[] bytes){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    public Map<String,Integer> callback(UUID id,String jwt,Map<String,Object> body){
        var callback=provider.verifyCallback(jwt,body);
        String requestHash=hash((callback.key()+"|"+callback.status()+"|"+callback.url()+"|"+callback.users()).getBytes(StandardCharsets.UTF_8));
        CallbackState prepared=tx.execute(status->{
            documents.lock();Cycle c=repo.cycle(id);if(!c.key().equals(callback.key()))throw ApiException.forbidden();
            if(repo.count("select count(*) from nite_connect.document_callback_receipts where cycle_id=? and request_sha256=?",id,requestHash)>0)return new CallbackState(c,null,null,null,true);
            requireOpen(c);Doc d=documents.editorGuard(c.actor(),c.documentId(),!c.readOnly());
            if(!d.currentVersionId().equals(c.headVersionId()))throw ApiException.conflict("Origem da sessão alterada");
            UUID author=c.actor();
            if(Set.of(2,6).contains(callback.status())){
                if(c.readOnly()||callback.url()==null||callback.users().isEmpty())throw ApiException.forbidden();
                for(UUID user:callback.users())documents.require(user,d.id(),true);
                author=callback.users().getLast();
            }
            return new CallbackState(c,d,repo.version(c.headVersionId()),author,false);
        });
        if(prepared==null)throw unavailable();if(prepared.replay())return Map.of("error",0);
        int state=callback.status();
        if(!Set.of(2,6).contains(state)){
            tx.executeWithoutResult(status->{documents.lock();Cycle c=repo.cycle(id);requireOpen(c);documents.require(c.actor(),c.documentId(),!c.readOnly());
                if(state==4||state==3||state==7)repo.update("update nite_connect.document_cycles set status=? where id=?",state==4?"CLOSED":"FAILED",id);
                receipt(id,state,requestHash,requestHash,null);documents.event(c.actor(),c.documentId(),"EDITOR_STATUS_"+state,id,"Notificação assinada do motor");});return Map.of("error",0);
        }
        // Remote I/O has a bounded timeout and takes place outside the database transaction/lock.
        byte[] bytes=provider.download(callback.url(),8*1024*1024);
        var verdict=guard.assess(store.read(prepared.head().objectId(),prepared.head().size(),prepared.head().sha256()),bytes);
        var object=store.put(bytes);
        tx.executeWithoutResult(status->{
            documents.lock();Cycle c=repo.cycle(id);
            if(repo.count("select count(*) from nite_connect.document_callback_receipts where cycle_id=? and request_sha256=?",id,requestHash)>0)return;
            requireOpen(c);Doc d=documents.editorGuard(c.actor(),c.documentId(),true);for(UUID user:callback.users())documents.require(user,d.id(),true);
            boolean current=c.headVersionId().equals(prepared.head().id())&&d.currentVersionId().equals(c.headVersionId());
            boolean accepted=current&&verdict.accepted();String code=!current?"STALE_HEAD":(accepted?null:"OOXML_PROTECTED_CHANGE");
            // A delayed concurrent save is retained in quarantine, never rebased silently onto newer bytes.
            Version v=documents.append(prepared.author(),d,object,prepared.head().id(),accepted,code);
            if(accepted)repo.update("update nite_connect.document_cycles set head_version_id=? where id=?",v.id(),id);
            if(state==2)repo.update("update nite_connect.document_cycles set status='CLOSED' where id=?",id);
            receipt(id,state,object.sha256(),requestHash,v.id());documents.event(prepared.author(),d.id(),accepted?"EDITOR_VERSION_AVAILABLE":"EDITOR_VERSION_QUARANTINED",v.id(),"Candidata persistida; não equivale a aceite editorial");
        });
        // Acknowledges durable receipt, not fidelity approval. Quarantine is visible in version history.
        return Map.of("error",0);
    }
    private void receipt(UUID cycle,int status,String sha,String request,UUID version){repo.update("insert into nite_connect.document_callback_receipts(id,cycle_id,status,sha256,request_sha256,version_id) values (?,?,?,?,?,?) on conflict(cycle_id,request_sha256) do nothing",UUID.randomUUID(),cycle,status,sha,request,version);}
    public PdfBytes pdf(UUID actor,UUID doc,UUID version){
        documents.editorGuard(actor,doc,false);URI base=base();
        SourceGrant grant=tx.execute(status->{documents.lock();documents.editorGuard(actor,doc,false);Version v=documents.boundVersion(doc,version);if(!v.status().equals("AVAILABLE"))throw ApiException.conflict("Versão em quarentena");return grant(actor,doc,version,null,clock.instant().plusSeconds(300));});
        if(grant==null)throw unavailable();var existing=repo.pdf(version);
        if(existing.isPresent()){Pdf p=existing.get();return new PdfBytes(version,store.read(p.objectId(),p.size(),p.sha256()));}
        byte[] pdf=provider.convertPdf("pdf-"+UUID.randomUUID(),sourceUrl(base,grant));
        if(pdf==null||pdf.length<5||!new String(pdf,0,5,StandardCharsets.US_ASCII).equals("%PDF-"))throw new ApiException(HttpStatus.BAD_GATEWAY,"PDF inválido do conversor");
        var object=store.put(pdf);
        return tx.execute(status->{documents.lock();documents.editorGuard(actor,doc,false);documents.boundVersion(doc,version);
            var present=repo.pdf(version);if(present.isEmpty()){repo.update("insert into nite_connect.document_pdfs(id,version_id,object_id,size,sha256,created_by) values (?,?,?,?,?,?)",UUID.randomUUID(),version,object.key(),object.size(),object.sha256(),actor);documents.event(actor,doc,"PDF_CREATED",version,"PDF vinculado à versão imutável");return new PdfBytes(version,pdf);}
            Pdf p=present.get();return new PdfBytes(version,store.read(p.objectId(),p.size(),p.sha256()));});
    }
    private static ApiException unavailable(){return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Editor documental local indisponível");}
}
