CREATE TABLE nite_connect.academic_terms (
 id uuid PRIMARY KEY, title varchar(100) NOT NULL CHECK(length(trim(title))>0),
 starts_at timestamptz NOT NULL, ends_at timestamptz NOT NULL, zone varchar(80) NOT NULL,
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0), CHECK(starts_at<ends_at)
);
CREATE TABLE nite_connect.academic_policies (
 id uuid PRIMARY KEY, term_id uuid NOT NULL REFERENCES nite_connect.academic_terms(id),
 revision integer NOT NULL CHECK(revision>0), status varchar(20) NOT NULL CHECK(status IN ('DRAFT','CONFIRMED')),
 rules jsonb NOT NULL CHECK(jsonb_typeof(rules)='object'), authority_reference varchar(1000),
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0), UNIQUE(term_id,revision), UNIQUE(id,term_id),
 CHECK(status<>'CONFIRMED' OR (authority_reference IS NOT NULL AND length(trim(authority_reference))>0))
);
CREATE TABLE nite_connect.academic_enrollments (
 id uuid PRIMARY KEY, term_id uuid NOT NULL REFERENCES nite_connect.academic_terms(id),
 member_id uuid NOT NULL REFERENCES nite_connect.members(id), policy_id uuid NOT NULL REFERENCES nite_connect.academic_policies(id),
 status varchar(32) NOT NULL CHECK(status IN ('SOLICITADA','EM_ANALISE','CORRECAO_SOLICITADA','DEFERIDA','INDEFERIDA','EM_ACOMPANHAMENTO','CALCULADA','EM_REVISAO','HOMOLOGADA')),
 disciplines jsonb NOT NULL CHECK(jsonb_typeof(disciplines)='array'), version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), UNIQUE(member_id,term_id), UNIQUE(id,policy_id),
 FOREIGN KEY(policy_id,term_id) REFERENCES nite_connect.academic_policies(id,term_id)
);
CREATE INDEX academic_enrollment_term_idx ON nite_connect.academic_enrollments(term_id,member_id,id);
CREATE TABLE nite_connect.academic_plans (
 id uuid PRIMARY KEY, enrollment_id uuid NOT NULL REFERENCES nite_connect.academic_enrollments(id),
 revision integer NOT NULL CHECK(revision>0), status varchar(32) NOT NULL CHECK(status IN ('RASCUNHO','SUBMETIDO','EM_ANALISE','CORRECAO_SOLICITADA','APROVADO')),
 content varchar(20000) NOT NULL CHECK(length(trim(content))>0), version bigint NOT NULL DEFAULT 0 CHECK(version>=0),
 UNIQUE(enrollment_id,revision)
);
CREATE TABLE nite_connect.academic_evidence (
 id uuid PRIMARY KEY, enrollment_id uuid NOT NULL REFERENCES nite_connect.academic_enrollments(id),
 snapshot jsonb NOT NULL CHECK(jsonb_typeof(snapshot)='object'),
 captured_by uuid NOT NULL REFERENCES nite_connect.members(id), captured_at timestamptz NOT NULL DEFAULT clock_timestamp(), UNIQUE(id,enrollment_id)
);
CREATE INDEX academic_evidence_enrollment_idx ON nite_connect.academic_evidence(enrollment_id,captured_at,id);
CREATE TABLE nite_connect.academic_assessments (
 id uuid PRIMARY KEY, enrollment_id uuid NOT NULL REFERENCES nite_connect.academic_enrollments(id),
 pillar varchar(24) NOT NULL CHECK(pillar IN ('DOCUMENTATION','PRESENTATIONS','DELIVERIES')),
 score numeric NOT NULL,
 reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0), revision integer NOT NULL CHECK(revision>0),
 assessed_by uuid NOT NULL REFERENCES nite_connect.members(id), created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 UNIQUE(enrollment_id,pillar,revision), UNIQUE(id,enrollment_id)
);
CREATE TABLE nite_connect.academic_assessment_evidence (
 assessment_id uuid NOT NULL, enrollment_id uuid NOT NULL, evidence_id uuid NOT NULL,
 position integer NOT NULL CHECK(position BETWEEN 0 AND 99), PRIMARY KEY(assessment_id,evidence_id), UNIQUE(assessment_id,position),
 FOREIGN KEY(assessment_id,enrollment_id) REFERENCES nite_connect.academic_assessments(id,enrollment_id),
 FOREIGN KEY(evidence_id,enrollment_id) REFERENCES nite_connect.academic_evidence(id,enrollment_id)
);
CREATE FUNCTION nite_connect.require_academic_assessment_evidence() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM nite_connect.academic_assessment_evidence WHERE assessment_id=NEW.id) THEN
  RAISE EXCEPTION 'Academic assessment requires evidence' USING ERRCODE='23514';
 END IF;
 RETURN NULL;
END; $$;
CREATE CONSTRAINT TRIGGER academic_assessment_evidence_required AFTER INSERT ON nite_connect.academic_assessments
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION nite_connect.require_academic_assessment_evidence();
CREATE TABLE nite_connect.academic_calculations (
 id uuid PRIMARY KEY, enrollment_id uuid NOT NULL REFERENCES nite_connect.academic_enrollments(id),
 policy_id uuid NOT NULL REFERENCES nite_connect.academic_policies(id), enrollment_version bigint NOT NULL,
 data jsonb NOT NULL CHECK(jsonb_typeof(data)='object'), created_at timestamptz NOT NULL DEFAULT clock_timestamp(), UNIQUE(id,enrollment_id),
 FOREIGN KEY(enrollment_id,policy_id) REFERENCES nite_connect.academic_enrollments(id,policy_id)
);
CREATE INDEX academic_calculations_enrollment_idx ON nite_connect.academic_calculations(enrollment_id,created_at,id);
CREATE TABLE nite_connect.academic_homologations (
 id uuid PRIMARY KEY, enrollment_id uuid NOT NULL REFERENCES nite_connect.academic_enrollments(id),
 calculation_id uuid NOT NULL UNIQUE REFERENCES nite_connect.academic_calculations(id),
 supersedes_id uuid UNIQUE REFERENCES nite_connect.academic_homologations(id),
 decided_by uuid NOT NULL REFERENCES nite_connect.members(id), reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(), UNIQUE(id,enrollment_id),
 FOREIGN KEY(calculation_id,enrollment_id) REFERENCES nite_connect.academic_calculations(id,enrollment_id),
 FOREIGN KEY(supersedes_id,enrollment_id) REFERENCES nite_connect.academic_homologations(id,enrollment_id)
);
CREATE UNIQUE INDEX academic_initial_homologation_idx ON nite_connect.academic_homologations(enrollment_id) WHERE supersedes_id IS NULL;
CREATE TABLE nite_connect.academic_reviews (
 id uuid PRIMARY KEY, enrollment_id uuid NOT NULL REFERENCES nite_connect.academic_enrollments(id),
 calculation_id uuid NOT NULL REFERENCES nite_connect.academic_calculations(id),
 homologation_id uuid REFERENCES nite_connect.academic_homologations(id),
 requested_by uuid NOT NULL REFERENCES nite_connect.members(id), reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0),
 status varchar(20) NOT NULL CHECK(status IN ('REQUESTED','ACCEPTED','REJECTED')),
 decided_by uuid REFERENCES nite_connect.members(id), decision_reason varchar(1000),
 version bigint NOT NULL DEFAULT 0 CHECK(version>=0), created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 FOREIGN KEY(calculation_id,enrollment_id) REFERENCES nite_connect.academic_calculations(id,enrollment_id),
 FOREIGN KEY(homologation_id,enrollment_id) REFERENCES nite_connect.academic_homologations(id,enrollment_id),
 CHECK((status='REQUESTED' AND decided_by IS NULL AND decision_reason IS NULL) OR
       (status<>'REQUESTED' AND decided_by IS NOT NULL AND decision_reason IS NOT NULL AND length(trim(decision_reason))>0))
);
CREATE UNIQUE INDEX academic_review_pending_idx ON nite_connect.academic_reviews(enrollment_id) WHERE status='REQUESTED';
CREATE TABLE nite_connect.academic_forwardings (
 id uuid PRIMARY KEY, homologation_id uuid NOT NULL UNIQUE REFERENCES nite_connect.academic_homologations(id),
 forwarded_by uuid NOT NULL REFERENCES nite_connect.members(id), procedure_reference varchar(1000) NOT NULL,
 receipt_reference varchar(1000) NOT NULL CHECK(length(trim(receipt_reference))>0),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE TABLE nite_connect.academic_events (
 id uuid PRIMARY KEY, enrollment_id uuid REFERENCES nite_connect.academic_enrollments(id),
 entity_type varchar(40) NOT NULL, entity_id uuid NOT NULL, actor_id uuid NOT NULL REFERENCES nite_connect.members(id),
 action varchar(40) NOT NULL, before_value jsonb, after_value jsonb,
 reason varchar(1000) NOT NULL CHECK(length(trim(reason))>0), occurred_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX academic_events_enrollment_idx ON nite_connect.academic_events(enrollment_id,occurred_at,id);
CREATE FUNCTION nite_connect.protect_academic_policy() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' OR OLD.status='CONFIRMED' THEN RAISE EXCEPTION 'Academic policy is immutable'; END IF;
 IF NEW.rules<>OLD.rules OR NEW.term_id<>OLD.term_id OR NEW.revision<>OLD.revision OR NEW.status<>'CONFIRMED' OR NEW.version<>OLD.version+1 THEN
  RAISE EXCEPTION 'Only policy confirmation is permitted';
 END IF;
 RETURN NEW;
END; $$;
CREATE TRIGGER protect_academic_policy BEFORE UPDATE OR DELETE ON nite_connect.academic_policies
 FOR EACH ROW EXECUTE FUNCTION nite_connect.protect_academic_policy();
CREATE FUNCTION nite_connect.protect_academic_plan() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' OR OLD.status='APROVADO' THEN RAISE EXCEPTION 'Approved plan is immutable'; END IF;
 RETURN NEW;
END; $$;
CREATE TRIGGER protect_academic_plan BEFORE UPDATE OR DELETE ON nite_connect.academic_plans
 FOR EACH ROW EXECUTE FUNCTION nite_connect.protect_academic_plan();
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['academic_terms','academic_evidence','academic_assessments','academic_assessment_evidence','academic_calculations','academic_homologations','academic_forwardings','academic_events'] LOOP
  EXECUTE format('CREATE TRIGGER immutable_academic_record BEFORE UPDATE OR DELETE ON nite_connect.%I FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation()',t);
 END LOOP;
 FOREACH t IN ARRAY ARRAY['academic_enrollments','academic_reviews'] LOOP
  EXECUTE format('CREATE TRIGGER preserve_academic_record BEFORE DELETE ON nite_connect.%I FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation()',t);
 END LOOP;
END; $$;
