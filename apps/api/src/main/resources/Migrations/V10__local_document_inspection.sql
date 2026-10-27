-- Opt-in local inspection does not promote versions or change the normal editor cycle.
CREATE TABLE nite_connect.document_inspection_sessions (
 id uuid PRIMARY KEY,
 document_id uuid NOT NULL REFERENCES nite_connect.documents(id),
 version_id uuid NOT NULL,
 actor_id uuid NOT NULL REFERENCES nite_connect.members(id),
 editor_key varchar(128) NOT NULL UNIQUE,
 status varchar(12) NOT NULL CHECK (status IN ('OPEN','CLOSED','REVOKED','FAILED')),
 expires_at timestamptz NOT NULL,
 last_provider_status integer,
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 FOREIGN KEY (document_id,version_id) REFERENCES nite_connect.document_versions(document_id,id)
);
CREATE INDEX document_inspection_actor_idx ON nite_connect.document_inspection_sessions(actor_id,expires_at);
