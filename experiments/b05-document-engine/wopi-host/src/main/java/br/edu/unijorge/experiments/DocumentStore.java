package br.edu.unijorge.experiments;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import static java.nio.file.StandardOpenOption.*;

/** Disposable single-process host. No database or NITE authorization contract. */
public final class DocumentStore {
    public record Session(UUID id,String token,Instant expiresAt) {
        @Override public String toString(){return "Session[id="+id+", token=REDACTED]";}
    }
    public record Info(String name,long size,String version,String modified,boolean writable) {}
    private record Grant(UUID id,UUID document,boolean writable,Instant expiresAt) {}
    private record Lock(String value,Instant expiresAt) {}
    private record Pointer(String version,Instant modified,long number,String checksum) {}
    public static final class Failure extends RuntimeException {
        private final int status;private final String lock;
        public Failure(int status,String message){this(status,message,"");}
        Failure(int status,String message,String lock){super(message);this.status=status;this.lock=lock;}
        public int status(){return status;}
        public String lock(){return lock;}
    }
    private final Path root;private final Clock clock;private final CandidateGate gate;
    private final Map<String,Grant> grants=new HashMap<>();private final Map<UUID,Lock> locks=new HashMap<>();
    public DocumentStore(Path root,Clock clock)throws IOException{this(root,clock,(id,after,number)->{});}
    public DocumentStore(Path root,Clock clock,CandidateGate gate)throws IOException{this.root=root.toAbsolutePath().normalize();this.clock=clock;this.gate=Objects.requireNonNull(gate);Files.createDirectories(this.root);}
    public synchronized void seed(UUID id,String name,byte[] bytes) {
        gate.register(id,name,bytes);
        if(Files.exists(dir(id).resolve("current")))return;
        DocxValidator.validate(bytes);
        if(!name.matches("F[0-9]{2}\\.docx"))throw new Failure(400,"Nome de fixture inválido");
        io(()->{Files.createDirectories(dir(id).resolve("versions"));Files.writeString(dir(id).resolve("name"),name,CREATE_NEW);write(id,bytes,null);return null;});
    }
    public synchronized Session issue(UUID id,boolean writable,Duration ttl) {
        pointer(id);if(ttl.isNegative()||ttl.isZero()||ttl.compareTo(Duration.ofHours(1))>0)throw new Failure(400,"Validade técnica inválida");
        byte[] random=new byte[32];new SecureRandom().nextBytes(random);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        var grant=new Grant(UUID.randomUUID(),id,writable,clock.instant().plus(ttl));grants.put(hash(token.getBytes(StandardCharsets.UTF_8)),grant);
        return new Session(grant.id(),token,grant.expiresAt());
    }
    public synchronized void revoke(UUID session){grants.values().removeIf(g->g.id().equals(session));}
    public synchronized String user(UUID id,String token){return authorize(id,token,false).id().toString();}
    private Grant authorize(UUID id,String token,boolean write) {
        if(token==null)throw new Failure(401,"Sessão inválida");
        var grant=grants.get(hash(token.getBytes(StandardCharsets.UTF_8)));
        if(grant==null||!clock.instant().isBefore(grant.expiresAt()))throw new Failure(401,"Sessão inválida");
        if(!grant.document().equals(id))throw new Failure(404,"Documento indisponível");
        if(write&&!grant.writable())throw new Failure(403,"Sessão somente leitura");return grant;
    }
    public synchronized Info info(UUID id,String token) {
        var g=authorize(id,token,false);var p=pointer(id);
        return io(()->new Info(Files.readString(dir(id).resolve("name")),Files.size(versionPath(id,p.version())),p.version(),p.modified().toString(),g.writable()));
    }
    public synchronized byte[] read(UUID id,String token){authorize(id,token,false);return current(id);}
    synchronized byte[] current(UUID id){return version(id,pointer(id).version());}
    synchronized String currentVersion(UUID id){return pointer(id).version();}
    synchronized byte[] version(UUID id,String version) {
        try{UUID.fromString(version);}catch(IllegalArgumentException e){throw new Failure(404,"Versão indisponível");}
        if(!Files.isRegularFile(versionPath(id,version)))throw new Failure(404,"Versão indisponível");
        return io(()->Files.readAllBytes(versionPath(id,version)));
    }
    synchronized List<String> versions(UUID id) {
        pointer(id);return io(()->{try(var files=Files.list(dir(id).resolve("versions"))){return files.filter(p->p.toString().endsWith(".docx")).map(p->p.getFileName().toString().replace(".docx","")).sorted().toList();}});
    }
    public Info save(UUID id,String token,byte[] bytes,String base,String lock) {
        synchronized(this){authorize(id,token,true);}
        DocxValidator.validate(bytes);
        synchronized(this) {
            authorize(id,token,true);var before=pointer(id);String held=lockValue(id);
            if(!held.isEmpty()&&!held.equals(lock))throw new Failure(409,"Lock conflitante",held);
            // Collabora's timestamp is the CAS base. Missing timestamp never authorizes a force-overwrite.
            Instant expected=null;
            if(base!=null)try{expected=Instant.parse(base);}catch(java.time.format.DateTimeParseException ignored){/* invalid base conflicts */}
            if(expected==null||!before.modified().truncatedTo(java.time.temporal.ChronoUnit.MICROS).equals(expected.truncatedTo(java.time.temporal.ChronoUnit.MICROS)))throw new Failure(409,"Versão de origem alterada",held);
            return commit(id,token,bytes,before);
        }
    }
    /** Native host operation for a protocol adapter, without WOPI headers or timestamp emulation. */
    public Info saveVersion(UUID id,String token,byte[] bytes,String expectedVersion) {
        synchronized(this){authorize(id,token,true);}
        DocxValidator.validate(bytes);
        synchronized(this) {
            authorize(id,token,true);var before=pointer(id);
            if(!before.version().equals(expectedVersion)||!lockValue(id).isEmpty())throw new Failure(409,"Versão de origem alterada");
            return commit(id,token,bytes,before);
        }
    }
    private Info commit(UUID id,String token,byte[] bytes,Pointer before) {
            try { gate.validate(id,bytes,before.number()+1); }
            catch(Failure rejected) {
                // Private bounded quarantine, never exposed by the version/download routes.
                io(()->{Path rejectedDir=dir(id).resolve("rejected");Files.createDirectories(rejectedDir);
                    try(var entries=Files.list(rejectedDir)){if(entries.count()>=64)throw new IOException("Quarantine full");}
                    String attempt=UUID.randomUUID().toString();
                    durable(rejectedDir.resolve(attempt+".docx"),bytes);
                    durable(rejectedDir.resolve(attempt+".txt"),(hash(bytes)+"\n"+before.version()+"\n"+clock.instant()+"\n"+rejected.getMessage()+"\n").getBytes(StandardCharsets.UTF_8));return null;});
                throw rejected;
            }
            io(()->{write(id,bytes,before);return null;});return info(id,token);
    }
    public synchronized String lockValue(UUID id) {
        var lock=locks.get(id);if(lock!=null&&!clock.instant().isBefore(lock.expiresAt())){locks.remove(id);lock=null;}
        return lock==null?"":lock.value();
    }
    public synchronized void lock(UUID id,String token,String operation,String value,String old) {
        authorize(id,token,true);pointer(id);String current=lockValue(id);
        if(value==null||value.isBlank()||value.length()>1024)throw new Failure(400,"Lock inválido");
        if(operation.equals("LOCK")) {
            if(old!=null?!current.equals(old):!current.isEmpty()&&!current.equals(value))throw new Failure(409,"Lock conflitante",current);
            locks.put(id,new Lock(value,clock.instant().plusSeconds(1800)));
        }else if(operation.equals("REFRESH_LOCK")||operation.equals("UNLOCK")) {
            if(!current.equals(value))throw new Failure(409,"Lock conflitante",current);
            if(operation.equals("UNLOCK"))locks.remove(id);else locks.put(id,new Lock(value,clock.instant().plusSeconds(1800)));
        }else throw new Failure(501,"Operação não implementada");
    }
    private Path dir(UUID id){return root.resolve(id.toString());}
    private Path versionPath(UUID id,String version){return dir(id).resolve("versions").resolve(version+".docx");}
    private Pointer pointer(UUID id) {
        if(!Files.isRegularFile(dir(id).resolve("current")))throw new Failure(404,"Documento indisponível");
        return io(()->{var lines=Files.readAllLines(dir(id).resolve("current"));return new Pointer(lines.get(0),Instant.parse(lines.get(1)),Long.parseLong(lines.get(2)),lines.get(3));});
    }
    private void write(UUID id,byte[] bytes,Pointer before)throws IOException {
        String version=UUID.randomUUID().toString();Instant modified=clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        if(before!=null&&!modified.isAfter(before.modified().truncatedTo(java.time.temporal.ChronoUnit.MICROS)))modified=before.modified().truncatedTo(java.time.temporal.ChronoUnit.MICROS).plusMillis(1);
        // Unreferenced objects after a failed pointer update are retained; never delete a committed version.
        durable(versionPath(id,version),bytes);
        Path pending=dir(id).resolve(UUID.randomUUID()+".pending");
        durable(pending,(version+"\n"+modified+"\n"+(before==null?1:before.number()+1)+"\n"+hash(bytes)+"\n").getBytes(StandardCharsets.UTF_8));
        Files.move(pending,dir(id).resolve("current"),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
    private static void durable(Path path,byte[] bytes)throws IOException {
        try(var channel=FileChannel.open(path,CREATE_NEW,WRITE)){var buffer=ByteBuffer.wrap(bytes);while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}
    }
    private static String hash(byte[] bytes) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    @FunctionalInterface private interface IO<T>{T run()throws IOException;}
    private static <T>T io(IO<T> operation){try{return operation.run();}catch(IOException e){throw new Failure(503,"Armazenamento indisponível");}}
}
