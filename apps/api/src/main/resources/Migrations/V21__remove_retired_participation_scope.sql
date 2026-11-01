-- Preserve encounter attendance and its audit history while removing the retired
-- work-hour, justification, correction, policy, and retention data model.
DO $$
BEGIN
 IF EXISTS (
   SELECT 1 FROM nite_connect.participation_periods
   WHERE kind='ATTENDANCE' AND attendance_id IS NULL
 ) THEN
  RAISE EXCEPTION 'Cannot classify an attendance segment without attendance_id';
 END IF;
 IF EXISTS (
   SELECT 1 FROM nite_connect.participation_events e
   LEFT JOIN nite_connect.participation_periods p ON p.id=e.entity_id
   WHERE e.entity_type='PERIOD' AND p.id IS NULL
 ) THEN
  RAISE EXCEPTION 'Cannot classify a participation PERIOD event without its source row';
 END IF;
 IF EXISTS (
   SELECT 1 FROM nite_connect.audit_events e
   LEFT JOIN nite_connect.participation_periods p ON p.id=e.entity_id
   WHERE e.entity_type='PERIOD' AND p.id IS NULL
 ) THEN
  RAISE EXCEPTION 'Cannot classify an audit PERIOD event without its source row';
 END IF;
END;
$$;

CREATE TABLE nite_connect.attendance_segments (
 id uuid PRIMARY KEY,
 member_id uuid NOT NULL REFERENCES nite_connect.members(id),
 attendance_id uuid NOT NULL,
 starts_at timestamptz NOT NULL,
 ends_at timestamptz,
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 CHECK(ends_at IS NULL OR ends_at>starts_at),
 FOREIGN KEY(attendance_id,member_id) REFERENCES nite_connect.attendance_records(id,member_id)
);
CREATE INDEX attendance_segments_attendance_idx
 ON nite_connect.attendance_segments(attendance_id,starts_at,id);
CREATE INDEX attendance_segments_member_idx
 ON nite_connect.attendance_segments(member_id,starts_at,id);
CREATE UNIQUE INDEX attendance_segments_open_member_idx
 ON nite_connect.attendance_segments(member_id) WHERE ends_at IS NULL;

INSERT INTO nite_connect.attendance_segments(id,member_id,attendance_id,starts_at,ends_at,version,created_at)
 SELECT id,member_id,attendance_id,starts_at,ends_at,version,created_at
 FROM nite_connect.participation_periods
 WHERE kind='ATTENDANCE';

CREATE TABLE nite_connect.attendance_events (
 id uuid PRIMARY KEY,
 entity_type varchar(32) NOT NULL
   CHECK(entity_type IN ('CALENDAR_EVENT','ATTENDANCE','ATTENDANCE_SEGMENT')),
 entity_id uuid NOT NULL,
 member_id uuid REFERENCES nite_connect.members(id),
 actor_id uuid NOT NULL REFERENCES nite_connect.members(id),
 action varchar(64) NOT NULL,
 entity_version bigint NOT NULL DEFAULT 0 CHECK(entity_version>=0),
 before_value text,
 after_value text,
 reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 occurred_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX attendance_history_member_idx
 ON nite_connect.attendance_events(member_id,occurred_at DESC,id);
CREATE INDEX attendance_history_entity_idx
 ON nite_connect.attendance_events(entity_type,entity_id,entity_version);
CREATE TRIGGER immutable_attendance_events BEFORE UPDATE OR DELETE ON nite_connect.attendance_events
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();

INSERT INTO nite_connect.attendance_events(
 id,entity_type,entity_id,member_id,actor_id,action,entity_version,before_value,after_value,reason,occurred_at
)
SELECT e.id,
       CASE WHEN e.entity_type='PERIOD' THEN 'ATTENDANCE_SEGMENT' ELSE e.entity_type END,
       e.entity_id,e.member_id,e.actor_id,e.action,e.entity_version,e.before_value,e.after_value,e.reason,e.occurred_at
FROM nite_connect.participation_events e
LEFT JOIN nite_connect.participation_periods p
 ON p.id=e.entity_id AND e.entity_type='PERIOD'
WHERE e.entity_type IN ('CALENDAR_EVENT','ATTENDANCE')
   OR (e.entity_type='PERIOD' AND p.kind='ATTENDANCE');

CREATE FUNCTION nite_connect.protect_attendance_segment() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Attendance segments cannot be deleted'; END IF;
 IF TG_OP='UPDATE' AND (
    NEW.member_id<>OLD.member_id OR NEW.attendance_id<>OLD.attendance_id
    OR NEW.created_at<>OLD.created_at OR NEW.id<>OLD.id OR NEW.version<>OLD.version+1
 ) THEN
  RAISE EXCEPTION 'Attendance segment identity is immutable and version must advance' USING ERRCODE='23514';
 END IF;
 PERFORM pg_advisory_xact_lock(hashtextextended(NEW.member_id::text,42));
 IF EXISTS(
   SELECT 1 FROM nite_connect.attendance_segments s
   WHERE s.member_id=NEW.member_id AND s.id<>NEW.id
     AND tstzrange(s.starts_at,s.ends_at,'[)') && tstzrange(NEW.starts_at,NEW.ends_at,'[)')
 ) THEN
  RAISE EXCEPTION 'Overlapping attendance segments' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER protect_attendance_segment BEFORE INSERT OR UPDATE OR DELETE ON nite_connect.attendance_segments
 FOR EACH ROW EXECUTE FUNCTION nite_connect.protect_attendance_segment();

CREATE FUNCTION nite_connect.require_attendance_segment_history() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(
   SELECT 1 FROM nite_connect.attendance_events e
   WHERE e.entity_type='ATTENDANCE_SEGMENT' AND e.entity_id=NEW.id
     AND e.entity_version=NEW.version AND e.before_value IS NOT NULL AND e.after_value IS NOT NULL
 ) THEN
  RAISE EXCEPTION 'Attendance segment updates require immutable history' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END;
$$;
CREATE CONSTRAINT TRIGGER attendance_segment_history_required AFTER UPDATE ON nite_connect.attendance_segments
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION nite_connect.require_attendance_segment_history();

-- The generic audit trail is immutable at runtime. This migration performs a
-- one-time type conversion for retained attendance segments and deletes only
-- events tied to the retired B04 entities.
ALTER TABLE nite_connect.audit_events DROP CONSTRAINT audit_entity_check;
DROP TRIGGER immutable_audit_events ON nite_connect.audit_events;
DELETE FROM nite_connect.audit_events e
 WHERE e.entity_type IN ('JUSTIFICATION','CORRECTION','WORK_LOG_CORRECTION_RESPONSE')
    OR (e.entity_type='PERIOD' AND EXISTS(
       SELECT 1 FROM nite_connect.participation_periods p
       WHERE p.id=e.entity_id AND p.kind='WORK_LOG'
    ));
UPDATE nite_connect.audit_events e SET entity_type='ATTENDANCE_SEGMENT'
 WHERE e.entity_type='PERIOD' AND EXISTS(
   SELECT 1 FROM nite_connect.participation_periods p
   WHERE p.id=e.entity_id AND p.kind='ATTENDANCE'
 );
ALTER TABLE nite_connect.audit_events ADD CONSTRAINT audit_entity_check CHECK (
 (entity_type='MEMBER' AND member_id IS NOT NULL AND (entity_id IS NULL OR entity_id=member_id)) OR
 (entity_type IN ('PROJECT','PROPOSAL','ACTIVITY','EVIDENCE','CALENDAR_EVENT','ATTENDANCE','ATTENDANCE_SEGMENT',
                  'DOCUMENT','DOCUMENT_TEMPLATE') AND entity_id IS NOT NULL AND member_id IS NULL)
);
CREATE TRIGGER immutable_audit_events BEFORE UPDATE OR DELETE ON nite_connect.audit_events
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();

-- Drop child records explicitly; no CASCADE is used so unclassified dependencies
-- stop this migration instead of silently widening the deletion.
DROP TABLE nite_connect.justification_attachment_retention_purge_items;
DROP TABLE nite_connect.justification_attachment_retention_purge_run_events;
DROP TABLE nite_connect.absence_justification_attachment_retention_holds;
DROP TABLE nite_connect.absence_justification_attachment_events;
DROP TABLE nite_connect.absence_justification_retention_classifications;
DROP TABLE nite_connect.absence_justification_attachments;
DROP TABLE nite_connect.justification_attachment_retention_events;
DROP TABLE nite_connect.justification_attachment_retention_purge_runs;
DROP TABLE nite_connect.justification_attachment_retention_policies;
DROP TABLE nite_connect.work_log_correction_decisions;
DROP TABLE nite_connect.work_log_correction_responses;
DROP TABLE nite_connect.participation_review_batch_items;
DROP TABLE nite_connect.participation_review_batches;
DROP TABLE nite_connect.participation_replacement_allocations;
DROP TABLE nite_connect.participation_adjustment_events;
DROP TABLE nite_connect.participation_adjustments;
DROP TABLE nite_connect.correction_requests;
DROP TABLE nite_connect.absence_justifications;
DROP TABLE nite_connect.participation_calendar_versions;
DROP TABLE nite_connect.participation_load_policy_events;
DROP TABLE nite_connect.participation_load_policy_drafts;
DROP TABLE nite_connect.participation_load_policy_versions;
DROP TABLE nite_connect.participation_periods;
DROP TABLE nite_connect.participation_events;

DROP FUNCTION nite_connect.protect_participation_period();
DROP FUNCTION nite_connect.require_participation_history();
DROP FUNCTION nite_connect.protect_participation_calendar_version();
DROP FUNCTION nite_connect.protect_participation_adjustment();
DROP FUNCTION nite_connect.require_participation_adjustment_event();
DROP FUNCTION nite_connect.protect_participation_load_policy_version();
DROP FUNCTION nite_connect.protect_participation_load_policy_event();
DROP FUNCTION nite_connect.protect_participation_load_policy_draft();
DROP FUNCTION nite_connect.reject_justification_attachment_history_mutation();
DROP FUNCTION nite_connect.protect_justification_attachment_retention_policy();
DROP FUNCTION nite_connect.protect_absence_justification_attachment();
DROP FUNCTION nite_connect.reject_justification_attachment_purge_history_mutation();

COMMENT ON TABLE nite_connect.attendance_segments IS 'Presence intervals within an authorized NITE encounter; this table does not calculate work hours.';
COMMENT ON TABLE nite_connect.attendance_events IS 'Append-only history for agenda events, attendance records, and attendance segments.';
