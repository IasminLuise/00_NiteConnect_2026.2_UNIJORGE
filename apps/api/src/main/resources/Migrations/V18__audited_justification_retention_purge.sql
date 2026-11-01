CREATE TABLE nite_connect.justification_attachment_retention_purge_runs (
 id uuid PRIMARY KEY,
 mode varchar(12) NOT NULL CHECK(mode IN ('DRY_RUN','EXECUTE','SCHEDULED')),
 status varchar(24) NOT NULL CHECK(status IN
   ('INICIADA','SIMULACAO','BLOQUEADA','CONCLUIDA','CONCLUIDA_COM_FALHAS','FALHA')),
 result_code varchar(64),
 retention_policy_id uuid REFERENCES nite_connect.justification_attachment_retention_policies(id),
 requested_by uuid REFERENCES nite_connect.members(id),
 requested_at timestamptz NOT NULL,
 completed_at timestamptz,
 reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 candidate_count integer NOT NULL DEFAULT 0 CHECK(candidate_count>=0),
 skipped_count integer NOT NULL DEFAULT 0 CHECK(skipped_count>=0),
 deleted_count integer NOT NULL DEFAULT 0 CHECK(deleted_count>=0),
 failed_count integer NOT NULL DEFAULT 0 CHECK(failed_count>=0),
 CHECK((status='INICIADA' AND completed_at IS NULL) OR (status<>'INICIADA' AND completed_at IS NOT NULL))
);
CREATE INDEX justification_attachment_retention_purge_runs_idx
 ON nite_connect.justification_attachment_retention_purge_runs(requested_at DESC,id);

CREATE TABLE nite_connect.justification_attachment_retention_purge_run_events (
 id uuid PRIMARY KEY,
 run_id uuid NOT NULL REFERENCES nite_connect.justification_attachment_retention_purge_runs(id),
 event_type varchar(24) NOT NULL CHECK(event_type IN
   ('INICIADO','BLOQUEADO','SIMULACAO_CONCLUIDA','EXECUCAO_CONCLUIDA','EXECUCAO_COM_FALHAS','FALHA')),
 actor_id uuid REFERENCES nite_connect.members(id),
 occurred_at timestamptz NOT NULL,
 reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 result_code varchar(64),
 candidate_count integer NOT NULL CHECK(candidate_count>=0),
 skipped_count integer NOT NULL CHECK(skipped_count>=0),
 deleted_count integer NOT NULL CHECK(deleted_count>=0),
 failed_count integer NOT NULL CHECK(failed_count>=0)
);
CREATE INDEX justification_attachment_retention_purge_run_events_idx
 ON nite_connect.justification_attachment_retention_purge_run_events(run_id,occurred_at,id);

CREATE TABLE nite_connect.justification_attachment_retention_purge_items (
 id uuid PRIMARY KEY,
 run_id uuid NOT NULL REFERENCES nite_connect.justification_attachment_retention_purge_runs(id),
 attachment_id uuid NOT NULL REFERENCES nite_connect.absence_justification_attachments(id),
 outcome varchar(12) NOT NULL CHECK(outcome IN ('ELEGIVEL','IGNORADO','EXCLUIDO','FALHA')),
 result_code varchar(64),
 retention_expires_on date NOT NULL,
 deletion_deadline_on date NOT NULL,
 occurred_at timestamptz NOT NULL,
 UNIQUE(run_id,attachment_id)
);
CREATE INDEX justification_attachment_retention_purge_items_idx
 ON nite_connect.justification_attachment_retention_purge_items(run_id,occurred_at,attachment_id);

ALTER TABLE nite_connect.absence_justification_attachment_events
 ALTER COLUMN actor_id DROP NOT NULL;
ALTER TABLE nite_connect.absence_justification_attachment_events
 ADD COLUMN retention_purge_run_id uuid
   REFERENCES nite_connect.justification_attachment_retention_purge_runs(id);
ALTER TABLE nite_connect.absence_justification_attachment_events
 ADD CONSTRAINT justification_attachment_event_actor_source
 CHECK((actor_id IS NOT NULL AND retention_purge_run_id IS NULL)
    OR (actor_id IS NULL AND retention_purge_run_id IS NOT NULL
        AND event_type IN ('EXCLUSAO_INICIADA','EXCLUIDO','EXCLUSAO_FALHOU')));

CREATE FUNCTION nite_connect.reject_justification_attachment_purge_history_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'Justification attachment purge history is append-only'; END;
$$;
CREATE TRIGGER immutable_justification_attachment_retention_purge_run_events
 BEFORE UPDATE OR DELETE ON nite_connect.justification_attachment_retention_purge_run_events
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_justification_attachment_purge_history_mutation();
CREATE TRIGGER immutable_justification_attachment_retention_purge_items
 BEFORE UPDATE OR DELETE ON nite_connect.justification_attachment_retention_purge_items
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_justification_attachment_purge_history_mutation();

CREATE INDEX absence_justification_retention_purge_candidates_idx
 ON nite_connect.absence_justification_attachments(id)
 WHERE state IN ('DISPONIVEL','EXCLUSAO_PENDENTE');
