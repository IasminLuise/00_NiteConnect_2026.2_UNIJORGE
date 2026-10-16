package br.edu.unijorge.niteconnect.activities.application;

import br.edu.unijorge.niteconnect.access.application.AccessService;
import br.edu.unijorge.niteconnect.activities.api.EvidenceContracts.Upload;
import br.edu.unijorge.niteconnect.activities.infrastructure.LocalEvidenceObjectStore;
import br.edu.unijorge.niteconnect.shared.api.ApiException;
import org.springframework.stereotype.Service;
import java.io.*;
import java.time.*;
import java.util.*;

/** Coordinates private object I/O outside PostgreSQL transactions/advisory locks. */
@Service
public class EvidenceUploads {
    private final EvidenceService evidence; private final EvidenceObjectStore store; private final AccessService access;
    public EvidenceUploads(EvidenceService evidence,EvidenceObjectStore store,AccessService access) { this.evidence=evidence;this.store=store;this.access=access; }
    public EvidenceService.View upload(UUID actor,UUID activity,Upload c,String filename,String mime,InputStream input) {
        var receipt=evidence.prepare(actor,activity,c,LocalEvidenceObjectStore.safeName(filename),store.enabled());
        try {
            var object=store.write(receipt.objectKey(),filename,mime,input);
            return evidence.finish(actor,activity,c,receipt.id(),object);
        } catch(RuntimeException failure) {
            // An ambiguous DB failure must never delete a successfully committed AVAILABLE object.
            try { if(evidence.fail(receipt.id())) store.delete(receipt.objectKey()); }
            catch(RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    public byte[] bytes(UUID actor,UUID activity,UUID id,boolean personal) {
        var download=evidence.download(actor,activity,id,personal);return store.read(download.key(),download.size(),download.sha256());
    }
    /** Explicit administrative recovery contract; no scheduled cleanup or public deletion endpoint. */
    public List<UUID> reconcile(UUID actor,Duration minimumAge,boolean dryRun) {
        access.requireAdministrator(actor);
        if(minimumAge.compareTo(Duration.ofHours(1))<0) throw ApiException.invalid("Recuperação exige margem mínima de uma hora");
        Instant cutoff=Instant.now().minus(minimumAge);List<UUID> candidates=new ArrayList<>();
        Set<UUID> keys=new LinkedHashSet<>(evidence.stalePendingKeys(actor,cutoff));
        store.olderThan(cutoff).forEach(entry->keys.add(entry.key()));
        for(UUID key:keys) {
            if(evidence.claimOrphan(actor,key,cutoff,dryRun)) {
                candidates.add(key);if(!dryRun) store.delete(key);
            }
        }
        return List.copyOf(candidates);
    }
}
