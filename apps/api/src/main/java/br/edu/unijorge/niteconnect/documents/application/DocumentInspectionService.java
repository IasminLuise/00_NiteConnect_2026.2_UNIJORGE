package br.edu.unijorge.niteconnect.documents.application;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.documents.infrastructure.DocumentInspectionRepository;
import br.edu.unijorge.niteconnect.documents.infrastructure.DocumentInspectionRepository.Inspection;
import br.edu.unijorge.niteconnect.shared.api.ApiException;

/** Explicit local read-only capability. Quarantine, normal permissions and head are untouched. */
@Service
public class DocumentInspectionService {
    private final DocumentInspectionRepository inspections;
    private final DocumentService documents;
    private final AccessService access;
    private final DocumentObjectStore store;
    private final DocumentEditorProvider provider;
    private final DocxIntegrity guard;
    private final Clock clock;
    private final boolean enabled,localDevelopment;
    private final String bind,publicBase;
    private final byte[] secret;
    public DocumentInspectionService(DocumentInspectionRepository inspections,DocumentService documents,
            AccessService access,DocumentObjectStore store,DocumentEditorProvider provider,DocxIntegrity guard,Clock clock,
            @Value("${nite.documents.inspection.enabled:false}") boolean enabled,
            @Value("${nite.documents.local-development:false}") boolean localDevelopment,
            @Value("${server.address:}") String bind,@Value("${nite.documents.public-base-url:}") String publicBase,
            @Value("${nite.documents.capability-secret:}") String capabilitySecret){
        this.inspections=inspections;this.documents=documents;this.access=access;this.store=store;
        this.provider=provider;this.guard=guard;this.clock=clock;this.enabled=enabled;this.localDevelopment=localDevelopment;
        this.bind=bind;this.publicBase=publicBase;this.secret=capabilitySecret.getBytes(StandardCharsets.UTF_8);
    }
    public record Open(@NotNull UUID versionId,@NotBlank @Size(max=1000) String reason){}
    public record Session(UUID id,UUID versionId,Instant expiresAt,boolean readOnly,DocumentEditorProvider.EditorConfiguration editor){
        @Override public String toString(){return "InspectionSession[id="+id+",editor=REDACTED]";}
    }
    private static boolean loopback(String host){return Set.of("127.0.0.1","localhost","::1","[::1]").contains(host);}
    private URI base(){
        if(!enabled||!localDevelopment||!loopback(bind)||!store.enabled()||!provider.enabled()||secret.length<32)throw unavailable();
        try{
            URI uri=URI.create(publicBase);
            if(!Set.of("http","https").contains(uri.getScheme())||uri.getHost()==null||!loopback(uri.getHost())
                    ||uri.getRawUserInfo()!=null||uri.getRawQuery()!=null||uri.getRawFragment()!=null
                    ||!(uri.getPath().isEmpty()||uri.getPath().equals("/")))throw unavailable();
            return uri;
        }catch(IllegalArgumentException error){throw unavailable();}
    }
    private static ApiException unavailable(){return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Inspeção documental local indisponível");}
    @Transactional
    public Session open(UUID actor,UUID document,Open request){
        URI origin=base();documents.reason(request.reason());documents.lock();access.requireAdministrator(actor);
        var doc=documents.editorGuard(actor,document,false);
        var version=documents.boundVersion(document,request.versionId());
        guard.validate(store.read(version.objectId(),version.size(),version.sha256()));
        Instant expires=clock.instant().plusSeconds(300).truncatedTo(ChronoUnit.SECONDS);
        var inspection=new Inspection(UUID.randomUUID(),document,version.id(),actor,"inspection-"+UUID.randomUUID(),"OPEN",expires,null);
        inspections.insert(inspection);
        URI source=origin.resolve("/api/v1/document-provider/inspection-sources/"+inspection.id()+"?token="+capability(inspection));
        URI callback=origin.resolve("/api/v1/document-provider/inspection-cycles/"+inspection.id()+"/callback");
        var editor=provider.openInspection(inspection.editorKey(),doc.title(),source,callback,actor);
        documents.event(actor,document,"INSPECTION_OPENED",inspection.id(),request.reason());
        return new Session(inspection.id(),version.id(),expires,true,editor);
    }
    private void requireOpen(Inspection item){
        if(!item.status().equals("OPEN")||!item.expiresAt().isAfter(clock.instant()))throw ApiException.forbidden();
    }
    @Transactional
    public br.edu.unijorge.niteconnect.documents.domain.DocumentRecords.VersionView sample(
            UUID actor,UUID document,UUID basedOn,long expected,byte[] bytes,String reason){
        base();documents.reason(reason);documents.lock();access.requireAdministrator(actor);
        var doc=documents.editorGuard(actor,document,false);
        documents.version(doc.version(),expected);documents.boundVersion(document,basedOn);
        guard.validate(bytes);
        var version=documents.append(actor,doc,store.put(bytes),basedOn,false,"INSPECTION_ONLY");
        documents.event(actor,document,"INSPECTION_SAMPLE_RECEIVED",version.id(),reason);
        return br.edu.unijorge.niteconnect.documents.infrastructure.DocumentRepository.view(version);
    }
    private void requireActor(Inspection item){
        access.requireAdministrator(item.actorId());documents.editorGuard(item.actorId(),item.documentId(),false);
    }
    @Transactional
    public byte[] source(UUID id,String token){
        base();documents.lock();var item=inspections.find(id).orElseThrow(ApiException::forbidden);
        if(token==null||!token.matches("[A-Za-z0-9_-]{43}")||!MessageDigest.isEqual(
                capability(item).getBytes(StandardCharsets.US_ASCII),token.getBytes(StandardCharsets.US_ASCII)))throw ApiException.forbidden();
        requireOpen(item);requireActor(item);
        var version=documents.boundVersion(item.documentId(),item.versionId());
        // This role already has the immutable version download privilege. No promotion is performed.
        byte[] bytes=store.read(version.objectId(),version.size(),version.sha256());
        documents.event(item.actorId(),item.documentId(),"INSPECTION_SOURCE_READ",item.id(),"Leitura de versão imutável para inspeção local autorizada");
        return bytes;
    }
    @Transactional
    public void close(UUID actor,UUID document,UUID id){
        base();documents.lock();access.requireAdministrator(actor);documents.editorGuard(actor,document,false);
        var item=inspections.find(id).orElseThrow(ApiException::notFound);
        if(!item.documentId().equals(document))throw ApiException.notFound();
        if(!item.status().equals("REVOKED")){
            inspections.state(id,"REVOKED",item.lastProviderStatus());
            documents.event(actor,document,"INSPECTION_REVOKED",id,"Encerramento explícito da inspeção local");
        }
    }
    @Transactional
    public Map<String,Integer> callback(UUID id,String jwt,Map<String,Object> body){
        base();var notification=provider.verifyCallback(jwt,body);
        if(!Set.of(1,3,4,7).contains(notification.status()))throw ApiException.forbidden();
        documents.lock();var item=inspections.find(id).orElseThrow(ApiException::forbidden);
        if(!item.editorKey().equals(notification.key()))throw ApiException.forbidden();
        requireActor(item);
        if(Objects.equals(item.lastProviderStatus(),notification.status()))return Map.of("error",0);
        requireOpen(item);
        String next=switch(notification.status()){case 4->"CLOSED";case 3,7->"FAILED";default->"OPEN";};
        inspections.state(id,next,notification.status());
        documents.event(item.actorId(),item.documentId(),"INSPECTION_STATUS_"+notification.status(),id,"Notificação assinada da inspeção somente leitura");
        return Map.of("error",0);
    }
    private String capability(Inspection item){
        try{
            var hmac=Mac.getInstance("HmacSHA256");hmac.init(new SecretKeySpec(secret,"HmacSHA256"));
            String payload="inspection|"+item.id()+"|"+item.documentId()+"|"+item.versionId()+"|"+item.actorId()+"|"+item.expiresAt().getEpochSecond();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        }catch(java.security.GeneralSecurityException error){throw unavailable();}
    }
}
