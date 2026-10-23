package br.edu.unijorge.niteconnect.documents.infrastructure;

import br.edu.unijorge.niteconnect.documents.application.DocumentObjectStore;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Opt-in LOCAL DEVELOPMENT adapter. The root must be private and outside public directories.
 * This is not a production storage selection. No automatic deletion: orphans remain recoverable.
 * The private root and its ancestors must not be writable by untrusted local processes.
 */
@Component
public class LocalDocumentObjectStore implements DocumentObjectStore {
    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private final Path root;

    public LocalDocumentObjectStore(@Value("${nite.documents.storage-root:}") String storageRoot,
            @Value("${nite.documents.local-development:false}") boolean localDevelopment, Environment environment) {
        boolean production = java.util.Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("prod") || profile.equalsIgnoreCase("production"));
        root = !localDevelopment || production || storageRoot == null || storageRoot.isBlank()
                ? null : Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @Override public boolean enabled() { return root != null; }

    @Override public Stored put(byte[] bytes) {
        requireEnabled();
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) throw ApiException.invalid("Documento inválido");
        byte[] snapshot = bytes.clone();
        try {
            inspectAncestors();
            Files.createDirectories(root);
            inspectAncestors();
            UUID key = UUID.randomUUID();
            try (var channel = Files.newByteChannel(root.resolve(key.toString()), StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                ByteBuffer buffer = ByteBuffer.wrap(snapshot);
                while (buffer.hasRemaining()) channel.write(buffer);
            }
            return new Stored(key, snapshot.length, digest(snapshot));
        } catch (IOException | SecurityException e) { throw unavailable(); }
    }

    @Override public byte[] read(UUID key, long size, String sha256) {
        requireEnabled();
        if (key == null || size < 1 || size > MAX_BYTES || sha256 == null || !sha256.matches("[0-9a-f]{64}")) throw unavailable();
        try {
            inspectAncestors();
            Path object = root.resolve(key.toString());
            if (!Files.isRegularFile(object, LinkOption.NOFOLLOW_LINKS)) throw unavailable();
            try (SeekableByteChannel channel = Files.newByteChannel(object, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                if (channel.size() != size) throw unavailable();
                ByteBuffer buffer = ByteBuffer.allocate((int) size + 1);
                while (buffer.hasRemaining() && channel.read(buffer) != -1) { /* bounded read */ }
                if (buffer.position() != size) throw unavailable();
                byte[] bytes = java.util.Arrays.copyOf(buffer.array(), (int) size);
                if (!MessageDigest.isEqual(digest(bytes).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                        sha256.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) throw unavailable();
                return bytes;
            }
        } catch (IOException | SecurityException e) { throw unavailable(); }
    }

    private void inspectAncestors() throws IOException {
        for (Path path = root; path != null; path = path.getParent()) {
            if (Files.isSymbolicLink(path) || (Files.exists(path, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS))) throw unavailable();
            // Also reject filesystem aliases such as Windows directory junctions.
            if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !path.toRealPath().equals(path)) throw unavailable();
        }
    }

    private void requireEnabled() { if (!enabled()) throw unavailable(); }
    private static ApiException unavailable() { return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Armazenamento de documentos indisponível"); }
    private static String digest(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 unavailable"); }
    }
}
