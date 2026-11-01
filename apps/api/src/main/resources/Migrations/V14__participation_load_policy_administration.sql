CREATE TABLE nite_connect.participation_load_policy_drafts (
 id uuid PRIMARY KEY,
 version integer NOT NULL UNIQUE CHECK(version > 0),
 presential_hours numeric(7,2) NOT NULL CHECK(presential_hours >= 0),
 remote_hours numeric(7,2) NOT NULL CHECK(remote_hours >= 0),
 effective_from date NOT NULL,
 created_by uuid NOT NULL REFERENCES nite_connect.members(id),
 created_at timestamptz NOT NULL,
 creation_reason varchar(1000) NOT NULL CHECK(length(trim(creation_reason)) > 0),
 supersedes_id uuid REFERENCES nite_connect.participation_load_policy_drafts(id),
 CHECK(supersedes_id IS NULL OR supersedes_id <> id)
);

CREATE TABLE nite_connect.participation_load_policy_events (
 id uuid PRIMARY KEY,
 draft_id uuid NOT NULL REFERENCES nite_connect.participation_load_policy_drafts(id),
 sequence bigint GENERATED ALWAYS AS IDENTITY,
 event_type varchar(16) NOT NULL CHECK(event_type IN ('CRIADA','APROVADA','ATIVADA','SUBSTITUIDA')),
 actor_id uuid NOT NULL REFERENCES nite_connect.members(id),
 occurred_at timestamptz NOT NULL,
 reason varchar(1000) NOT NULL CHECK(length(trim(reason)) > 0),
 source_reference varchar(1000),
 UNIQUE(draft_id,sequence),
 CHECK((event_type='APROVADA' AND source_reference IS NOT NULL AND length(trim(source_reference)) > 0)
    OR (event_type<>'APROVADA' AND source_reference IS NULL))
);

CREATE UNIQUE INDEX participation_load_policy_event_singleton_idx
 ON nite_connect.participation_load_policy_events(draft_id,event_type);
CREATE INDEX participation_load_policy_draft_history_idx
 ON nite_connect.participation_load_policy_drafts(version DESC,id);
CREATE INDEX participation_load_policy_events_history_idx
 ON nite_connect.participation_load_policy_events(draft_id,sequence);

CREATE FUNCTION nite_connect.protect_participation_load_policy_event() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION 'Participation load policy history is append-only';
END;
$$;
CREATE TRIGGER protect_participation_load_policy_events
 BEFORE UPDATE OR DELETE ON nite_connect.participation_load_policy_events
 FOR EACH ROW EXECUTE FUNCTION nite_connect.protect_participation_load_policy_event();

CREATE FUNCTION nite_connect.protect_participation_load_policy_draft() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 RAISE EXCEPTION 'Participation load policy drafts are immutable; create a new version';
END;
$$;
CREATE TRIGGER protect_participation_load_policy_drafts
 BEFORE UPDATE OR DELETE ON nite_connect.participation_load_policy_drafts
 FOR EACH ROW EXECUTE FUNCTION nite_connect.protect_participation_load_policy_draft();
