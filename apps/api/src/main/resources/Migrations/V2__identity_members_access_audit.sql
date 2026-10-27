CREATE TABLE nite_connect.members (
    id uuid PRIMARY KEY,
    name varchar(160) NOT NULL CHECK (length(trim(name)) > 0),
    institutional_email varchar(254) NOT NULL
        CHECK (institutional_email = lower(trim(institutional_email)))
        CHECK (institutional_email ~ '^[^@[:space:]]+@unijorge\.com\.br$'),
    status varchar(24) NOT NULL CHECK (status IN ('PENDENTE_INGRESSO', 'ATIVO', 'AFASTADO', 'DESLIGADO')),
    version bigint NOT NULL DEFAULT 0 CHECK (version >= 0),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (institutional_email)
);
CREATE INDEX members_status_name_idx ON nite_connect.members (status, name, id);

CREATE TABLE nite_connect.identity_accounts (
    tenant_id uuid NOT NULL,
    object_id uuid NOT NULL,
    member_id uuid NOT NULL UNIQUE REFERENCES nite_connect.members(id),
    linked_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, object_id)
);

CREATE TABLE nite_connect.invitations (
    id uuid PRIMARY KEY,
    member_id uuid NOT NULL REFERENCES nite_connect.members(id),
    tenant_id uuid NOT NULL,
    object_id uuid NOT NULL,
    created_by uuid NOT NULL REFERENCES nite_connect.members(id),
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamptz NOT NULL CHECK (expires_at > created_at),
    accepted_at timestamptz,
    revoked_at timestamptz,
    CHECK (accepted_at IS NULL OR revoked_at IS NULL)
);
CREATE UNIQUE INDEX invitations_pending_member_idx ON nite_connect.invitations(member_id)
    WHERE accepted_at IS NULL AND revoked_at IS NULL;
CREATE UNIQUE INDEX invitations_pending_identity_idx ON nite_connect.invitations(tenant_id, object_id)
    WHERE accepted_at IS NULL AND revoked_at IS NULL;
CREATE INDEX invitations_created_idx ON nite_connect.invitations(created_at, id);

CREATE TABLE nite_connect.role_assignments (
    member_id uuid NOT NULL REFERENCES nite_connect.members(id),
    role varchar(24) NOT NULL CHECK (role IN ('COORDENADOR', 'GESTOR_GERAL')),
    granted_by uuid NOT NULL REFERENCES nite_connect.members(id),
    granted_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reason varchar(1000) NOT NULL CHECK (length(trim(reason)) > 0),
    PRIMARY KEY (member_id, role)
);

CREATE TABLE nite_connect.member_status_history (
    id uuid PRIMARY KEY,
    member_id uuid NOT NULL REFERENCES nite_connect.members(id),
    actor_id uuid NOT NULL REFERENCES nite_connect.members(id),
    previous_status varchar(24) CHECK (previous_status IN ('PENDENTE_INGRESSO', 'ATIVO', 'AFASTADO', 'DESLIGADO')),
    new_status varchar(24) NOT NULL CHECK (new_status IN ('PENDENTE_INGRESSO', 'ATIVO', 'AFASTADO', 'DESLIGADO')),
    reason varchar(1000) NOT NULL CHECK (length(trim(reason)) > 0),
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX member_status_history_member_idx ON nite_connect.member_status_history(member_id, occurred_at, id);

CREATE TABLE nite_connect.audit_events (
    id uuid PRIMARY KEY,
    actor_id uuid NOT NULL REFERENCES nite_connect.members(id),
    member_id uuid NOT NULL REFERENCES nite_connect.members(id),
    action varchar(64) NOT NULL,
    before_value text,
    after_value text,
    reason varchar(1000) NOT NULL CHECK (length(trim(reason)) > 0),
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX audit_events_member_idx ON nite_connect.audit_events(member_id, occurred_at, id);

-- Históricos não são reescritos nem excluídos por operações ordinárias.
CREATE FUNCTION nite_connect.reject_history_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Historical records are immutable';
END;
$$;
CREATE TRIGGER immutable_audit_events BEFORE UPDATE OR DELETE ON nite_connect.audit_events
    FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();
CREATE TRIGGER immutable_member_status_history BEFORE UPDATE OR DELETE ON nite_connect.member_status_history
    FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();
