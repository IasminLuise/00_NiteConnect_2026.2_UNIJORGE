CREATE TABLE nite_connect.diaries (
    member_id uuid PRIMARY KEY REFERENCES nite_connect.members(id),
    document_id uuid NOT NULL UNIQUE REFERENCES nite_connect.documents(id),
    imported_by uuid NOT NULL REFERENCES nite_connect.members(id),
    imported_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    import_reason varchar(1000) NOT NULL,
    source_created_at timestamptz,
    source_modified_at timestamptz,
    source_metadata_origin varchar(40),
    CHECK ((source_created_at IS NULL AND source_modified_at IS NULL AND source_metadata_origin IS NULL)
        OR ((source_created_at IS NOT NULL OR source_modified_at IS NOT NULL) AND source_metadata_origin = 'SHAREPOINT'))
);

CREATE FUNCTION nite_connect.diary_provenance_immutable_row() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Diary import provenance is immutable';
END;
$$;

CREATE TRIGGER diary_provenance_immutable
    BEFORE UPDATE OR DELETE ON nite_connect.diaries
    FOR EACH ROW EXECUTE FUNCTION nite_connect.diary_provenance_immutable_row();

CREATE INDEX diaries_imported_by_idx ON nite_connect.diaries(imported_by, imported_at, member_id);
