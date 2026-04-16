ALTER TABLE grade_batches ADD COLUMN course_id BIGINT AFTER semester_id;
ALTER TABLE grade_batches ADD COLUMN org_unit_id BIGINT AFTER course_id;
ALTER TABLE grade_batches ADD INDEX idx_course (course_id);
ALTER TABLE grade_batches ADD INDEX idx_org_unit (org_unit_id);
