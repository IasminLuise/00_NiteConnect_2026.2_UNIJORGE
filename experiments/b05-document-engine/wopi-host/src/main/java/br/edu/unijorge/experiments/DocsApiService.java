package br.edu.unijorge.experiments;
import java.nio.file.*;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.io.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
final class DocsApiService {
    private final Path root,fixtures;private final DocsJwt jwt;private final DocsFetch fetch;
    private final String external,internal,editor;
    private final Map<UUID,ObjectNode> works=new HashMap<>();
    private final Map<UUID,CandidateGate> gates=new HashMap<>();
    private final Map<UUID,DocumentStore.Session> grants=new HashMap<>();
    private final DocumentStore store;
    @FunctionalInterface interface StateWriter {void write(Path target,byte[] bytes);}
    private final StateWriter stateWriter;
    DocsApiService(Path root,Path fixtures,DocsJwt jwt,DocsFetch fetch,String external,String internal,String editor)throws IOException {
        this(root,fixtures,jwt,fetch,external,internal,editor,DocsApiService::durable);
    }
    DocsApiService(Path root,Path fixtures,DocsJwt jwt,DocsFetch fetch,String external,String internal,String editor,StateWriter stateWriter)throws IOException {
        this.stateWriter=stateWriter;
        this.root=root.toAbsolutePath().normalize();this.fixtures=fixtures;this.jwt=jwt;this.fetch=fetch;this.external=external;this.internal=internal;this.editor=editor;
        Files.createDirectories(this.root.resolve("sessions"));Files.createDirectories(this.root.resolve("owners"));Files.createDirectories(this.root.resolve("quarantine"));
        store=new DocumentStore(this.root.resolve("versions"),Clock.systemUTC(),(id,data,number)->{var gate=gates.get(id);if(gate==null)throw new DocumentStore.Failure(500,"Política indisponível");gate.validate(id,data,number);});
        try(var files=Files.list(this.root.resolve("sessions"))) {
            for(var file:files.filter(p->p.toString().endsWith(".json")).sorted().toList()) {
                var node=DocsJwt.JSON.readTree(Files.readAllBytes(file));
                if(!(node instanceof ObjectNode object))throw new IOException("Invalid private session state");
                UUID id=UUID.fromString(object.path("id").asString());works.put(id,object);register(object);
            }
        }
    }
    private static String random(){byte[] value=new byte[32];new SecureRandom().nextBytes(value);return Base64.getUrlEncoder().withoutPadding().encodeToString(value);}
    static String checksum(byte[] data){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
    private static void fail(int code,String reason){throw new DocumentStore.Failure(code,reason);}
    private UUID document(ObjectNode work){return UUID.fromString(work.path("document").asString());}
    private DocumentStore.Session grant(ObjectNode work) {
        UUID id=UUID.fromString(work.path("id").asString());
        return grants.computeIfAbsent(id,key->store.issue(document(work),work.path("writable").asBoolean(),Duration.ofSeconds(Math.max(1,Math.min(3600,work.path("expires").asLong()-Instant.now().getEpochSecond())))));
    }
    private void register(ObjectNode work)throws IOException {
        String alias=work.path("alias").asString();if(!alias.matches("F0[1-9]\\.docx"))throw new IOException("Invalid private alias");
        CandidateGate gate=switch(work.path("mode").asString()) {
            case "structural"->(id,data,number)->{};
            case "strict","rebase"->new ScriptedDocxGate(Map.of(alias,List.of("XYZ","")));
            case "reject"->(id,data,number)->{throw new DocumentStore.Failure(500,"INDUCED_REJECTION");};
            default->throw new IOException("Invalid private policy");
        };
        byte[] original=Files.readAllBytes(fixtures.resolve(alias));DocxValidator.validate(original);
        UUID doc=document(work);gate.register(doc,alias,original);gates.put(doc,gate);store.seed(doc,alias,original);
    }
    synchronized JsonNode create(String alias,String mode,boolean writable) {
        if(!alias.matches("F0[1-9]\\.docx")||!Set.of("structural","strict","rebase","reject").contains(mode)||works.size()>=32)fail(400,"Caso de laboratório inválido");
        var work=DocsJwt.JSON.createObjectNode();UUID id=UUID.randomUUID();
        work.put("id",id.toString());work.put("document",UUID.randomUUID().toString());work.put("alias",alias);work.put("mode",mode);work.put("writable",writable);
        work.put("key","b0503-"+UUID.randomUUID());work.put("cap",random());work.put("expires",Instant.now().plusSeconds(3600).getEpochSecond());
        work.put("state","editing-local");work.put("closed",false);work.put("commits",0);work.put("lastHash","");work.put("dirtyEpoch",0);work.put("forceEpoch",-1);work.put("pendingForce","");work.set("receipts",DocsJwt.JSON.createObjectNode());
        try{register(work);}catch(IOException e){fail(503,"Fixture indisponível");}
        var initial=store.info(document(work),grant(work).token());work.put("sourceVersion",initial.version());work.put("headVersion",initial.version());
        works.put(id,work);active(work);persist(work);return work.deepCopy();
    }
    private void active(ObjectNode work){durable(root.resolve("owners").resolve(document(work).toString()),work.path("key").asString().getBytes(StandardCharsets.UTF_8));}
    private boolean isActive(ObjectNode work){try{return Files.readString(root.resolve("owners").resolve(document(work).toString())).equals(work.path("key").asString());}catch(IOException e){fail(503,"Estado indisponível");return false;}}
    private ObjectNode work(UUID id,String cap) {
        var value=works.get(id);
        if(value==null||cap==null||!MessageDigest.isEqual(value.path("cap").asString().getBytes(StandardCharsets.US_ASCII),cap.getBytes(StandardCharsets.US_ASCII))||Instant.now().getEpochSecond()>=value.path("expires").asLong())fail(401,"Sessão inválida");
        return value;
    }
    synchronized JsonNode reopen(UUID id,String cap){
        var previous=work(id,cap);if(!previous.path("closed").asBoolean()||!isActive(previous))fail(409,"Sessão não finalizada ou chave obsoleta");
        if(works.size()>=32)fail(400,"Limite de sessões");
        var next=previous.deepCopy();UUID nextId=UUID.randomUUID();next.put("id",nextId.toString());next.put("key","b0503-"+UUID.randomUUID());next.put("cap",random());next.put("expires",Instant.now().plusSeconds(3600).getEpochSecond());next.put("state","editing-local");next.put("closed",false);next.put("commits",0);next.put("dirtyEpoch",0);next.put("pendingForce","");next.set("receipts",DocsJwt.JSON.createObjectNode());
        for(String name:List.of("candidateHash","reason","lastStatus"))next.remove(name);next.put("forceEpoch",-1);
        next.put("sourceVersion",store.currentVersion(document(previous)));next.put("headVersion",store.currentVersion(document(previous)));next.put("lastHash","");works.put(nextId,next);active(next);persist(next);return next.deepCopy();
    }
    synchronized JsonNode state(UUID id,String cap){
        var work=work(id,cap);var result=DocsJwt.JSON.createObjectNode();
        for(String name:List.of("state","headVersion","commits","lastHash","lastStatus","closed","reason","candidateHash","dirtyEpoch","pendingForce"))if(work.has(name))result.set(name,work.get(name));
        result.put("canRecover",work.has("candidateHash"));return result;
    }
    synchronized JsonNode config(UUID id,String cap){
        var work=work(id,cap);if(work.path("closed").asBoolean()||!isActive(work))fail(409,"Sessão finalizada");
        String capability=work.path("cap").asString();
        var config=new LinkedHashMap<String,Object>();config.put("type","desktop");config.put("documentType","word");config.put("width","100%");config.put("height","100%");
        config.put("document",Map.of("fileType","docx","key",work.path("key").asString(),"title",work.path("alias").asString(),"url",sourceUrl(work),"permissions",Map.of("edit",work.path("writable").asBoolean(),"download",false,"print",false,"comment",false,"review",false)));
        config.put("editorConfig",Map.of("callbackUrl",internal+"/docs-api/callback/"+id+"?cap="+capability,"mode",work.path("writable").asBoolean()?"edit":"view","lang","pt-BR","user",Map.of("id","synthetic-"+id,"name","Pessoa sintética"),"customization",Map.of("forcesave",false,"compactToolbar",true)));
        var claims=new LinkedHashMap<String,Object>(config);claims.put("exp",work.path("expires").asLong());config.put("token",jwt.sign(claims));return DocsJwt.JSON.valueToTree(config);
    }
    private String sourceUrl(ObjectNode work){return internal+"/docs-api/source/"+work.path("id").asString()+"?cap="+work.path("cap").asString();}
    synchronized byte[] source(UUID id,String cap,String authorization){
        var work=work(id,cap);var claims=jwt.verify(bearer(authorization),"download");
        if(!sourceUrl(work).equals(claims.path("payload").path("url").asString()))fail(401,"Escopo de download inválido");
        return store.version(document(work),work.path("sourceVersion").asString());
    }
    synchronized byte[] version(UUID id,String cap,String version){return store.version(document(work(id,cap)),version);}
    synchronized byte[] recovery(UUID id,String cap){
        var work=work(id,cap);if(!work.has("candidateHash"))fail(404,"Tentativa indisponível");
        String hash=work.path("candidateHash").asString();if(!hash.matches("[a-f0-9]{64}"))fail(503,"Estado inválido");
        try{return Files.readAllBytes(root.resolve("quarantine").resolve(id.toString()).resolve(hash+".docx"));}catch(IOException e){fail(503,"Tentativa indisponível");return new byte[0];}
    }
    synchronized void event(UUID id,String cap,boolean dirty){
        var work=work(id,cap);if(!isActive(work)||work.path("closed").asBoolean())fail(409,"Sessão finalizada");
        if(dirty)work.put("dirtyEpoch",work.path("dirtyEpoch").asLong()+1);
        if(!Set.of("rejected","recovery-required").contains(work.path("state").asString()))work.put("state",dirty?"editing-local":"in-editor-service");
        persist(work);
    }
    synchronized JsonNode force(UUID id,String cap){
        var work=work(id,cap);if(!work.path("writable").asBoolean())fail(403,"Somente leitura");
        if(work.path("closed").asBoolean()||!isActive(work)||!work.path("pendingForce").asString().isEmpty())fail(409,"Salvamento indisponível");
        String operation=UUID.randomUUID().toString();work.put("pendingForce",operation);work.put("forceEpoch",work.path("dirtyEpoch").asLong());work.put("state","save-requested");persist(work);
        var request=new LinkedHashMap<String,Object>();request.put("c","forcesave");request.put("key",work.path("key").asString());request.put("userdata",operation);
        var claims=new LinkedHashMap<String,Object>(request);claims.put("operation","command");claims.put("exp",Instant.now().plusSeconds(60).getEpochSecond());request.put("token",jwt.sign(claims));
        try {var response=DocsJwt.JSON.readTree(fetch.command(DocsJwt.JSON.writeValueAsBytes(request)));if(response.path("error").asInt(-1)!=0){work.put("state","timeout");work.put("pendingForce","");persist(work);}return response;}
        catch(DocumentStore.Failure failure){work.put("state","recovery-required");work.put("pendingForce","");persist(work);throw failure;}
    }
    private static String bearer(String header){return header!=null&&header.startsWith("Bearer ")?header.substring(7):null;}
    private JsonNode signedMessage(JsonNode claims){
        if(claims.has("payload"))return claims.get("payload");
        if(!(claims instanceof ObjectNode object))fail(401,"JWT sem mensagem");
        var result=claims.deepCopy();
        if(result instanceof ObjectNode message){for(String name:List.of("operation","exp","iat","nbf"))message.remove(name);return message;}
        fail(401,"JWT sem mensagem");return claims;
    }
    private static Object stable(JsonNode node){
        if(node.isObject()){var values=new TreeMap<String,Object>();node.properties().forEach(e->values.put(e.getKey(),stable(e.getValue())));return values;}
        if(node.isArray()){var values=new ArrayList<Object>();node.forEach(n->values.add(stable(n)));return values;}
        return node;
    }
    synchronized int callback(UUID id,String cap,JsonNode body,String authorization){
        var work=work(id,cap);
        if(!(body instanceof ObjectNode object))fail(400,"Mensagem inválida");
        var message=body.deepCopy();if(message instanceof ObjectNode value)value.remove("token");
        String token=bearer(authorization);if(token==null)token=body.path("token").asString("");
        var payload=signedMessage(jwt.verify(token,"callback"));
        if(payload instanceof ObjectNode value){var clean=value.deepCopy();clean.remove("token");payload=clean;}
        if(body.has("token")&&!signedMessage(jwt.verify(body.path("token").asString(),"callback")).equals(message))fail(401,"Token de corpo sem escopo válido");
        if(!payload.isObject()||!payload.equals(message)||!work.path("key").asString().equals(payload.path("key").asString())||!payload.path("status").isIntegralNumber())fail(401,"Callback sem escopo válido");
        int status=payload.path("status").asInt();if(!Set.of(1,2,3,4,6,7).contains(status))fail(400,"Status inválido");
        String fingerprint=checksum(DocsJwt.JSON.writeValueAsBytes(stable(payload)));var receipts=work.withObject("receipts");
        if(receipts.has(fingerprint))return 0;
        if(!isActive(work)||work.path("closed").asBoolean())fail(409,"Chave obsoleta");
        work.put("lastStatus",status);
        if(status==1){if(!Set.of("rejected","recovery-required").contains(work.path("state").asString()))work.put("state","in-editor-service");persist(work);return 0;}
        if(status==3||status==7){work.put("state","recovery-required");work.put("pendingForce","");persist(work);return 0;}
        if(status==4){work.put("closed",true);if(!Set.of("committed","rejected","recovery-required","timeout").contains(work.path("state").asString()))work.put("state","in-editor-service");receipts.put(fingerprint,true);persist(work);return 0;}
        if(!work.path("writable").asBoolean())fail(403,"Somente leitura");
        if(!"docx".equals(payload.path("filetype").asString("docx")))fail(415,"Tipo inválido");
        String operation=payload.path("userdata").asString("");
        if(status==6&&(operation.isEmpty()||work.path("pendingForce").asString().isEmpty()||!operation.equals(work.path("pendingForce").asString())))fail(409,"Checkpoint não solicitado ou obsoleto");
        work.put("state","candidate-received");persist(work);
        byte[] downloaded;
        try{downloaded=fetch.download(payload.path("url").asString(),work.path("key").asString());}
        catch(DocumentStore.Failure failure){work.put("state","recovery-required");work.put("reason","DOWNLOAD_FAILED");work.put("pendingForce","");persist(work);throw failure;}
        String downloadedHash=checksum(downloaded);
        byte[] source=store.version(document(work),work.path("sourceVersion").asString());
        byte[] candidate=SavedateFieldSemanticRepair.repairKnownInstructionLoss(source,downloaded);
        candidate=ImplicitParagraphDefaultsRepair.repairIfSynthesized(source,candidate);
        long nextVersion=store.versions(document(work)).size()+1L;
        if("rebase".equals(work.path("mode").asString()))
            candidate=gates.get(document(work)).normalizeCandidate(document(work),candidate,nextVersion);
        String hash=checksum(candidate);work.put("state","validation-pending");persist(work);
        try {
            DocxValidator.validate(candidate);
            String current=store.currentVersion(document(work));boolean alreadyCommitted=hash.equals(checksum(store.version(document(work),current)))&&!current.equals(work.path("sourceVersion").asString());
            if(!alreadyCommitted) {
                var saved=store.saveVersion(document(work),grant(work).token(),candidate,work.path("headVersion").asString());
                work.put("headVersion",saved.version());work.put("commits",work.path("commits").asInt()+1);
            }else {if(!current.equals(work.path("headVersion").asString()))work.put("commits",work.path("commits").asInt()+1);work.put("headVersion",current);}
        }catch(DocumentStore.Failure failure){
            quarantine(id,downloadedHash,downloaded);work.put("candidateHash",downloadedHash);work.put("reason",failure.status()==409?"CONFLICT":"CANDIDATE_REJECTED");work.put("state","rejected");work.put("pendingForce","");persist(work);throw failure;
        }
        String pending=work.path("pendingForce").asString();
        work.put("lastHash",hash);receipts.put(fingerprint,true);
        if(status==2)work.put("closed",true);
        boolean correlated=operation.equals(pending)&&work.path("forceEpoch").asLong()==work.path("dirtyEpoch").asLong();
        work.put("state",status==2||correlated?"committed":"in-editor-service");work.put("pendingForce","");
        try{persist(work);return 0;}
        catch(DocumentStore.Failure metadataFailure){
            // The validated object already committed. This is an acknowledgment/reconciliation
            // failure, never an unvalidated rejection or a promise to roll back immutable data.
            receipts.remove(fingerprint);work.put("closed",false);work.put("pendingForce",pending);work.put("state","recovery-required");work.put("reason","ACK_METADATA_PENDING");
            try{persist(work);}catch(DocumentStore.Failure ignored){/* retained current allows retry reconciliation */}
            throw metadataFailure;
        }
    }
    private void quarantine(UUID id,String hash,byte[] bytes) {
        try{Path folder=root.resolve("quarantine").resolve(id.toString());Files.createDirectories(folder);Path file=folder.resolve(hash+".docx");if(Files.exists(file))return;try(var items=Files.list(folder)){if(items.count()>=8)fail(503,"Quarentena cheia");}durable(file,bytes);}
        catch(IOException e){fail(503,"Quarentena indisponível");}
    }
    private void persist(ObjectNode work){stateWriter.write(root.resolve("sessions").resolve(work.path("id").asString()+".json"),DocsJwt.JSON.writeValueAsBytes(work));}
    private static void durable(Path target,byte[] bytes){
        Path pending=target.resolveSibling(UUID.randomUUID()+".pending");
        try{try(var channel=FileChannel.open(pending,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){var buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}Files.move(pending,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(IOException e){fail(503,"Persistência indisponível");}
    }
}
