package br.edu.unijorge.niteconnect.attendance.application;

import java.io.InputStream;
import java.util.UUID;

/** Private B04 storage boundary; it is intentionally separate from B03 activity evidence. */
public interface JustificationAttachmentStore {
    boolean enabled();
    Stored write(UUID key, String filename, String contentType, InputStream input);
    byte[] read(UUID key, long size, String sha256);
    /** Deletion must be idempotent so retries can recover an interrupted metadata finalization. */
    void delete(UUID key);

    record Stored(String filename, String contentType, long size, String sha256) {}
}
