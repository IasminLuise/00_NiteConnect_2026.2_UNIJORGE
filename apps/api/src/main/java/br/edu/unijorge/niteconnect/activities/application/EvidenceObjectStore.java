package br.edu.unijorge.niteconnect.activities.application;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Private storage boundary. Callers never supply filesystem paths or public URLs. */
public interface EvidenceObjectStore {
    boolean enabled();
    Stored write(UUID key,String filename,String contentType,InputStream input);
    byte[] read(UUID key,long size,String sha256);
    void delete(UUID key);
    List<StoredKey> olderThan(Instant cutoff);
    record Stored(String filename,String contentType,long size,String sha256,String evidenceType) {}
    record StoredKey(UUID key,Instant modifiedAt) {}
}
