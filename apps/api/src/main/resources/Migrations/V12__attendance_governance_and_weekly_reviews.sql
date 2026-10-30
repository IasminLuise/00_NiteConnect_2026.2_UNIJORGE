CREATE TABLE nite_connect.participation_calendar_versions (
 id uuid PRIMARY KEY,
 week_start date NOT NULL,
 week_end date NOT NULL,
 version integer NOT NULL CHECK(version > 0),
 exception_type varchar(32) NOT NULL CHECK(exception_type IN (
   'CANCELAMENTO_PRESENCIAL','RECESSO_INTEGRAL','SUSPENSAO_PRESENCIAL','SUSPENSAO_PARCIAL','REMARCACAO')),
 presential_hours_relieved numeric(5,2) NOT NULL CHECK(presential_hours_relieved BETWEEN 0 AND 5),
 remote_hours_relieved numeric(5,2) NOT NULL CHECK(remote_hours_relieved BETWEEN 0 AND 4),
 affected_event_id uuid REFERENCES nite_connect.calendar_events(id),
 replacement_event_id uuid REFERENCES nite_connect.calendar_events(id),
 valid_from timestamptz NOT NULL,
 valid_until timestamptz,
 created_by uuid NOT NULL REFERENCES nite_connect.members(id),
 reason varchar(1000) NOT NULL CHECK(length(trim(reason)) > 0),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 UNIQUE(week_start,week_end,version),
 CHECK(week_end=week_start+6),
 CHECK(valid_until IS NULL OR valid_until > valid_from),
 CHECK((exception_type='CANCELAMENTO_PRESENCIAL' AND presential_hours_relieved=5 AND remote_hours_relieved=0
          AND affected_event_id IS NOT NULL AND replacement_event_id IS NULL)
    OR (exception_type='RECESSO_INTEGRAL' AND presential_hours_relieved=5 AND remote_hours_relieved=4
          AND affected_event_id IS NULL AND replacement_event_id IS NULL)
    OR (exception_type='SUSPENSAO_PRESENCIAL' AND presential_hours_relieved=5 AND remote_hours_relieved=0
          AND affected_event_id IS NULL AND replacement_event_id IS NULL)
    OR (exception_type='SUSPENSAO_PARCIAL' AND (presential_hours_relieved>0 OR remote_hours_relieved>0)
          AND affected_event_id IS NULL AND replacement_event_id IS NULL)
    OR (exception_type='REMARCACAO' AND presential_hours_relieved=0 AND remote_hours_relieved=0
          AND affected_event_id IS NOT NULL AND replacement_event_id IS NOT NULL
          AND affected_event_id<>replacement_event_id))
);
CREATE UNIQUE INDEX participation_calendar_current_week_idx
 ON nite_connect.participation_calendar_versions(week_start,week_end) WHERE valid_until IS NULL;
CREATE INDEX participation_calendar_dates_idx
 ON nite_connect.participation_calendar_versions(week_start,week_end,version DESC);

CREATE FUNCTION nite_connect.protect_participation_calendar_version() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Participation calendar versions cannot be deleted'; END IF;
 IF NEW.id<>OLD.id OR NEW.week_start<>OLD.week_start OR NEW.week_end<>OLD.week_end OR NEW.version<>OLD.version
    OR NEW.exception_type<>OLD.exception_type OR NEW.presential_hours_relieved<>OLD.presential_hours_relieved
    OR NEW.remote_hours_relieved<>OLD.remote_hours_relieved
    OR NEW.affected_event_id IS DISTINCT FROM OLD.affected_event_id
    OR NEW.replacement_event_id IS DISTINCT FROM OLD.replacement_event_id
    OR NEW.valid_from<>OLD.valid_from OR NEW.created_by<>OLD.created_by OR NEW.reason<>OLD.reason
    OR NEW.created_at<>OLD.created_at OR OLD.valid_until IS NOT NULL OR NEW.valid_until IS NULL
    OR NEW.valid_until<=OLD.valid_from THEN
  RAISE EXCEPTION 'Only one closing instant may be added to a participation calendar version';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER protect_participation_calendar_versions
 BEFORE UPDATE OR DELETE ON nite_connect.participation_calendar_versions
 FOR EACH ROW EXECUTE FUNCTION nite_connect.protect_participation_calendar_version();

CREATE TABLE nite_connect.participation_adjustments (
 id uuid PRIMARY KEY,
 member_id uuid NOT NULL REFERENCES nite_connect.members(id),
 source_week_start date NOT NULL,
 source_week_end date NOT NULL,
 kind varchar(16) NOT NULL CHECK(kind IN ('DISPENSA','REPOSICAO')),
 requested_modality varchar(16) NOT NULL CHECK(requested_modality IN ('PRESENCIAL','REMOTA')),
 requested_hours numeric(5,2) NOT NULL CHECK(requested_hours>0 AND requested_hours<=CASE requested_modality WHEN 'PRESENCIAL' THEN 5 ELSE 4 END),
 authorized_modality varchar(16) CHECK(authorized_modality IN ('PRESENCIAL','REMOTA')),
 authorized_hours numeric(5,2) CHECK(authorized_hours>0 AND authorized_hours<=CASE authorized_modality WHEN 'PRESENCIAL' THEN 5 WHEN 'REMOTA' THEN 4 ELSE 0 END),
 due_on date,
 status varchar(16) NOT NULL CHECK(status IN ('PENDENTE','AUTORIZADA','RECUSADA')),
 requested_by uuid NOT NULL REFERENCES nite_connect.members(id),
 requested_at timestamptz NOT NULL,
 decided_by uuid REFERENCES nite_connect.members(id),
 decided_at timestamptz,
 decision_reason varchar(1000),
 reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
 CHECK(source_week_end=source_week_start+6),
 CHECK((kind='DISPENSA' AND status='AUTORIZADA' AND requested_by=decided_by
          AND authorized_modality=requested_modality AND authorized_hours=requested_hours AND due_on IS NULL
          AND decided_at IS NOT NULL AND length(trim(decision_reason))>0)
    OR (kind='REPOSICAO' AND status='PENDENTE' AND authorized_modality IS NULL AND authorized_hours IS NULL
          AND due_on IS NULL AND decided_by IS NULL AND decided_at IS NULL AND decision_reason IS NULL)
    OR (kind='REPOSICAO' AND status='AUTORIZADA' AND authorized_modality IS NOT NULL AND authorized_hours IS NOT NULL
          AND authorized_modality=requested_modality AND authorized_hours<=requested_hours AND due_on>source_week_end AND decided_by IS NOT NULL
          AND decided_at IS NOT NULL AND length(trim(decision_reason))>0)
    OR (kind='REPOSICAO' AND status='RECUSADA' AND authorized_modality IS NULL AND authorized_hours IS NULL
          AND due_on IS NULL AND decided_by IS NOT NULL AND decided_at IS NOT NULL AND length(trim(decision_reason))>0))
);
CREATE INDEX participation_adjustments_member_week_idx
 ON nite_connect.participation_adjustments(member_id,source_week_start,kind,status);
CREATE UNIQUE INDEX participation_one_open_replacement_idx
 ON nite_connect.participation_adjustments(member_id,source_week_start,requested_modality)
 WHERE kind='REPOSICAO' AND status='PENDENTE';

CREATE TABLE nite_connect.participation_adjustment_events (
 id uuid PRIMARY KEY,
 adjustment_id uuid NOT NULL REFERENCES nite_connect.participation_adjustments(id),
 actor_id uuid NOT NULL REFERENCES nite_connect.members(id),
 action varchar(32) NOT NULL,
 before_value text,
 after_value text NOT NULL,
 reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 occurred_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX participation_adjustment_events_idx
 ON nite_connect.participation_adjustment_events(adjustment_id,occurred_at,id);
CREATE TRIGGER immutable_participation_adjustment_events
 BEFORE UPDATE OR DELETE ON nite_connect.participation_adjustment_events
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();
CREATE FUNCTION nite_connect.protect_participation_adjustment() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Participation adjustments cannot be deleted'; END IF;
 IF NEW.id<>OLD.id OR NEW.member_id<>OLD.member_id OR NEW.source_week_start<>OLD.source_week_start
    OR NEW.source_week_end<>OLD.source_week_end OR NEW.kind<>OLD.kind
    OR NEW.requested_modality<>OLD.requested_modality OR NEW.requested_hours<>OLD.requested_hours
    OR NEW.requested_by<>OLD.requested_by OR NEW.requested_at<>OLD.requested_at OR NEW.reason<>OLD.reason
    OR NEW.version<>OLD.version+1 THEN
  RAISE EXCEPTION 'Participation adjustment identity is immutable and version must advance';
 END IF;
 IF OLD.status<>'PENDENTE' OR NEW.status='PENDENTE' THEN
  RAISE EXCEPTION 'A participation adjustment may receive only one terminal decision';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER protect_participation_adjustments
 BEFORE UPDATE OR DELETE ON nite_connect.participation_adjustments
 FOR EACH ROW EXECUTE FUNCTION nite_connect.protect_participation_adjustment();
CREATE FUNCTION nite_connect.require_participation_adjustment_event() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM nite_connect.participation_adjustment_events e
    WHERE e.adjustment_id=NEW.id AND (e.after_value::jsonb->>'version')::bigint=NEW.version ) THEN
  RAISE EXCEPTION 'Participation adjustment updates require immutable history';
 END IF;
 RETURN NEW;
END;
$$;
CREATE CONSTRAINT TRIGGER participation_adjustment_history_required
 AFTER UPDATE ON nite_connect.participation_adjustments
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION nite_connect.require_participation_adjustment_event();

CREATE TABLE nite_connect.participation_replacement_allocations (
 id uuid PRIMARY KEY,
 adjustment_id uuid NOT NULL REFERENCES nite_connect.participation_adjustments(id),
 period_id uuid NOT NULL UNIQUE REFERENCES nite_connect.participation_periods(id),
 assigned_by uuid NOT NULL REFERENCES nite_connect.members(id),
 assigned_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TRIGGER immutable_participation_replacement_allocations
 BEFORE UPDATE OR DELETE ON nite_connect.participation_replacement_allocations
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();

CREATE TABLE nite_connect.participation_review_batches (
 id uuid PRIMARY KEY,
 week_start date NOT NULL,
 week_end date NOT NULL,
 reviewed_by uuid NOT NULL REFERENCES nite_connect.members(id),
 reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 CHECK(week_end=week_start+6)
);
CREATE INDEX participation_review_batches_dates_idx
 ON nite_connect.participation_review_batches(week_start,week_end,created_at DESC,id);
CREATE TRIGGER immutable_participation_review_batches
 BEFORE UPDATE OR DELETE ON nite_connect.participation_review_batches
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();

CREATE TABLE nite_connect.participation_review_batch_items (
 id uuid PRIMARY KEY,
 batch_id uuid NOT NULL REFERENCES nite_connect.participation_review_batches(id),
 period_id uuid NOT NULL REFERENCES nite_connect.participation_periods(id),
 action varchar(24) NOT NULL CHECK(action IN ('APROVAR','REJEITAR','CORRECAO_SOLICITADA')),
 reviewer_reason varchar(1000) NOT NULL CHECK(length(trim(reviewer_reason))>0),
 previous_version bigint NOT NULL CHECK(previous_version>=0),
 resulting_version bigint NOT NULL CHECK(resulting_version>=previous_version),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 UNIQUE(batch_id,period_id),
 CHECK((action='CORRECAO_SOLICITADA' AND resulting_version=previous_version)
    OR (action IN ('APROVAR','REJEITAR') AND resulting_version=previous_version+1))
);
CREATE INDEX participation_review_batch_items_period_idx
 ON nite_connect.participation_review_batch_items(period_id,created_at DESC,id);
CREATE TRIGGER immutable_participation_review_batch_items
 BEFORE UPDATE OR DELETE ON nite_connect.participation_review_batch_items
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();
