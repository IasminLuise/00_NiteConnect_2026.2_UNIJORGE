-- Supports bounded global cleanup ordered by expiry without scanning per-list indexes.
CREATE INDEX frequency_qr_handoffs_expiry_idx
    ON nite_connect.frequency_qr_handoffs(expires_at, handoff_hash);
