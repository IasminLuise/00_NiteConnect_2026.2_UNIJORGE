package br.edu.unijorge.niteconnect.activities.infrastructure;

import br.edu.unijorge.niteconnect.activities.application.EvidenceObjectStore;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.*;
import java.time.Instant;
import java.util.*;

/** Opt-in local development/test adapter. Its directory must never be served as static content. */
@Component
public class LocalEvidenceObjectStore implements EvidenceObjectStore {
    public static final int MAX_BYTES=10*1024*1024;
    private final Path root;
    public LocalEvidenceObjectStore(@Value("${nite.evidence.local-root:}") String directory) {
        try {
            if(directory.isBlank()) { root=null;return; }
            Path configured=Path.of(directory).toAbsolutePath().normalize();
            Files.createDirectories(configured);
            if(Files.isSymbolicLink(configured)) throw new IOException("Symbolic storage root");
            root=configured.toRealPath();
        } catch(IOException e) { throw new IllegalStateException("Não foi possível inicializar armazenamento privado"); }
    }
    public boolean enabled() { return root!=null; }
    private Path path(UUID key,String suffix) {
        if(!enabled()) throw unavailable();
        return root.resolve(key.toString()+suffix);
    }
    public static ApiException unavailable() { return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,"Armazenamento privado indisponível"); }
    private static ApiException invalidFile() { return new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Formato ou conteúdo de arquivo inválido"); }
    public Stored write(UUID key,String filename,String contentType,InputStream input) {
        Path staging=path(key,".upload");
        try {
            byte[] bytes=input.readNBytes(MAX_BYTES+1);
            if(bytes.length>MAX_BYTES) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,"Anexo excede 10 MiB");
            if(bytes.length==0) throw invalidFile();
            String name=safeName(filename);String mime=validate(name,contentType,bytes);
            Files.write(staging,bytes,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS);
            try { Files.move(staging,path(key,".object"),StandardCopyOption.ATOMIC_MOVE); }
            catch(AtomicMoveNotSupportedException e) { Files.move(staging,path(key,".object")); }
            return new Stored(name,mime,bytes.length,digest(bytes),mime.startsWith("image/")?"IMAGE":"FILE");
        } catch(ApiException e) { throw e; }
        catch(IOException e) { throw unavailable(); }
    }
    public byte[] read(UUID key,long size,String sha256) {
        Path object=path(key,".object");
        try(var input=Files.newInputStream(object,StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS)) {
            byte[] data=input.readNBytes(MAX_BYTES+1);
            if(data.length!=size || !digest(data).equals(sha256)) throw unavailable();
            return data;
        } catch(IOException e) { throw unavailable(); }
    }
    public void delete(UUID key) {
        try { Files.deleteIfExists(path(key,".upload"));Files.deleteIfExists(path(key,".object")); }
        catch(IOException e) { throw unavailable(); }
    }
    public List<StoredKey> olderThan(Instant cutoff) {
        if(!enabled()) throw unavailable();
        try(var stream=Files.list(root)) {
            List<StoredKey> result=new ArrayList<>();
            for(Path file:stream.toList()) {
                String name=file.getFileName().toString();
                if(!name.matches("[0-9a-f-]{36}\\.(object|upload)") || Files.isSymbolicLink(file)) continue;
                Instant modified=Files.getLastModifiedTime(file,LinkOption.NOFOLLOW_LINKS).toInstant();
                if(modified.isBefore(cutoff)) result.add(new StoredKey(UUID.fromString(name.substring(0,36)),modified));
            }
            return result;
        } catch(IOException e) { throw unavailable(); }
    }
    public static String safeName(String filename) {
        if(filename==null || filename.isBlank()) throw invalidFile();
        String name=filename.replaceAll("[\\p{Cntrl}/\\\\:\"<>|?*]","_").strip();
        if(name.length()>200 || name.equals(".") || name.equals("..")) throw invalidFile();return name;
    }
    private static String validate(String name,String supplied,byte[] bytes) {
        String lower=name.toLowerCase(Locale.ROOT);String mime;
        if(lower.endsWith(".txt")) {
            mime="text/plain";
            try {
                String text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
                if(text.indexOf(0)>=0) throw invalidFile();
            } catch(CharacterCodingException e) { throw invalidFile(); }
        } else if(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            boolean png=lower.endsWith(".png");mime=png?"image/png":"image/jpeg";
            if(png) {
                byte[] signature={(byte)137,80,78,71,13,10,26,10};
                byte[] end={0,0,0,0,73,69,78,68,(byte)174,66,96,(byte)130};
                if(bytes.length<20 || !Arrays.equals(Arrays.copyOf(bytes,8),signature)
                        || !Arrays.equals(Arrays.copyOfRange(bytes,bytes.length-12,bytes.length),end)) throw invalidFile();
            } else if(bytes.length<4 || bytes[0]!=(byte)255 || bytes[1]!=(byte)216 || bytes[bytes.length-2]!=(byte)255 || bytes[bytes.length-1]!=(byte)217) throw invalidFile();
            try(var image=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers=ImageIO.getImageReaders(image);if(!readers.hasNext()) throw invalidFile();
                var reader=readers.next();
                try {
                    reader.setInput(image,true,true);long width=reader.getWidth(0),height=reader.getHeight(0);
                    if(width<1 || height<1 || width*height>20_000_000L) throw invalidFile();
                    reader.read(0);
                } finally { reader.dispose(); }
            } catch(IOException | IllegalArgumentException e) { throw invalidFile(); }
        } else throw invalidFile();
        if(supplied==null || !supplied.split(";",2)[0].strip().equalsIgnoreCase(mime)) throw invalidFile();
        return mime;
    }
    private static String digest(byte[] data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 indisponível",e); }
    }
}
