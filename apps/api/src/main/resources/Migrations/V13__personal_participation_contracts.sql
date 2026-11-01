-- Homologated weekly-load versions are empty until an institutional source is approved.
CREATE TABLE nite_connect.participation_load_policy_versions (
 id uuid PRIMARY KEY,
 version integer NOT NULL CHECK(version > 0),
 effective_from date NOT NULL,
 effective_until date,
 presential_hours numeric(7,2) NOT NULL CHECK(presential_hours >= 0),
 remote_hours numeric(7,2) NOT NULL CHECK(remote_hours >= 0),
 source_reference varchar(1000) NOT NULL CHECK(length(trim(source_reference)) > 0),
 approved_by uuid NOT NULL REFERENCES nite_connect.members(id),
 approved_at timestamptz NOT NULL,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 UNIQUE(version,effective_from),
 CHECK(effective_until IS NULL OR effective_until > effective_from)
);
CREATE INDEX participation_load_policy_effective_idx
 ON nite_connect.participation_load_policy_versions(effective_from,effective_until,version DESC);
CREATE FUNCTION nite_connect.protect_participation_load_policy_version() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Participation load policy versions cannot be deleted'; END IF;
 IF NEW.id<>OLD.id OR NEW.version<>OLD.version OR NEW.effective_from<>OLD.effective_from
    OR NEW.presential_hours<>OLD.presential_hours OR NEW.remote_hours<>OLD.remote_hours
    OR NEW.source_reference<>OLD.source_reference OR NEW.approved_by<>OLD.approved_by
    OR NEW.approved_at<>OLD.approved_at OR NEW.created_at<>OLD.created_at
    OR OLD.effective_until IS NOT NULL OR NEW.effective_until IS NULL
    OR NEW.effective_until<=OLD.effective_from THEN
  RAISE EXCEPTION 'Only the end of a participation load policy version may be recorded';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER protect_participation_load_policy_versions
 BEFORE UPDATE OR DELETE ON nite_connect.participation_load_policy_versions
 FOR EACH ROW EXECUTE FUNCTION nite_connect.protect_participation_load_policy_version();

-- Replace historical 5h/4h caps with explicit positive/relief amounts. They are
-- decisions attached to records, not proof of an institutionally homologated load.
ALTER TABLE nite_connect.participation_calendar_versions
 DROP CONSTRAINT participation_calendar_versions_presential_hours_relieved_check,
 DROP CONSTRAINT participation_calendar_versions_remote_hours_relieved_check,
 DROP CONSTRAINT participation_calendar_versions_check,
 ADD CONSTRAINT participation_calendar_hours_nonnegative CHECK (
   presential_hours_relieved >= 0 AND remote_hours_relieved >= 0
 ),
 ADD CONSTRAINT participation_calendar_exception_shape CHECK (
   (exception_type='CANCELAMENTO_PRESENCIAL' AND presential_hours_relieved>0 AND remote_hours_relieved=0
      AND affected_event_id IS NOT NULL AND replacement_event_id IS NULL)
   OR (exception_type='RECESSO_INTEGRAL' AND presential_hours_relieved>0 AND remote_hours_relieved>0
      AND affected_event_id IS NULL AND replacement_event_id IS NULL)
   OR (exception_type='SUSPENSAO_PRESENCIAL' AND presential_hours_relieved>0 AND remote_hours_relieved=0
      AND affected_event_id IS NULL AND replacement_event_id IS NULL)
   OR (exception_type='SUSPENSAO_PARCIAL' AND (presential_hours_relieved>0 OR remote_hours_relieved>0)
      AND affected_event_id IS NULL AND replacement_event_id IS NULL)
   OR (exception_type='REMARCACAO' AND presential_hours_relieved=0 AND remote_hours_relieved=0
      AND affected_event_id IS NOT NULL AND replacement_event_id IS NOT NULL
      AND affected_event_id<>replacement_event_id)
 );

ALTER TABLE nite_connect.participation_adjustments
 DROP CONSTRAINT IF EXISTS participation_adjustments_requested_hours_check,
 DROP CONSTRAINT IF EXISTS participation_adjustments_authorized_hours_check;
DO $$
DECLARE restriction record;
BEGIN
 FOR restriction IN
  SELECT conname FROM pg_constraint
  WHERE conrelid='nite_connect.participation_adjustments'::regclass AND contype='c'
    AND ((position('requested_hours' in lower(pg_get_constraintdef(oid)))>0
          AND position('<=' in lower(pg_get_constraintdef(oid)))>0
          AND position('case' in lower(pg_get_constraintdef(oid)))>0)
      OR (position('authorized_hours' in lower(pg_get_constraintdef(oid)))>0
          AND position('<=' in lower(pg_get_constraintdef(oid)))>0
          AND position('case' in lower(pg_get_constraintdef(oid)))>0))
 LOOP
  EXECUTE format('ALTER TABLE nite_connect.participation_adjustments DROP CONSTRAINT %I',restriction.conname);
 END LOOP;
END $$;
ALTER TABLE nite_connect.participation_adjustments
 ADD CONSTRAINT participation_adjustment_requested_hours_positive CHECK(requested_hours>0),
 ADD CONSTRAINT participation_adjustment_authorized_hours_positive CHECK(authorized_hours IS NULL OR authorized_hours>0);

-- Requests, member responses and final review decisions remain separately traceable.
ALTER TABLE nite_connect.participation_events DROP CONSTRAINT participation_events_entity_type_check;
ALTER TABLE nite_connect.participation_events ADD CONSTRAINT participation_events_entity_type_check CHECK (
 entity_type IN ('CALENDAR_EVENT','ATTENDANCE','PERIOD','JUSTIFICATION','CORRECTION','WORK_LOG_CORRECTION_RESPONSE')
);
ALTER TABLE nite_connect.audit_events ALTER COLUMN entity_type TYPE varchar(40);
ALTER TABLE nite_connect.audit_events DROP CONSTRAINT audit_entity_check;
ALTER TABLE nite_connect.audit_events ADD CONSTRAINT audit_entity_check CHECK (
 (entity_type='MEMBER' AND member_id IS NOT NULL AND (entity_id IS NULL OR entity_id=member_id)) OR
 (entity_type IN ('PROJECT','PROPOSAL','ACTIVITY','EVIDENCE','CALENDAR_EVENT','ATTENDANCE','PERIOD','JUSTIFICATION',
                  'CORRECTION','DOCUMENT','DOCUMENT_TEMPLATE','WORK_LOG_CORRECTION_RESPONSE')
     AND entity_id IS NOT NULL AND member_id IS NULL)
);

CREATE TABLE nite_connect.work_log_correction_responses (
 id uuid PRIMARY KEY,
 review_request_id uuid NOT NULL UNIQUE REFERENCES nite_connect.participation_review_batch_items(id),
 period_id uuid NOT NULL REFERENCES nite_connect.participation_periods(id),
 member_id uuid NOT NULL REFERENCES nite_connect.members(id),
 expected_period_version bigint NOT NULL CHECK(expected_period_version>=0),
 proposed_start timestamptz NOT NULL,
 proposed_end timestamptz NOT NULL CHECK(proposed_end>proposed_start),
 proposed_description varchar(10000) NOT NULL CHECK(length(trim(proposed_description))>0),
 proposed_activity_id uuid REFERENCES nite_connect.activities(id),
 proposed_evidence_id uuid,
 reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 responded_at timestamptz NOT NULL,
 version bigint NOT NULL DEFAULT 0 CHECK(version=0),
 FOREIGN KEY(proposed_evidence_id,proposed_activity_id)
   REFERENCES nite_connect.activity_evidence(id,activity_id),
 CHECK(proposed_evidence_id IS NULL OR proposed_activity_id IS NOT NULL),
 UNIQUE(id,period_id)
);
CREATE INDEX work_log_correction_responses_period_idx
 ON nite_connect.work_log_correction_responses(period_id,responded_at DESC,id);
CREATE TRIGGER immutable_work_log_correction_responses
 BEFORE UPDATE OR DELETE ON nite_connect.work_log_correction_responses
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();

CREATE TABLE nite_connect.work_log_correction_decisions (
 id uuid PRIMARY KEY,
 response_id uuid NOT NULL UNIQUE REFERENCES nite_connect.work_log_correction_responses(id),
 decision varchar(16) NOT NULL CHECK(decision IN ('ACEITA','RECUSADA')),
 expected_response_version bigint NOT NULL CHECK(expected_response_version=0),
 resulting_period_version bigint,
 decided_by uuid NOT NULL REFERENCES nite_connect.members(id),
 decided_at timestamptz NOT NULL,
 reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 CHECK((decision='ACEITA' AND resulting_period_version IS NOT NULL)
    OR (decision='RECUSADA' AND resulting_period_version IS NULL))
);
CREATE TRIGGER immutable_work_log_correction_decisions
 BEFORE UPDATE OR DELETE ON nite_connect.work_log_correction_decisions
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();

-- A manager-approved correction may change a work log's activity/evidence links;
-- the application still validates that each link is currently authorized.
CREATE OR REPLACE FUNCTION nite_connect.protect_participation_period() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Participation periods cannot be deleted'; END IF;
 IF TG_OP='UPDATE' THEN
  IF NEW.member_id<>OLD.member_id OR NEW.kind<>OLD.kind OR NEW.attendance_id IS DISTINCT FROM OLD.attendance_id
    OR NEW.created_at<>OLD.created_at OR NEW.id<>OLD.id OR NEW.version<>OLD.version+1 THEN
   RAISE EXCEPTION 'Period identity is immutable and version must advance' USING ERRCODE='23514';
  END IF;
 END IF;
 PERFORM pg_advisory_xact_lock(hashtextextended(NEW.member_id::text,42));
 IF EXISTS(SELECT 1 FROM nite_connect.participation_periods p WHERE p.member_id=NEW.member_id AND p.id<>NEW.id
   AND tstzrange(p.starts_at,p.ends_at,'[)') && tstzrange(NEW.starts_at,NEW.ends_at,'[)')) THEN
  RAISE EXCEPTION 'Overlapping participation intervals' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END;
$$;
