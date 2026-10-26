-- Intended student guidance is separate from private decision/audit reasons.
ALTER TABLE nite_connect.academic_enrollments ADD COLUMN feedback varchar(1000);
ALTER TABLE nite_connect.academic_plans ADD COLUMN feedback varchar(1000);
CREATE INDEX academic_homologations_enrollment_idx ON nite_connect.academic_homologations(enrollment_id,created_at,id);
CREATE INDEX academic_reviews_enrollment_idx ON nite_connect.academic_reviews(enrollment_id,created_at,id);
