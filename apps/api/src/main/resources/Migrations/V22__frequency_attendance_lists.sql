ALTER TABLE nite_connect.calendar_events
    ADD COLUMN frequency_quick boolean NOT NULL DEFAULT false;
ALTER TABLE nite_connect.calendar_events
    ALTER COLUMN ends_at DROP NOT NULL;
ALTER TABLE nite_connect.calendar_events
    ADD CONSTRAINT calendar_events_quick_end_check
        CHECK (ends_at IS NOT NULL OR (frequency_quick AND status='ABERTO'));

CREATE TABLE nite_connect.frequency_settings (
    id smallint PRIMARY KEY CHECK (id=1),
    activated_at timestamptz NOT NULL
);
INSERT INTO nite_connect.frequency_settings(id,activated_at) VALUES (1,clock_timestamp());

CREATE TABLE nite_connect.attendance_lists (
    id uuid PRIMARY KEY,
    calendar_event_id uuid NOT NULL UNIQUE REFERENCES nite_connect.calendar_events(id),
    meeting_name varchar(200) NOT NULL CHECK (length(trim(meeting_name))>0),
    classification varchar(16) NOT NULL CHECK (classification IN ('REGULAR','EXCEPTIONAL')),
    counts_for_frequency boolean NOT NULL,
    confirmation_window_minutes smallint NOT NULL CHECK (confirmation_window_minutes IN (3,5,10)),
    confirmation_deadline timestamptz NOT NULL,
    opened_at timestamptz NOT NULL,
    opened_by uuid NOT NULL REFERENCES nite_connect.members(id),
    closed_at timestamptz,
    closed_by uuid REFERENCES nite_connect.members(id),
    status varchar(16) NOT NULL DEFAULT 'ABERTA' CHECK (status IN ('ABERTA','CONCLUIDA','CANCELADA')),
    outcome varchar(32) CHECK (outcome IN ('REALIZADA_CONTABILIZAVEL','REALIZADA_NAO_CONTABILIZAVEL','NAO_REALIZADA')),
    version bigint NOT NULL DEFAULT 0 CHECK (version>=0),
    qr_token_hash varchar(64) CHECK (qr_token_hash IS NULL OR length(qr_token_hash)=64),
    qr_expires_at timestamptz,
    qr_version integer NOT NULL DEFAULT 0 CHECK (qr_version>=0),
    idempotency_key varchar(128) NOT NULL CHECK (length(trim(idempotency_key))>0),
    request_hash varchar(64) NOT NULL CHECK (length(request_hash)=64),
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    UNIQUE(opened_by,idempotency_key),
    CHECK (classification<>'REGULAR' OR counts_for_frequency),
    CHECK ((status='ABERTA' AND closed_at IS NULL AND closed_by IS NULL AND outcome IS NULL)
        OR (status='CONCLUIDA' AND closed_at IS NOT NULL AND closed_by IS NOT NULL AND outcome IS NOT NULL)
        OR (status='CANCELADA' AND closed_at IS NOT NULL AND closed_by IS NOT NULL AND outcome='NAO_REALIZADA')),
    CHECK ((qr_token_hash IS NULL AND qr_expires_at IS NULL)
        OR (status='ABERTA' AND qr_token_hash IS NOT NULL AND qr_expires_at IS NOT NULL AND qr_version>0)),
    CHECK (confirmation_deadline=opened_at + confirmation_window_minutes * interval '1 minute')
);
CREATE INDEX attendance_lists_opened_idx ON nite_connect.attendance_lists(opened_at DESC,id);
CREATE INDEX attendance_lists_status_opened_idx ON nite_connect.attendance_lists(status,opened_at DESC,id);

CREATE TABLE nite_connect.attendance_list_members (
    list_id uuid NOT NULL REFERENCES nite_connect.attendance_lists(id),
    member_id uuid NOT NULL REFERENCES nite_connect.members(id),
    display_name varchar(160) NOT NULL CHECK (length(trim(display_name))>0),
    eligibility_status varchar(16) NOT NULL CHECK (eligibility_status='ATIVO'),
    captured_at timestamptz NOT NULL,
    sort_key text NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'AGUARDANDO'
        CHECK (status IN ('AGUARDANDO','PRESENTE','JUSTIFICADO','AUSENTE')),
    attendance_id uuid UNIQUE,
    confirmed_at timestamptz,
    changed_at timestamptz,
    changed_by uuid REFERENCES nite_connect.members(id),
    reason varchar(1000),
    version bigint NOT NULL DEFAULT 0 CHECK (version>=0),
    PRIMARY KEY(list_id,member_id),
    FOREIGN KEY(attendance_id,member_id) REFERENCES nite_connect.attendance_records(id,member_id),
    CHECK ((status='PRESENTE' AND attendance_id IS NOT NULL AND confirmed_at IS NOT NULL)
        OR (status<>'PRESENTE' AND attendance_id IS NULL AND confirmed_at IS NULL)),
    CHECK ((changed_at IS NULL AND changed_by IS NULL AND reason IS NULL)
        OR (changed_at IS NOT NULL AND changed_by IS NOT NULL))
);
CREATE INDEX attendance_list_members_order_idx ON nite_connect.attendance_list_members(list_id,sort_key,display_name,member_id);
CREATE INDEX attendance_list_members_member_idx ON nite_connect.attendance_list_members(member_id,list_id);

CREATE TABLE nite_connect.attendance_list_events (
    id uuid PRIMARY KEY,
    list_id uuid NOT NULL REFERENCES nite_connect.attendance_lists(id),
    member_id uuid REFERENCES nite_connect.members(id),
    actor_id uuid NOT NULL REFERENCES nite_connect.members(id),
    action varchar(64) NOT NULL,
    list_version bigint NOT NULL CHECK (list_version>=0),
    member_version bigint CHECK (member_version IS NULL OR member_version>=0),
    before_value text,
    after_value text NOT NULL,
    reason varchar(1000) NOT NULL CHECK (length(trim(reason))>0),
    occurred_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX attendance_list_events_history_idx ON nite_connect.attendance_list_events(list_id,list_version,id);
CREATE INDEX attendance_list_events_member_idx ON nite_connect.attendance_list_events(member_id,occurred_at DESC,id);
CREATE TRIGGER immutable_attendance_list_events BEFORE UPDATE OR DELETE ON nite_connect.attendance_list_events
    FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();

CREATE TABLE nite_connect.attendance_list_qr_tokens (
    token_hash varchar(64) PRIMARY KEY CHECK (length(token_hash)=64),
    list_id uuid NOT NULL REFERENCES nite_connect.attendance_lists(id),
    qr_version integer NOT NULL CHECK (qr_version>0),
    issued_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    UNIQUE(list_id,qr_version),
    CHECK (expires_at>issued_at),
    CHECK (revoked_at IS NULL OR revoked_at>=issued_at)
);
CREATE UNIQUE INDEX attendance_list_one_unrevoked_qr_idx
    ON nite_connect.attendance_list_qr_tokens(list_id) WHERE revoked_at IS NULL;
CREATE INDEX attendance_list_qr_token_list_idx ON nite_connect.attendance_list_qr_tokens(list_id,qr_version DESC);

CREATE TABLE nite_connect.frequency_qr_handoffs (
    handoff_hash varchar(64) PRIMARY KEY CHECK (length(handoff_hash)=64),
    list_id uuid NOT NULL REFERENCES nite_connect.attendance_lists(id),
    token_hash varchar(64) NOT NULL REFERENCES nite_connect.attendance_list_qr_tokens(token_hash),
    qr_version integer NOT NULL CHECK (qr_version>0),
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    consumed_at timestamptz,
    consumed_by uuid REFERENCES nite_connect.members(id),
    CHECK ((consumed_at IS NULL AND consumed_by IS NULL) OR (consumed_at IS NOT NULL AND consumed_by IS NOT NULL))
);
CREATE INDEX frequency_qr_handoffs_list_idx ON nite_connect.frequency_qr_handoffs(list_id,expires_at);

CREATE FUNCTION nite_connect.protect_attendance_list() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN
        RAISE EXCEPTION 'Attendance lists cannot be deleted';
    END IF;
    IF NEW.id<>OLD.id OR NEW.calendar_event_id<>OLD.calendar_event_id OR NEW.meeting_name<>OLD.meeting_name
       OR NEW.classification<>OLD.classification OR NEW.counts_for_frequency<>OLD.counts_for_frequency
       OR NEW.confirmation_window_minutes<>OLD.confirmation_window_minutes OR NEW.confirmation_deadline<>OLD.confirmation_deadline
       OR NEW.opened_at<>OLD.opened_at OR NEW.opened_by<>OLD.opened_by
       OR NEW.idempotency_key<>OLD.idempotency_key OR NEW.request_hash<>OLD.request_hash OR NEW.created_at<>OLD.created_at
       OR NEW.version<>OLD.version+1 THEN
        RAISE EXCEPTION 'Attendance list identity is immutable and version must advance' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER protect_attendance_list BEFORE UPDATE OR DELETE ON nite_connect.attendance_lists
    FOR EACH ROW EXECUTE FUNCTION nite_connect.protect_attendance_list();

CREATE FUNCTION nite_connect.require_attendance_list_history() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM nite_connect.attendance_list_events e
        WHERE e.list_id=NEW.id AND e.list_version=NEW.version
    ) THEN
        RAISE EXCEPTION 'Attendance list updates require immutable history' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE CONSTRAINT TRIGGER attendance_list_history_required AFTER UPDATE ON nite_connect.attendance_lists
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION nite_connect.require_attendance_list_history();

CREATE FUNCTION nite_connect.protect_attendance_list_member() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='DELETE' THEN
        RAISE EXCEPTION 'Attendance list snapshots cannot be deleted';
    END IF;
    IF NEW.list_id<>OLD.list_id OR NEW.member_id<>OLD.member_id OR NEW.display_name<>OLD.display_name
       OR NEW.eligibility_status<>OLD.eligibility_status OR NEW.captured_at<>OLD.captured_at OR NEW.sort_key<>OLD.sort_key
       OR NEW.version<>OLD.version+1 THEN
        RAISE EXCEPTION 'Attendance list snapshots are immutable and version must advance' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER protect_attendance_list_member BEFORE UPDATE OR DELETE ON nite_connect.attendance_list_members
    FOR EACH ROW EXECUTE FUNCTION nite_connect.protect_attendance_list_member();

CREATE FUNCTION nite_connect.require_attendance_list_member_history() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM nite_connect.attendance_list_events e
        WHERE e.list_id=NEW.list_id AND e.member_id=NEW.member_id AND e.member_version=NEW.version
    ) THEN
        RAISE EXCEPTION 'Attendance list member updates require immutable history' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE CONSTRAINT TRIGGER attendance_list_member_history_required AFTER UPDATE ON nite_connect.attendance_list_members
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION nite_connect.require_attendance_list_member_history();

CREATE FUNCTION nite_connect.protect_frequency_settings() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Frequency activation timestamp is immutable';
END;
$$;
CREATE TRIGGER immutable_frequency_settings BEFORE UPDATE OR DELETE ON nite_connect.frequency_settings
    FOR EACH ROW EXECUTE FUNCTION nite_connect.protect_frequency_settings();

-- Revoke QR values from the retired event-scoped session contract without deleting
-- calendar events, attendance records, segments, or their existing audit trail.
WITH revoked AS (
    UPDATE nite_connect.calendar_events
       SET qr_hash=NULL,qr_expires_at=NULL,version=version+1
     WHERE qr_hash IS NOT NULL OR qr_expires_at IS NOT NULL
     RETURNING id,created_by,version,status
)
INSERT INTO nite_connect.attendance_events(
    id,entity_type,entity_id,actor_id,action,entity_version,before_value,after_value,reason
)
SELECT gen_random_uuid(),'CALENDAR_EVENT',id,created_by,'LEGACY_QR_REVOKED',version,
       '{"qr":"revoked"}',json_build_object('status',status,'qrActive',false)::text,
       'Revogação do QR legado na ativação da Frequência'
FROM revoked;
