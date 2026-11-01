CREATE TABLE nite_connect.justification_attachment_retention_policies (
 id uuid PRIMARY KEY,
 version integer NOT NULL UNIQUE CHECK(version > 0),
 retention_months integer NOT NULL CHECK(retention_months > 0),
 deletion_grace_days integer NOT NULL CHECK(deletion_grace_days >= 0),
 anchor_rule varchar(48) NOT NULL CHECK(anchor_rule='MAX_ANALYSIS_AND_SEMESTER_END'),
 created_by uuid NOT NULL REFERENCES nite_connect.members(id),
 created_at timestamptz NOT NULL,
 creation_reason varchar(1000) NOT NULL CHECK(length(trim(creation_reason)) > 0),
 supersedes_id uuid REFERENCES nite_connect.justification_attachment_retention_policies(id),
 CHECK(supersedes_id IS NULL OR supersedes_id<>id)
);

CREATE TABLE nite_connect.justification_attachment_retention_events (
 id uuid PRIMARY KEY,
 policy_id uuid NOT NULL REFERENCES nite_connect.justification_attachment_retention_policies(id),
 sequence bigint GENERATED ALWAYS AS IDENTITY,
 event_type varchar(12) NOT NULL CHECK(event_type IN ('CRIADA','VALIDADA','SUBSTITUIDA')),
 actor_id uuid NOT NULL REFERENCES nite_connect.members(id),
 occurred_at timestamptz NOT NULL,
 reason varchar(1000) NOT NULL CHECK(length(trim(reason)) > 0),
 source_reference varchar(1000),
 backup_handling_reference varchar(1000),
 sensitive_data_handling_reference varchar(1000),
 UNIQUE(policy_id,sequence),
 CHECK((event_type='VALIDADA' AND length(trim(source_reference))>0
          AND length(trim(backup_handling_reference))>0 AND length(trim(sensitive_data_handling_reference))>0)
    OR (event_type<>'VALIDADA' AND source_reference IS NULL
          AND backup_handling_reference IS NULL AND sensitive_data_handling_reference IS NULL))
);
CREATE INDEX justification_attachment_retention_history_idx
 ON nite_connect.justification_attachment_retention_events(policy_id,sequence);

CREATE TABLE nite_connect.absence_justification_retention_classifications (
 id uuid PRIMARY KEY,
 justification_id uuid NOT NULL REFERENCES nite_connect.absence_justifications(id),
 version integer NOT NULL CHECK(version>0),
 semester_end_on date NOT NULL,
 source_reference varchar(1000) NOT NULL CHECK(length(trim(source_reference))>0),
 reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 created_by uuid NOT NULL REFERENCES nite_connect.members(id),
 created_at timestamptz NOT NULL,
 UNIQUE(justification_id,version)
);
CREATE INDEX absence_justification_retention_classification_idx
 ON nite_connect.absence_justification_retention_classifications(justification_id,version DESC);

CREATE TABLE nite_connect.absence_justification_attachments (
 id uuid PRIMARY KEY,
 justification_id uuid NOT NULL REFERENCES nite_connect.absence_justifications(id),
 member_id uuid NOT NULL REFERENCES nite_connect.members(id),
 retention_policy_id uuid NOT NULL REFERENCES nite_connect.justification_attachment_retention_policies(id),
 object_key uuid UNIQUE,
 context varchar(16) NOT NULL CHECK(context IN ('JUSTIFICATIVA','COMPLEMENTACAO')),
 filename varchar(200),
 content_type varchar(32),
 size_bytes bigint,
 sha256 varchar(64),
 state varchar(20) NOT NULL CHECK(state IN ('PENDENTE','DISPONIVEL','FALHA','EXCLUSAO_PENDENTE','REMOVIDO')),
 uploaded_by uuid NOT NULL REFERENCES nite_connect.members(id),
 created_at timestamptz NOT NULL,
 deleted_at timestamptz,
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
 CHECK((state IN ('DISPONIVEL','EXCLUSAO_PENDENTE') AND object_key IS NOT NULL AND filename IS NOT NULL
          AND content_type IN ('application/pdf','image/jpeg','image/png') AND size_bytes BETWEEN 1 AND 5000000 AND sha256 IS NOT NULL)
    OR (state IN ('PENDENTE','FALHA') AND object_key IS NOT NULL)
    OR (state='REMOVIDO' AND object_key IS NULL AND filename IS NULL AND content_type IS NULL
          AND size_bytes IS NULL AND sha256 IS NULL AND deleted_at IS NOT NULL))
);
CREATE INDEX absence_justification_attachments_idx
 ON nite_connect.absence_justification_attachments(justification_id,created_at,id);

CREATE TABLE nite_connect.absence_justification_attachment_events (
 id uuid PRIMARY KEY,
 attachment_id uuid NOT NULL REFERENCES nite_connect.absence_justification_attachments(id),
 sequence bigint GENERATED ALWAYS AS IDENTITY,
 event_type varchar(24) NOT NULL CHECK(event_type IN
   ('UPLOAD_INICIADO','UPLOAD_DISPONIVEL','UPLOAD_FALHOU','DOWNLOAD_SOLICITADO','EXCLUSAO_INICIADA','EXCLUIDO','EXCLUSAO_FALHOU')),
 actor_id uuid NOT NULL REFERENCES nite_connect.members(id),
 occurred_at timestamptz NOT NULL,
 reason varchar(1000),
 UNIQUE(attachment_id,sequence)
);
CREATE INDEX absence_justification_attachment_events_idx
 ON nite_connect.absence_justification_attachment_events(attachment_id,sequence);

CREATE TABLE nite_connect.absence_justification_attachment_retention_holds (
 event_id uuid PRIMARY KEY,
 hold_id uuid NOT NULL,
 attachment_id uuid NOT NULL REFERENCES nite_connect.absence_justification_attachments(id),
 sequence bigint GENERATED ALWAYS AS IDENTITY,
 event_type varchar(12) NOT NULL CHECK(event_type IN ('APLICADO','LIBERADO')),
 actor_id uuid NOT NULL REFERENCES nite_connect.members(id),
 occurred_at timestamptz NOT NULL,
 basis varchar(1000),
 reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 CHECK((event_type='APLICADO' AND length(trim(basis))>0) OR (event_type='LIBERADO' AND basis IS NULL)),
 UNIQUE(hold_id,sequence)
);
CREATE INDEX absence_justification_attachment_holds_idx
 ON nite_connect.absence_justification_attachment_retention_holds(attachment_id,sequence);

CREATE FUNCTION nite_connect.reject_justification_attachment_history_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Justification attachment history is append-only'; END;
$$;
CREATE TRIGGER immutable_justification_attachment_retention_events
 BEFORE UPDATE OR DELETE ON nite_connect.justification_attachment_retention_events
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_justification_attachment_history_mutation();
CREATE TRIGGER immutable_absence_justification_retention_classifications
 BEFORE UPDATE OR DELETE ON nite_connect.absence_justification_retention_classifications
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_justification_attachment_history_mutation();
CREATE TRIGGER immutable_absence_justification_attachment_events
 BEFORE UPDATE OR DELETE ON nite_connect.absence_justification_attachment_events
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_justification_attachment_history_mutation();
CREATE TRIGGER immutable_absence_justification_attachment_holds
 BEFORE UPDATE OR DELETE ON nite_connect.absence_justification_attachment_retention_holds
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_justification_attachment_history_mutation();
CREATE FUNCTION nite_connect.protect_justification_attachment_retention_policy() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Retention policy versions cannot be deleted'; END IF;
 RAISE EXCEPTION 'Retention policy versions are immutable; create a revision';
END;
$$;
CREATE TRIGGER immutable_justification_attachment_retention_policies
 BEFORE UPDATE OR DELETE ON nite_connect.justification_attachment_retention_policies
 FOR EACH ROW EXECUTE FUNCTION nite_connect.protect_justification_attachment_retention_policy();

CREATE FUNCTION nite_connect.protect_absence_justification_attachment() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Justification attachment metadata cannot be deleted'; END IF;
 IF NEW.id<>OLD.id OR NEW.justification_id<>OLD.justification_id OR NEW.member_id<>OLD.member_id
    OR NEW.retention_policy_id<>OLD.retention_policy_id OR NEW.context<>OLD.context
    OR NEW.uploaded_by<>OLD.uploaded_by OR NEW.created_at<>OLD.created_at OR NEW.version<>OLD.version+1 THEN
  RAISE EXCEPTION 'Justification attachment identity is immutable and version must advance';
 END IF;
 IF NOT ((OLD.state='PENDENTE' AND NEW.state IN ('DISPONIVEL','FALHA'))
      OR (OLD.state='DISPONIVEL' AND NEW.state='EXCLUSAO_PENDENTE')
      OR (OLD.state='EXCLUSAO_PENDENTE' AND NEW.state IN ('DISPONIVEL','REMOVIDO'))) THEN
  RAISE EXCEPTION 'Invalid justification attachment state transition';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER protect_absence_justification_attachment
 BEFORE UPDATE OR DELETE ON nite_connect.absence_justification_attachments
 FOR EACH ROW EXECUTE FUNCTION nite_connect.protect_absence_justification_attachment();
