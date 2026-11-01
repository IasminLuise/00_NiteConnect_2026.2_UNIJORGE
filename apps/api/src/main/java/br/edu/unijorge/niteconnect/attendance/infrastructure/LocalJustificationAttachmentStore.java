package br.edu.unijorge.niteconnect.attendance.infrastructure;

import br.edu.unijorge.niteconnect.attendance.domain.JustificationAttachmentRules;
import br.edu.unijorge.niteconnect.attendance.application.JustificationAttachmentStore;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/** Opt-in private filesystem adapter; the configured directory must not be served as static content. */
@Component
public class LocalJustificationAttachmentStore implements JustificationAttachmentStore {
    public static final int MAX_BYTES = Math.toIntExact(JustificationAttachmentRules.MAX_BYTES_PER_ATTACHMENT);
    private final Path root;

    public LocalJustificationAttachmentStore(@Value("${nite.justification.local-root:}") String directory) {
        try {
            if (directory.isBlank()) { root = null; return; }
            Path configured = Path.of(directory).toAbsolutePath().normalize();
            Files.createDirectories(configured);
            if (Files.isSymbolicLink(configured)) throw new IOException("Symbolic storage root");
            root = configured.toRealPath();
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível inicializar o armazenamento privado de justificativas");
        }
    }

    @Override public boolean enabled() { return root != null; }

    @Override
    public Stored write(UUID key, String filename, String contentType, InputStream input) {
        Path staging = path(key, ".staging");
        try {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "Comprovante excede 5.000.000 bytes");
            if (bytes.length == 0) throw invalid();
            String safeName = safeName(filename);
            String mime = validate(safeName, contentType, bytes);
            Files.write(staging, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
            try { Files.move(staging, path(key, ".object"), StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(staging, path(key, ".object")); }
            return new Stored(safeName, mime, bytes.length, digest(bytes));
        } catch (ApiException e) {
            cleanup(staging);
            throw e;
        } catch (IOException e) {
            cleanup(staging);
            throw unavailable();
        }
    }

    @Override
    public byte[] read(UUID key, long size, String sha256) {
        try (InputStream input = Files.newInputStream(path(key, ".object"), StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES || bytes.length != size || !digest(bytes).equals(sha256)) throw unavailable();
            return bytes;
        } catch (IOException e) { throw unavailable(); }
    }

    @Override
    public void delete(UUID key) {
        try {
            Files.deleteIfExists(path(key, ".staging"));
            Files.deleteIfExists(path(key, ".object"));
        } catch (IOException e) { throw unavailable(); }
    }

    public static String safeName(String filename) {
        if (filename == null || filename.isBlank()) throw invalid();
        String name = filename.replaceAll("[\\p{Cntrl}/\\\\:\"<>|?*]", "_").strip();
        if (name.length() > 200 || name.equals(".") || name.equals("..")) throw invalid();
        return name;
    }

    private static String validate(String filename, String suppliedContentType, byte[] bytes) {
        String lower = filename.toLowerCase(Locale.ROOT);
        String mime;
        if (lower.endsWith(".pdf")) {
            mime = "application/pdf";
            if (!startsWith(bytes, new byte[]{'%', 'P', 'D', 'F', '-'}) || !contains(bytes, "%%EOF".getBytes(java.nio.charset.StandardCharsets.US_ASCII))) throw invalid();
        } else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            mime = "image/jpeg";
            validateImage(bytes, false);
        } else if (lower.endsWith(".png")) {
            mime = "image/png";
            validateImage(bytes, true);
        } else throw invalid();

        if (suppliedContentType == null || !suppliedContentType.split(";", 2)[0].strip().equalsIgnoreCase(mime)) throw invalid();
        return mime;
    }

    private static void validateImage(byte[] bytes, boolean png) {
        if (png) {
            byte[] signature = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
            byte[] end = {0, 0, 0, 0, 73, 69, 78, 68, (byte)174, 66, 96, (byte)130};
            if (bytes.length < 20 || !Arrays.equals(Arrays.copyOf(bytes, 8), signature)
                    || !Arrays.equals(Arrays.copyOfRange(bytes, bytes.length - 12, bytes.length), end)) throw invalid();
        } else if (bytes.length < 4 || bytes[0] != (byte)255 || bytes[1] != (byte)216
                || bytes[bytes.length - 2] != (byte)255 || bytes[bytes.length - 1] != (byte)217) throw invalid();
        try (var image = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (image == null) throw invalid();
            var readers = ImageIO.getImageReaders(image);
            if (!readers.hasNext()) throw invalid();
            var reader = readers.next();
            try {
                reader.setInput(image, true, true);
                long width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width * height > 20_000_000L) throw invalid();
                reader.read(0);
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException e) { throw invalid(); }
    }

    private Path path(UUID key, String suffix) {
        if (!enabled()) throw unavailable();
        return root.resolve(key + suffix);
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        return bytes.length >= prefix.length && Arrays.equals(Arrays.copyOf(bytes, prefix.length), prefix);
    }
    private static boolean contains(byte[] bytes, byte[] token) {
        outer: for (int i = 0; i <= bytes.length - token.length; i++) {
            for (int j = 0; j < token.length; j++) if (bytes[i + j] != token[j]) continue outer;
            return true;
        }
        return false;
    }
    private static String digest(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 indisponível", e); }
    }
    private static ApiException invalid() { return new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Formato ou conteúdo de comprovante inválido"); }
    private static ApiException unavailable() { return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Armazenamento privado indisponível"); }
    private static void cleanup(Path path) { try { Files.deleteIfExists(path); } catch (IOException ignored) { /* recovery remains visible through the failed upload state */ } }
}
