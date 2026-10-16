CREATE TABLE nite_connect.calendar_events (
 id uuid PRIMARY KEY, title varchar(200) NOT NULL CHECK(length(trim(title))>0),
 description varchar(10000) NOT NULL,
 kind varchar(24) NOT NULL CHECK(kind IN ('ENCONTRO','EXTRAORDINARIO','FERIADO','EVENTO')),
 modality varchar(16) NOT NULL CHECK(modality IN ('PRESENCIAL','REMOTA')),
 starts_at timestamptz NOT NULL, ends_at timestamptz NOT NULL CHECK(ends_at>starts_at),
 status varchar(16) NOT NULL DEFAULT 'AGENDADO' CHECK(status IN ('AGENDADO','ABERTO','ENCERRADO','CANCELADO')),
 qr_hash varchar(64), qr_expires_at timestamptz,
 created_by uuid NOT NULL REFERENCES nite_connect.members(id),
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 CHECK((qr_hash IS NULL AND qr_expires_at IS NULL) OR (status='ABERTO' AND qr_hash IS NOT NULL AND qr_expires_at IS NOT NULL))
);
CREATE INDEX calendar_dates_idx ON nite_connect.calendar_events(starts_at,id);
CREATE TABLE nite_connect.event_participants (
 event_id uuid NOT NULL REFERENCES nite_connect.calendar_events(id),
 member_id uuid NOT NULL REFERENCES nite_connect.members(id),
 PRIMARY KEY(event_id,member_id)
);
CREATE TABLE nite_connect.attendance_records (
 id uuid PRIMARY KEY, event_id uuid NOT NULL REFERENCES nite_connect.calendar_events(id),
 member_id uuid NOT NULL REFERENCES nite_connect.members(id),
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
 UNIQUE(event_id,member_id), UNIQUE(id,member_id),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX attendance_member_idx ON nite_connect.attendance_records(member_id,event_id);
CREATE TABLE nite_connect.participation_periods (
 id uuid PRIMARY KEY, member_id uuid NOT NULL REFERENCES nite_connect.members(id),
 attendance_id uuid, kind varchar(16) NOT NULL CHECK(kind IN ('ATTENDANCE','WORK_LOG')),
 modality varchar(16) NOT NULL CHECK(modality IN ('PRESENCIAL','REMOTA')),
 starts_at timestamptz NOT NULL, ends_at timestamptz CHECK(ends_at>starts_at),
 description varchar(10000) NOT NULL DEFAULT '',
 activity_id uuid REFERENCES nite_connect.activities(id),
 evidence_id uuid,
 origin varchar(16) NOT NULL CHECK(origin IN ('QR','DECLARATION','CORRECTION')),
 review_status varchar(16) NOT NULL DEFAULT 'PENDENTE' CHECK(review_status IN ('PENDENTE','APROVADA','RECUSADA')),
 reviewed_by uuid REFERENCES nite_connect.members(id), reviewed_at timestamptz, review_reason varchar(1000),
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 FOREIGN KEY(attendance_id,member_id) REFERENCES nite_connect.attendance_records(id,member_id),
 FOREIGN KEY(evidence_id,activity_id) REFERENCES nite_connect.activity_evidence(id,activity_id),
 CHECK(evidence_id IS NULL OR activity_id IS NOT NULL),
 CHECK((kind='ATTENDANCE' AND attendance_id IS NOT NULL AND activity_id IS NULL AND evidence_id IS NULL)
    OR (kind='WORK_LOG' AND attendance_id IS NULL AND ends_at IS NOT NULL AND modality='REMOTA' AND length(trim(description))>0)),
 CHECK((review_status='PENDENTE' AND reviewed_by IS NULL AND reviewed_at IS NULL AND review_reason IS NULL)
    OR (review_status<>'PENDENTE' AND ends_at IS NOT NULL AND reviewed_by IS NOT NULL AND reviewed_by<>member_id
        AND reviewed_at IS NOT NULL AND length(trim(review_reason))>0))
);
CREATE INDEX participation_member_dates_idx ON nite_connect.participation_periods(member_id,starts_at,id);
CREATE INDEX participation_attendance_idx ON nite_connect.participation_periods(attendance_id,starts_at,id);
CREATE UNIQUE INDEX participation_open_member_idx ON nite_connect.participation_periods(member_id) WHERE ends_at IS NULL;
CREATE TABLE nite_connect.absence_justifications (
 id uuid PRIMARY KEY, member_id uuid NOT NULL REFERENCES nite_connect.members(id),
 event_id uuid NOT NULL REFERENCES nite_connect.calendar_events(id),
 reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 status varchar(32) NOT NULL DEFAULT 'PENDENTE' CHECK(status IN ('PENDENTE','COMPLEMENTACAO_SOLICITADA','ACEITA','RECUSADA')),
 decision_reason varchar(1000), decided_by uuid REFERENCES nite_connect.members(id), decided_at timestamptz,
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0), created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 UNIQUE(member_id,event_id),
 CHECK((status='PENDENTE' AND decided_by IS NULL AND decided_at IS NULL AND decision_reason IS NULL)
    OR (status<>'PENDENTE' AND decided_by IS NOT NULL AND decided_by<>member_id AND decided_at IS NOT NULL AND length(trim(decision_reason))>0))
);
CREATE INDEX justifications_member_idx ON nite_connect.absence_justifications(member_id,created_at,id);
CREATE TABLE nite_connect.correction_requests (
 id uuid PRIMARY KEY, member_id uuid NOT NULL REFERENCES nite_connect.members(id),
 period_id uuid REFERENCES nite_connect.participation_periods(id),
 event_id uuid REFERENCES nite_connect.calendar_events(id),
 proposed_start timestamptz NOT NULL, proposed_end timestamptz NOT NULL CHECK(proposed_end>proposed_start),
 reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 status varchar(16) NOT NULL DEFAULT 'PENDENTE' CHECK(status IN ('PENDENTE','ACEITA','RECUSADA')),
 decision_reason varchar(1000), decided_by uuid REFERENCES nite_connect.members(id), decided_at timestamptz,
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0), created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 CHECK((period_id IS NOT NULL AND event_id IS NULL) OR (period_id IS NULL AND event_id IS NOT NULL)),
 CHECK((status='PENDENTE' AND decided_by IS NULL AND decided_at IS NULL AND decision_reason IS NULL)
    OR (status<>'PENDENTE' AND decided_by IS NOT NULL AND decided_by<>member_id AND decided_at IS NOT NULL AND length(trim(decision_reason))>0))
);
CREATE INDEX corrections_member_idx ON nite_connect.correction_requests(member_id,created_at,id);
CREATE UNIQUE INDEX correction_pending_period_idx ON nite_connect.correction_requests(period_id) WHERE status='PENDENTE';
CREATE UNIQUE INDEX correction_pending_event_idx ON nite_connect.correction_requests(member_id,event_id) WHERE status='PENDENTE';
CREATE TABLE nite_connect.participation_events (
 id uuid PRIMARY KEY, entity_type varchar(32) NOT NULL CHECK(entity_type IN ('CALENDAR_EVENT','ATTENDANCE','PERIOD','JUSTIFICATION','CORRECTION')),
 entity_id uuid NOT NULL, member_id uuid REFERENCES nite_connect.members(id), actor_id uuid NOT NULL REFERENCES nite_connect.members(id),
 action varchar(64) NOT NULL, entity_version bigint NOT NULL DEFAULT 0 CHECK(entity_version>=0),
 before_value text, after_value text, reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 occurred_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX participation_history_member_idx ON nite_connect.participation_events(member_id,occurred_at DESC,id);
CREATE INDEX participation_history_entity_idx ON nite_connect.participation_events(entity_type,entity_id,entity_version);
CREATE TRIGGER immutable_participation_events BEFORE UPDATE OR DELETE ON nite_connect.participation_events
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();

CREATE FUNCTION nite_connect.protect_participation_period() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Participation periods cannot be deleted'; END IF;
 IF TG_OP='UPDATE' THEN
  IF NEW.member_id<>OLD.member_id OR NEW.kind<>OLD.kind OR NEW.attendance_id IS DISTINCT FROM OLD.attendance_id
    OR NEW.activity_id IS DISTINCT FROM OLD.activity_id OR NEW.evidence_id IS DISTINCT FROM OLD.evidence_id
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
CREATE TRIGGER protect_participation_period BEFORE INSERT OR UPDATE OR DELETE ON nite_connect.participation_periods
 FOR EACH ROW EXECUTE FUNCTION nite_connect.protect_participation_period();
CREATE FUNCTION nite_connect.require_participation_history() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM nite_connect.participation_events WHERE entity_type='PERIOD' AND entity_id=NEW.id
   AND entity_version=NEW.version AND before_value IS NOT NULL AND after_value IS NOT NULL) THEN
  RAISE EXCEPTION 'Period updates require immutable history' USING ERRCODE='23514';
 END IF;
 RETURN NEW;
END;
$$;
CREATE CONSTRAINT TRIGGER participation_history_required AFTER UPDATE ON nite_connect.participation_periods
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION nite_connect.require_participation_history();
CREATE FUNCTION nite_connect.reject_participation_delete() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION 'Participation records cannot be deleted';
END;
$$;
CREATE TRIGGER preserve_calendar BEFORE DELETE ON nite_connect.calendar_events FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_participation_delete();
CREATE TRIGGER preserve_attendance BEFORE DELETE ON nite_connect.attendance_records FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_participation_delete();
CREATE TRIGGER preserve_justifications BEFORE DELETE ON nite_connect.absence_justifications FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_participation_delete();
CREATE TRIGGER preserve_corrections BEFORE DELETE ON nite_connect.correction_requests FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_participation_delete();
ALTER TABLE nite_connect.audit_events DROP CONSTRAINT audit_entity_check;
ALTER TABLE nite_connect.audit_events ADD CONSTRAINT audit_entity_check CHECK (
 (entity_type='MEMBER' AND member_id IS NOT NULL AND (entity_id IS NULL OR entity_id=member_id)) OR
 (entity_type IN ('PROJECT','PROPOSAL','ACTIVITY','EVIDENCE','CALENDAR_EVENT','ATTENDANCE','PERIOD','JUSTIFICATION','CORRECTION') AND entity_id IS NOT NULL AND member_id IS NULL)
);
