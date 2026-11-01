ALTER TABLE nite_connect.justification_attachment_retention_policies
 ADD CONSTRAINT justification_retention_grace_max_30_days
 CHECK (deletion_grace_days BETWEEN 0 AND 30);
