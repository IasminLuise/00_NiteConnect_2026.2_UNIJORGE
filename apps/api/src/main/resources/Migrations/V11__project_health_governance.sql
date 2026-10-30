CREATE TABLE nite_connect.project_health_policy_versions (
 id uuid PRIMARY KEY,
 project_id uuid NOT NULL REFERENCES nite_connect.projects(id),
 version integer NOT NULL CHECK(version > 0),
 attention_overdue_days integer NOT NULL CHECK(attention_overdue_days > 0),
 critical_overdue_days integer NOT NULL CHECK(critical_overdue_days > attention_overdue_days),
 attention_no_update_days integer,
 critical_no_update_days integer,
 valid_from timestamptz NOT NULL,
 valid_until timestamptz,
 created_by uuid REFERENCES nite_connect.members(id),
 origin varchar(32) NOT NULL CHECK(origin IN ('BASELINE_APROVADA','CONFIGURADA_POR_GESTAO')),
 reason varchar(1000) NOT NULL CHECK(length(trim(reason)) > 0),
 created_at timestamptz NOT NULL DEFAULT clock_timestamp(),
 UNIQUE(project_id,version),
 CHECK((attention_no_update_days IS NULL AND critical_no_update_days IS NULL)
    OR (attention_no_update_days > 0 AND critical_no_update_days > attention_no_update_days)),
 CHECK((origin='BASELINE_APROVADA' AND created_by IS NULL)
    OR (origin='CONFIGURADA_POR_GESTAO' AND created_by IS NOT NULL)),
 CHECK(valid_until IS NULL OR valid_until > valid_from)
);
CREATE UNIQUE INDEX project_health_one_current_policy_idx
 ON nite_connect.project_health_policy_versions(project_id) WHERE valid_until IS NULL;
CREATE INDEX project_health_policy_history_idx
 ON nite_connect.project_health_policy_versions(project_id,version DESC);

CREATE FUNCTION nite_connect.protect_project_health_policy() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'Project health policy versions cannot be deleted'; END IF;
 IF NEW.id<>OLD.id OR NEW.project_id<>OLD.project_id OR NEW.version<>OLD.version
    OR NEW.attention_overdue_days<>OLD.attention_overdue_days
    OR NEW.critical_overdue_days<>OLD.critical_overdue_days
    OR NEW.attention_no_update_days IS DISTINCT FROM OLD.attention_no_update_days
    OR NEW.critical_no_update_days IS DISTINCT FROM OLD.critical_no_update_days
    OR NEW.valid_from<>OLD.valid_from OR NEW.created_by IS DISTINCT FROM OLD.created_by
    OR NEW.origin<>OLD.origin
    OR NEW.reason<>OLD.reason OR NEW.created_at<>OLD.created_at
    OR OLD.valid_until IS NOT NULL OR NEW.valid_until IS NULL OR NEW.valid_until<=OLD.valid_from THEN
  RAISE EXCEPTION 'Only one closing instant may be added to a project health policy version';
 END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER protect_project_health_policy_versions
 BEFORE UPDATE OR DELETE ON nite_connect.project_health_policy_versions
 FOR EACH ROW EXECUTE FUNCTION nite_connect.protect_project_health_policy();

CREATE FUNCTION nite_connect.seed_project_health_policy() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 INSERT INTO nite_connect.project_health_policy_versions(
  id,project_id,version,attention_overdue_days,critical_overdue_days,
  attention_no_update_days,critical_no_update_days,valid_from,created_by,origin,reason)
 VALUES (
  gen_random_uuid(),NEW.id,1,7,14,14,30,clock_timestamp(),NULL,'BASELINE_APROVADA',
  'Baseline inicial aprovada: atraso 7/14 dias; sem atualização relevante 14/30 dias. Gestão Geral/Coordenação pode ajustar por projeto.');
 RETURN NEW;
END;
$$;

INSERT INTO nite_connect.project_health_policy_versions(
 id,project_id,version,attention_overdue_days,critical_overdue_days,
 attention_no_update_days,critical_no_update_days,valid_from,created_by,origin,reason)
SELECT gen_random_uuid(),p.id,1,7,14,14,30,clock_timestamp(),NULL,'BASELINE_APROVADA',
 'Baseline inicial aprovada: atraso 7/14 dias; sem atualização relevante 14/30 dias. Gestão Geral/Coordenação pode ajustar por projeto.'
FROM nite_connect.projects p;

CREATE TRIGGER seed_project_health_policy_after_insert
 AFTER INSERT ON nite_connect.projects
 FOR EACH ROW EXECUTE FUNCTION nite_connect.seed_project_health_policy();

CREATE TABLE nite_connect.project_health_evaluations (
 id uuid PRIMARY KEY,
 project_id uuid NOT NULL REFERENCES nite_connect.projects(id),
 policy_version_id uuid NOT NULL REFERENCES nite_connect.project_health_policy_versions(id),
 status varchar(24) NOT NULL CHECK(status IN ('REGULAR','ATENCAO','CRITICO','DADOS_INSUFICIENTES')),
 evaluated_by uuid NOT NULL REFERENCES nite_connect.members(id),
 evaluated_at timestamptz NOT NULL,
 max_overdue_days integer NOT NULL CHECK(max_overdue_days >= 0),
 days_since_relevant_update integer CHECK(days_since_relevant_update >= 0),
 attention_overdue_days integer NOT NULL CHECK(attention_overdue_days > 0),
 critical_overdue_days integer NOT NULL CHECK(critical_overdue_days > attention_overdue_days),
 attention_no_update_days integer,
 critical_no_update_days integer,
 source_activity_count integer NOT NULL CHECK(source_activity_count >= 0),
 reason varchar(2000) NOT NULL CHECK(length(trim(reason)) > 0),
 CHECK((attention_no_update_days IS NULL AND critical_no_update_days IS NULL)
    OR (attention_no_update_days > 0 AND critical_no_update_days > attention_no_update_days))
);
CREATE INDEX project_health_evaluations_history_idx
 ON nite_connect.project_health_evaluations(project_id,evaluated_at DESC,id);
CREATE TRIGGER immutable_project_health_evaluations
 BEFORE UPDATE OR DELETE ON nite_connect.project_health_evaluations
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();

CREATE TABLE nite_connect.project_health_evaluation_sources (
 evaluation_id uuid NOT NULL REFERENCES nite_connect.project_health_evaluations(id),
 activity_id uuid NOT NULL REFERENCES nite_connect.activities(id),
 source_kind varchar(24) NOT NULL CHECK(source_kind IN ('ATRASO','ATUALIZACAO_RELEVANTE')),
 PRIMARY KEY(evaluation_id,activity_id,source_kind)
);
CREATE TRIGGER immutable_project_health_evaluation_sources
 BEFORE UPDATE OR DELETE ON nite_connect.project_health_evaluation_sources
 FOR EACH ROW EXECUTE FUNCTION nite_connect.reject_history_mutation();
