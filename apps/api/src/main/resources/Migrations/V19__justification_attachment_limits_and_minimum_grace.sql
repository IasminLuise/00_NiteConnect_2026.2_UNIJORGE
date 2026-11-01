ALTER TABLE nite_connect.justification_attachment_retention_policies
 ADD CONSTRAINT justification_attachment_retention_minimum_grace
 CHECK(deletion_grace_days=30) NOT VALID;

ALTER TABLE nite_connect.absence_justification_attachments
 ADD CONSTRAINT justification_attachment_pending_size_reservation
 CHECK(state<>'PENDENTE' OR size_bytes BETWEEN 1 AND 5000000) NOT VALID;

CREATE INDEX absence_justification_active_attachments_idx
 ON nite_connect.absence_justification_attachments(justification_id,state)
 WHERE state IN ('PENDENTE','DISPONIVEL','EXCLUSAO_PENDENTE');

CREATE INDEX absence_justification_active_attachment_listing_idx
 ON nite_connect.absence_justification_attachments(justification_id,created_at,id)
 WHERE state IN ('PENDENTE','DISPONIVEL','EXCLUSAO_PENDENTE');

CREATE INDEX absence_justification_member_active_attachments_idx
 ON nite_connect.absence_justification_attachments(member_id,state)
 WHERE state IN ('PENDENTE','DISPONIVEL','EXCLUSAO_PENDENTE');
