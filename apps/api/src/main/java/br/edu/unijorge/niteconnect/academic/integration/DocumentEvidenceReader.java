package br.edu.unijorge.niteconnect.academic.integration;

import java.time.Instant;
import java.util.UUID;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.SourceSnapshot;

/** Future B05 provider must authorize reads and coordinate version consistency with the academic capture transaction.
 * No runtime implementation is supplied while B05 is unavailable. */
public interface DocumentEvidenceReader {
    SourceSnapshot read(UUID actor,UUID member,UUID source,Instant from,Instant to);
}
