package br.edu.unijorge.experiments;

import java.util.UUID;

/** Laboratory-only port. A rejection must happen before any current pointer changes. */
@FunctionalInterface
public interface CandidateGate {
    void validate(UUID document, byte[] candidate, long nextNumber);
    default void register(UUID document, String name, byte[] source) {}
}
