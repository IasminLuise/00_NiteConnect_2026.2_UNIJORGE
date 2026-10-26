package br.edu.unijorge.niteconnect.documents.application;

import java.util.UUID;

/** Private bytes for the experimental document flow; objects are retained for recovery. */
public interface DocumentObjectStore {
    boolean enabled();
    Stored put(byte[] bytes);
    byte[] read(UUID key, long size, String sha256);
    record Stored(UUID key, long size, String sha256) {}
}
