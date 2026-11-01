-- Allows an institutionally confirmed policy to be recorded before the exact
-- official calendar date exists. Activation still requires effective_from.
ALTER TABLE nite_connect.participation_load_policy_drafts
 ALTER COLUMN effective_from DROP NOT NULL,
 ADD COLUMN intended_semester varchar(8),
 ADD COLUMN effective_window varchar(48),
 ADD CONSTRAINT participation_load_policy_schedule_check CHECK (
   (effective_from IS NOT NULL AND
      ((intended_semester IS NULL AND effective_window IS NULL) OR
       (intended_semester ~ '^[0-9]{4}\.[1-2]$' AND effective_window='PRIMEIRA_SEMANA_OFICIAL_DE_ATIVIDADES')))
   OR
   (effective_from IS NULL AND intended_semester ~ '^[0-9]{4}\.[1-2]$'
      AND effective_window='PRIMEIRA_SEMANA_OFICIAL_DE_ATIVIDADES')
 );
