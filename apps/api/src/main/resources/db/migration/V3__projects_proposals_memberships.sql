CREATE TABLE nite_connect.project_proposals (
    id uuid PRIMARY KEY,
    title varchar(200) NOT NULL CHECK (length(trim(title)) > 0),
    description varchar(10000) NOT NULL CHECK (length(trim(description)) > 0),
    author_id uuid NOT NULL REFERENCES nite_connect.members(id),
    status varchar(24) NOT NULL DEFAULT 'PENDENTE'
        CHECK (status IN ('PENDENTE','APROVADA','REJEITADA','CONVERTIDA')),
    decided_by uuid REFERENCES nite_connect.members(id),
    decided_at timestamptz,
    decision_reason varchar(1000),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK ((status='PENDENTE' AND decided_by IS NULL AND decided_at IS NULL AND decision_reason IS NULL)
        OR (status<>'PENDENTE' AND decided_by IS NOT NULL AND decided_at IS NOT NULL
            AND decision_reason IS NOT NULL AND length(trim(decision_reason)) > 0))
);
CREATE INDEX proposals_author_idx ON nite_connect.project_proposals(author_id, created_at, id);
CREATE INDEX proposals_status_idx ON nite_connect.project_proposals(status, created_at, id);

CREATE TABLE nite_connect.projects (
    id uuid PRIMARY KEY,
    title varchar(200) NOT NULL CHECK (length(trim(title)) > 0),
    description varchar(10000) NOT NULL CHECK (length(trim(description)) > 0),
    status varchar(24) NOT NULL DEFAULT 'PLANEJAMENTO'
        CHECK (status IN ('PLANEJAMENTO','ATIVO','PAUSADO','CONCLUIDO')),
    starts_on date,
    ends_on date,
    proposal_id uuid UNIQUE REFERENCES nite_connect.project_proposals(id),
    created_by uuid NOT NULL REFERENCES nite_connect.members(id),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (starts_on IS NULL OR ends_on IS NULL OR ends_on >= starts_on)
);
CREATE INDEX projects_status_idx ON nite_connect.projects(status, title, id);

CREATE TABLE nite_connect.project_memberships (
    id uuid PRIMARY KEY,
    project_id uuid NOT NULL REFERENCES nite_connect.projects(id),
    member_id uuid NOT NULL REFERENCES nite_connect.members(id),
    responsibility varchar(200) NOT NULL CHECK (length(trim(responsibility)) > 0),
    started_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_by uuid NOT NULL REFERENCES nite_connect.members(id),
    start_reason varchar(1000) NOT NULL CHECK (length(trim(start_reason)) > 0),
    ended_at timestamptz,
    ended_by uuid REFERENCES nite_connect.members(id),
    end_reason varchar(1000),
    UNIQUE (id, project_id, member_id),
    CHECK ((ended_at IS NULL AND ended_by IS NULL AND end_reason IS NULL)
        OR (ended_at IS NOT NULL AND ended_at >= started_at AND ended_by IS NOT NULL
            AND end_reason IS NOT NULL AND length(trim(end_reason)) > 0))
);
CREATE UNIQUE INDEX memberships_current_idx ON nite_connect.project_memberships(project_id, member_id) WHERE ended_at IS NULL;
CREATE INDEX memberships_member_idx ON nite_connect.project_memberships(member_id, started_at, id);
CREATE INDEX memberships_project_idx ON nite_connect.project_memberships(project_id, started_at, id);

CREATE TABLE nite_connect.project_leadership_history (
    id uuid PRIMARY KEY,
    project_id uuid NOT NULL REFERENCES nite_connect.projects(id),
    member_id uuid NOT NULL REFERENCES nite_connect.members(id),
    membership_id uuid NOT NULL,
    started_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_by uuid NOT NULL REFERENCES nite_connect.members(id),
    start_reason varchar(1000) NOT NULL CHECK (length(trim(start_reason)) > 0),
    ended_at timestamptz,
    ended_by uuid REFERENCES nite_connect.members(id),
    end_reason varchar(1000),
    FOREIGN KEY (membership_id, project_id, member_id)
        REFERENCES nite_connect.project_memberships(id, project_id, member_id),
    CHECK ((ended_at IS NULL AND ended_by IS NULL AND end_reason IS NULL)
        OR (ended_at IS NOT NULL AND ended_at >= started_at AND ended_by IS NOT NULL
            AND end_reason IS NOT NULL AND length(trim(end_reason)) > 0))
);
CREATE UNIQUE INDEX leadership_current_idx ON nite_connect.project_leadership_history(project_id) WHERE ended_at IS NULL;
CREATE INDEX leadership_project_idx ON nite_connect.project_leadership_history(project_id, started_at, id);

CREATE TABLE nite_connect.project_status_history (
    id uuid PRIMARY KEY,
    project_id uuid NOT NULL REFERENCES nite_connect.projects(id),
    actor_id uuid NOT NULL REFERENCES nite_connect.members(id),
    previous_status varchar(24) CHECK (previous_status IN ('PLANEJAMENTO','ATIVO','PAUSADO','CONCLUIDO')),
    new_status varchar(24) NOT NULL CHECK (new_status IN ('PLANEJAMENTO','ATIVO','PAUSADO','CONCLUIDO')),
    reason varchar(1000) NOT NULL CHECK (length(trim(reason)) > 0),
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX project_status_history_idx ON nite_connect.project_status_history(project_id, occurred_at, id);
CREATE TRIGGER immutable_project_status_history BEFORE UPDATE OR DELETE ON nite_connect.project_status_history
    FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();
CREATE TRIGGER preserve_projects BEFORE DELETE ON nite_connect.projects
    FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();
CREATE TRIGGER preserve_proposals BEFORE DELETE ON nite_connect.project_proposals
    FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();

-- Somente encerramento único de período; dados de ingresso e períodos encerrados são imutáveis.
CREATE FUNCTION nite_connect.close_project_period_only() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN RAISE EXCEPTION 'Historical periods cannot be deleted'; END IF;
    IF OLD.ended_at IS NOT NULL OR NEW.ended_at IS NULL
        OR (to_jsonb(NEW) - ARRAY['ended_at','ended_by','end_reason'])
            IS DISTINCT FROM (to_jsonb(OLD) - ARRAY['ended_at','ended_by','end_reason']) THEN
        RAISE EXCEPTION 'Only closing an open period is allowed';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER preserve_membership_period BEFORE UPDATE OR DELETE ON nite_connect.project_memberships
    FOR EACH ROW EXECUTE FUNCTION nite_connect.close_project_period_only();
CREATE TRIGGER preserve_leadership_period BEFORE UPDATE OR DELETE ON nite_connect.project_leadership_history
    FOR EACH ROW EXECUTE FUNCTION nite_connect.close_project_period_only();

-- Sem reescrever eventos antigos: entity_id ausente equivale ao member_id original.
ALTER TABLE nite_connect.audit_events ALTER COLUMN member_id DROP NOT NULL;
ALTER TABLE nite_connect.audit_events ADD COLUMN entity_type varchar(24) NOT NULL DEFAULT 'MEMBER';
ALTER TABLE nite_connect.audit_events ADD COLUMN entity_id uuid;
ALTER TABLE nite_connect.audit_events ADD CONSTRAINT audit_entity_check CHECK (
    (entity_type='MEMBER' AND member_id IS NOT NULL AND (entity_id IS NULL OR entity_id=member_id))
    OR (entity_type IN ('PROJECT','PROPOSAL') AND entity_id IS NOT NULL AND member_id IS NULL)
);
CREATE INDEX audit_events_entity_idx ON nite_connect.audit_events(entity_type, entity_id, occurred_at, id);
