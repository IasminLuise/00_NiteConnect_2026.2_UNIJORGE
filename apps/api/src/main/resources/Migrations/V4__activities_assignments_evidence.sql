CREATE TABLE nite_connect.activities (
 id uuid PRIMARY KEY,
 title varchar(200) NOT NULL CHECK (length(trim(title))>0),
 description varchar(10000) NOT NULL CHECK (length(trim(description))>0),
 project_id uuid REFERENCES nite_connect.projects(id),
 status varchar(24) NOT NULL DEFAULT 'PLANEJADA' CHECK (status IN ('PLANEJADA','EM_EXECUCAO','CONCLUIDA','CANCELADA')),
 due_on date,
 created_by uuid NOT NULL REFERENCES nite_connect.members(id),
 version bigint NOT NULL DEFAULT 0 CHECK (version>=0),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 completed_at timestamptz,
 CHECK ((status='CONCLUIDA')=(completed_at IS NOT NULL))
);
CREATE INDEX activities_project_idx ON nite_connect.activities(project_id,status,due_on,id);
CREATE INDEX activities_status_due_idx ON nite_connect.activities(status,due_on,id);
CREATE INDEX activities_created_idx ON nite_connect.activities(created_at DESC,id);

CREATE TABLE nite_connect.activity_assignments (
 id uuid PRIMARY KEY,
 activity_id uuid NOT NULL REFERENCES nite_connect.activities(id),
 member_id uuid NOT NULL REFERENCES nite_connect.members(id),
 role varchar(24) NOT NULL CHECK (role IN ('EXECUTOR','COLLABORATOR')),
 started_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 started_by uuid NOT NULL REFERENCES nite_connect.members(id),
 start_reason varchar(1000) NOT NULL CHECK (length(trim(start_reason))>0),
 ended_at timestamptz,
 ended_by uuid REFERENCES nite_connect.members(id),
 end_reason varchar(1000),
 UNIQUE(id,activity_id,member_id),
 CHECK ((ended_at IS NULL AND ended_by IS NULL AND end_reason IS NULL) OR
  (ended_at IS NOT NULL AND ended_at>=started_at AND ended_by IS NOT NULL AND end_reason IS NOT NULL AND length(trim(end_reason))>0))
);
CREATE UNIQUE INDEX activity_current_member_idx ON nite_connect.activity_assignments(activity_id,member_id) WHERE ended_at IS NULL;
CREATE UNIQUE INDEX activity_current_executor_idx ON nite_connect.activity_assignments(activity_id) WHERE ended_at IS NULL AND role='EXECUTOR';
CREATE INDEX activity_assignment_member_idx ON nite_connect.activity_assignments(member_id,started_at,id);
CREATE INDEX activity_assignment_history_idx ON nite_connect.activity_assignments(activity_id,started_at,id);
CREATE TRIGGER preserve_activity_period BEFORE UPDATE OR DELETE ON nite_connect.activity_assignments
 FOR EACH ROW EXECUTE FUNCTION nite_connect.close_project_period_only();

CREATE FUNCTION nite_connect.require_activity_executor() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE target uuid;
BEGIN
 IF TG_TABLE_NAME='activities' THEN target=NEW.id; ELSE target=COALESCE(NEW.activity_id,OLD.activity_id); END IF;
 IF EXISTS(SELECT 1 FROM nite_connect.activities WHERE id=target) AND
  (SELECT count(*) FROM nite_connect.activity_assignments WHERE activity_id=target AND role='EXECUTOR' AND ended_at IS NULL)<>1 THEN
  RAISE EXCEPTION 'Activity must have exactly one current executor' USING ERRCODE='23514';
 END IF;
 RETURN NULL;
END;
$$;
CREATE CONSTRAINT TRIGGER activity_executor_required AFTER INSERT OR UPDATE ON nite_connect.activities
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION nite_connect.require_activity_executor();
CREATE CONSTRAINT TRIGGER assignment_executor_required AFTER INSERT OR UPDATE OR DELETE ON nite_connect.activity_assignments
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION nite_connect.require_activity_executor();
CREATE FUNCTION nite_connect.preserve_activity_context() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Activities cannot be deleted'; END IF;
 IF NEW.project_id IS DISTINCT FROM OLD.project_id OR NEW.created_by<>OLD.created_by OR NEW.created_at<>OLD.created_at THEN
  RAISE EXCEPTION 'Activity origin cannot change' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER preserve_activity BEFORE UPDATE OR DELETE ON nite_connect.activities
 FOR EACH ROW EXECUTE FUNCTION nite_connect.preserve_activity_context();

CREATE TABLE nite_connect.activity_events (
 id uuid PRIMARY KEY,
 activity_id uuid NOT NULL REFERENCES nite_connect.activities(id),
 actor_id uuid NOT NULL REFERENCES nite_connect.members(id),
 action varchar(64) NOT NULL,
 reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 occurred_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 activity_version bigint NOT NULL CHECK(activity_version>=0),
 before_value text,
 after_value text
);
CREATE INDEX activity_events_idx ON nite_connect.activity_events(activity_id,occurred_at DESC,id);
CREATE TRIGGER immutable_activity_events BEFORE UPDATE OR DELETE ON nite_connect.activity_events
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();

CREATE TABLE nite_connect.evidence_objects (
 id uuid PRIMARY KEY,
 activity_id uuid NOT NULL REFERENCES nite_connect.activities(id),
 author_id uuid NOT NULL REFERENCES nite_connect.members(id),
 object_key uuid NOT NULL UNIQUE,
 filename varchar(200) NOT NULL,
 content_type varchar(100),
 size_bytes bigint CHECK(size_bytes BETWEEN 1 AND 10485760),
 sha256 varchar(64),
 state varchar(16) NOT NULL CHECK(state IN ('PENDING','AVAILABLE','FAILED')),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 updated_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 UNIQUE(id,activity_id,author_id),
 CHECK(state<>'AVAILABLE' OR (content_type IS NOT NULL AND size_bytes IS NOT NULL AND sha256 IS NOT NULL AND sha256 ~ '^[0-9a-f]{64}$'))
);
CREATE INDEX evidence_objects_recovery_idx ON nite_connect.evidence_objects(state,updated_at,id);
CREATE TABLE nite_connect.activity_evidence (
 id uuid PRIMARY KEY,
 activity_id uuid NOT NULL REFERENCES nite_connect.activities(id),
 author_id uuid NOT NULL REFERENCES nite_connect.members(id),
 assignment_id uuid,
 type varchar(16) NOT NULL CHECK(type IN ('TEXT','LINK','TECHNICAL','FILE','IMAGE')),
 content varchar(10000),
 url varchar(2048),
 corrects_evidence_id uuid,
 object_id uuid UNIQUE,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 withdrawn_at timestamptz,
 withdrawn_by uuid REFERENCES nite_connect.members(id),
 withdraw_reason varchar(1000),
 UNIQUE(id,activity_id),
 FOREIGN KEY(assignment_id,activity_id,author_id) REFERENCES nite_connect.activity_assignments(id,activity_id,member_id),
 FOREIGN KEY(corrects_evidence_id,activity_id) REFERENCES nite_connect.activity_evidence(id,activity_id),
 FOREIGN KEY(object_id,activity_id,author_id) REFERENCES nite_connect.evidence_objects(id,activity_id,author_id),
 CHECK ((type IN ('TEXT','TECHNICAL') AND content IS NOT NULL AND length(trim(content))>0 AND url IS NULL AND object_id IS NULL)
  OR (type='LINK' AND url IS NOT NULL AND object_id IS NULL)
  OR (type IN ('FILE','IMAGE') AND object_id IS NOT NULL AND content IS NULL AND url IS NULL)),
 CHECK ((withdrawn_at IS NULL AND withdrawn_by IS NULL AND withdraw_reason IS NULL)
  OR (withdrawn_at IS NOT NULL AND withdrawn_at>=created_at AND withdrawn_by IS NOT NULL AND withdraw_reason IS NOT NULL AND length(trim(withdraw_reason))>0))
);
CREATE INDEX activity_evidence_idx ON nite_connect.activity_evidence(activity_id,created_at DESC,id);
CREATE INDEX evidence_author_idx ON nite_connect.activity_evidence(author_id,created_at DESC,id);
CREATE FUNCTION nite_connect.require_available_evidence_object() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NEW.object_id IS NOT NULL AND NOT EXISTS(
  SELECT 1 FROM nite_connect.evidence_objects WHERE id=NEW.object_id AND state='AVAILABLE'
   AND activity_id=NEW.activity_id AND author_id=NEW.author_id
 ) THEN RAISE EXCEPTION 'Evidence requires an available private object' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER require_available_evidence_object BEFORE INSERT ON nite_connect.activity_evidence
 FOR EACH ROW EXECUTE FUNCTION nite_connect.require_available_evidence_object();
CREATE FUNCTION nite_connect.preserve_evidence_object() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Upload receipts cannot be deleted'; END IF;
 IF OLD.state<>'PENDING' OR
  (to_jsonb(NEW)-ARRAY['filename','content_type','size_bytes','sha256','state','updated_at']) IS DISTINCT FROM
  (to_jsonb(OLD)-ARRAY['filename','content_type','size_bytes','sha256','state','updated_at']) THEN
  RAISE EXCEPTION 'Finalized upload metadata is immutable';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER preserve_evidence_object BEFORE UPDATE OR DELETE ON nite_connect.evidence_objects
 FOR EACH ROW EXECUTE FUNCTION nite_connect.preserve_evidence_object();
CREATE FUNCTION nite_connect.withdraw_evidence_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Evidence cannot be deleted'; END IF;
 IF OLD.withdrawn_at IS NOT NULL OR NEW.withdrawn_at IS NULL OR
  (to_jsonb(NEW)-ARRAY['withdrawn_at','withdrawn_by','withdraw_reason']) IS DISTINCT FROM
  (to_jsonb(OLD)-ARRAY['withdrawn_at','withdrawn_by','withdraw_reason']) THEN
  RAISE EXCEPTION 'Only withdrawing available evidence is allowed';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER preserve_evidence BEFORE UPDATE OR DELETE ON nite_connect.activity_evidence
 FOR EACH ROW EXECUTE FUNCTION nite_connect.withdraw_evidence_only();
ALTER TABLE nite_connect.audit_events DROP CONSTRAINT audit_entity_check;
ALTER TABLE nite_connect.audit_events ADD CONSTRAINT audit_entity_check CHECK (
 (entity_type='MEMBER' AND member_id IS NOT NULL AND (entity_id IS NULL OR entity_id=member_id)) OR
 (entity_type IN ('PROJECT','PROPOSAL','ACTIVITY','EVIDENCE') AND entity_id IS NOT NULL AND member_id IS NULL)
);
