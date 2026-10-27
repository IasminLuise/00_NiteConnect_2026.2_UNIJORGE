package br.edu.unijorge.niteconnect.academic.integration;

import java.time.Instant;
import java.util.UUID;
import br.edu.unijorge.niteconnect.academic.domain.AcademicRecords.SourceSnapshot;

/** A provider must authorize reads and coordinate version consistency with the academic capture transaction. */
public interface DocumentEvidenceReader {
    SourceSnapshot read(UUID actor,UUID member,UUID source,Instant from,Instant to);
}
