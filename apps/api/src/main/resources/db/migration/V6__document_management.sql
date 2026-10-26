CREATE TABLE nite_connect.document_templates (
 id uuid PRIMARY KEY, title varchar(200) NOT NULL, official boolean NOT NULL,
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0), retired_at timestamptz,
 created_by uuid NOT NULL REFERENCES nite_connect.members(id), created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE nite_connect.document_template_versions (
 id uuid PRIMARY KEY, template_id uuid NOT NULL REFERENCES nite_connect.document_templates(id), revision integer NOT NULL CHECK(revision>0),
 object_id uuid NOT NULL UNIQUE, size bigint NOT NULL CHECK(size>0 AND size<=8388608), sha256 char(64) NOT NULL,
 created_by uuid NOT NULL REFERENCES nite_connect.members(id), created_at timestamptz NOT NULL DEFAULT clock_timestamp(), UNIQUE(template_id,revision)
);
CREATE TABLE nite_connect.documents (
 id uuid PRIMARY KEY, title varchar(200) NOT NULL, context varchar(20) NOT NULL CHECK(context IN ('PERSONAL','PROJECT','EVENT','ADMINISTRATIVE')),
 owner_id uuid REFERENCES nite_connect.members(id), project_id uuid REFERENCES nite_connect.projects(id),
 event_id uuid REFERENCES nite_connect.calendar_events(id), demand_reference varchar(200),
 template_version_id uuid REFERENCES nite_connect.document_template_versions(id), official boolean NOT NULL,
 current_version_id uuid, version bigint NOT NULL DEFAULT 0 CHECK(version>=0), archived_at timestamptz,
 created_by uuid NOT NULL REFERENCES nite_connect.members(id), created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 CHECK ((context='PERSONAL' AND owner_id IS NOT NULL AND project_id IS NULL AND event_id IS NULL)
 OR (context='PROJECT' AND project_id IS NOT NULL AND owner_id IS NULL AND event_id IS NULL)
 OR (context='EVENT' AND event_id IS NOT NULL AND owner_id IS NULL AND project_id IS NULL)
 OR (context='ADMINISTRATIVE' AND owner_id IS NULL AND project_id IS NULL AND event_id IS NULL))
);
CREATE TABLE nite_connect.document_versions (
 id uuid PRIMARY KEY, document_id uuid NOT NULL REFERENCES nite_connect.documents(id), revision integer NOT NULL CHECK(revision>0),
 object_id uuid NOT NULL, size bigint NOT NULL CHECK(size>0 AND size<=8388608), sha256 char(64) NOT NULL,
 status varchar(20) NOT NULL CHECK(status IN ('AVAILABLE','QUARANTINED')), based_on uuid REFERENCES nite_connect.document_versions(id),
 rejection_code varchar(200), created_by uuid NOT NULL REFERENCES nite_connect.members(id),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), UNIQUE(document_id,revision), UNIQUE(document_id,id)
);
ALTER TABLE nite_connect.documents ADD CONSTRAINT document_current_version_fk
 FOREIGN KEY(id,current_version_id) REFERENCES nite_connect.document_versions(document_id,id);
CREATE TABLE nite_connect.document_grants (
 id uuid PRIMARY KEY, document_id uuid NOT NULL REFERENCES nite_connect.documents(id), member_id uuid NOT NULL REFERENCES nite_connect.members(id),
 can_edit boolean NOT NULL, granted_by uuid NOT NULL REFERENCES nite_connect.members(id), created_at timestamptz NOT NULL DEFAULT clock_timestamp(), revoked_at timestamptz
);
CREATE UNIQUE INDEX document_current_grant_idx ON nite_connect.document_grants(document_id,member_id) WHERE revoked_at IS NULL;
CREATE TABLE nite_connect.document_cycles (
 id uuid PRIMARY KEY, document_id uuid NOT NULL REFERENCES nite_connect.documents(id), source_version_id uuid NOT NULL REFERENCES nite_connect.document_versions(id),
 head_version_id uuid NOT NULL REFERENCES nite_connect.document_versions(id), editor_key varchar(128) NOT NULL UNIQUE,
 actor_id uuid NOT NULL REFERENCES nite_connect.members(id), read_only boolean NOT NULL,
 status varchar(12) NOT NULL CHECK(status IN ('OPEN','CLOSED','REVOKED','FAILED')), expires_at timestamptz NOT NULL, created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE UNIQUE INDEX document_open_cycle_idx ON nite_connect.document_cycles(document_id,read_only) WHERE status='OPEN';
CREATE TABLE nite_connect.document_source_grants (
 id uuid PRIMARY KEY, document_id uuid NOT NULL REFERENCES nite_connect.documents(id), version_id uuid NOT NULL REFERENCES nite_connect.document_versions(id),
 actor_id uuid NOT NULL REFERENCES nite_connect.members(id), cycle_id uuid REFERENCES nite_connect.document_cycles(id),
 expires_at timestamptz NOT NULL, created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE nite_connect.document_callback_receipts (
 id uuid PRIMARY KEY, cycle_id uuid NOT NULL REFERENCES nite_connect.document_cycles(id), status integer NOT NULL,
 sha256 char(64) NOT NULL, request_sha256 char(64) NOT NULL, version_id uuid REFERENCES nite_connect.document_versions(id),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), UNIQUE(cycle_id,request_sha256)
);
CREATE TABLE nite_connect.document_pdfs (
 id uuid PRIMARY KEY, version_id uuid NOT NULL UNIQUE REFERENCES nite_connect.document_versions(id), object_id uuid NOT NULL UNIQUE,
 size bigint NOT NULL CHECK(size>0 AND size<=8388608), sha256 char(64) NOT NULL,
 created_by uuid NOT NULL REFERENCES nite_connect.members(id), created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE nite_connect.document_events (
 id uuid PRIMARY KEY, document_id uuid NOT NULL REFERENCES nite_connect.documents(id), actor_id uuid NOT NULL REFERENCES nite_connect.members(id),
 action varchar(60) NOT NULL, reference_id uuid, reason varchar(1000) NOT NULL, occurred_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX document_events_page_idx ON nite_connect.document_events(document_id,occurred_at,id);
CREATE INDEX document_versions_page_idx ON nite_connect.document_versions(document_id,revision,id);
CREATE INDEX documents_owner_idx ON nite_connect.documents(owner_id,id);
CREATE INDEX documents_project_idx ON nite_connect.documents(project_id,id);
CREATE FUNCTION nite_connect.document_immutable_row() RETURNS trigger LANGUAGE plpgsql AS $$
 BEGIN RAISE EXCEPTION 'Immutable document record'; END;
$$;
CREATE TRIGGER document_template_versions_immutable BEFORE UPDATE OR DELETE ON nite_connect.document_template_versions FOR EACH ROW EXECUTE FUNCTION nite_connect.document_immutable_row();
CREATE TRIGGER document_versions_immutable BEFORE UPDATE OR DELETE ON nite_connect.document_versions FOR EACH ROW EXECUTE FUNCTION nite_connect.document_immutable_row();
CREATE TRIGGER document_pdfs_immutable BEFORE UPDATE OR DELETE ON nite_connect.document_pdfs FOR EACH ROW EXECUTE FUNCTION nite_connect.document_immutable_row();
CREATE TRIGGER document_events_immutable BEFORE UPDATE OR DELETE ON nite_connect.document_events FOR EACH ROW EXECUTE FUNCTION nite_connect.document_immutable_row();

-- Extend the audit contract in this migration; historical migrations remain immutable.
ALTER TABLE nite_connect.audit_events DROP CONSTRAINT audit_entity_check;
ALTER TABLE nite_connect.audit_events ADD CONSTRAINT audit_entity_check CHECK (
 (entity_type='MEMBER' AND member_id IS NOT NULL AND (entity_id IS NULL OR entity_id=member_id)) OR
 (entity_type IN ('PROJECT','PROPOSAL','ACTIVITY','EVIDENCE','CALENDAR_EVENT','ATTENDANCE','PERIOD','JUSTIFICATION','CORRECTION','DOCUMENT','DOCUMENT_TEMPLATE') AND entity_id IS NOT NULL AND member_id IS NULL)
);
