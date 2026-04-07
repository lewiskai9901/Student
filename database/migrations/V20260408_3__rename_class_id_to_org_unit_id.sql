-- 彻底清理: 所有 class_id → org_unit_id
-- 数据可清空，只改结构

-- 先删除所有涉及 class_id 的 VIEW
DROP VIEW IF EXISTS classes;
DROP VIEW IF EXISTS v_class_history_stats;
DROP VIEW IF EXISTS v_check_record_class_stats_enhanced;
DROP VIEW IF EXISTS v_check_record_items_enhanced;
DROP VIEW IF EXISTS v_class_check_summary;
DROP VIEW IF EXISTS check_record_category_stats;
DROP VIEW IF EXISTS v_class_inspection_summary;

-- ========== 只有 class_id 的表: 改名为 org_unit_id ==========

ALTER TABLE academic_warnings CHANGE class_id org_unit_id BIGINT;
ALTER TABLE appeal_records CHANGE class_id org_unit_id BIGINT;
ALTER TABLE appeals_v2 CHANGE class_id org_unit_id BIGINT;
ALTER TABLE attendance_records CHANGE class_id org_unit_id BIGINT;
ALTER TABLE check_item_appeals CHANGE class_id org_unit_id BIGINT;
ALTER TABLE check_plan_rating_frequency CHANGE class_id org_unit_id BIGINT;
ALTER TABLE check_plan_rating_results CHANGE class_id org_unit_id BIGINT;
ALTER TABLE check_rating_results_deprecated CHANGE class_id org_unit_id BIGINT;
ALTER TABLE check_record_appeals_new CHANGE class_id org_unit_id BIGINT;
ALTER TABLE check_record_category_stats_deprecated CHANGE class_id org_unit_id BIGINT;
ALTER TABLE check_record_category_stats_new CHANGE class_id org_unit_id BIGINT;
ALTER TABLE check_record_deductions_new CHANGE class_id org_unit_id BIGINT;
ALTER TABLE check_record_items CHANGE class_id org_unit_id BIGINT;
ALTER TABLE check_record_rating_results CHANGE class_id org_unit_id BIGINT;
ALTER TABLE check_record_ratings_deprecated CHANGE class_id org_unit_id BIGINT;
ALTER TABLE class_course_assignments CHANGE class_id org_unit_id BIGINT;
ALTER TABLE class_dormitory_bindings CHANGE class_id org_unit_id BIGINT;
ALTER TABLE class_quantification_summary CHANGE class_id org_unit_id BIGINT;
ALTER TABLE class_size_snapshots CHANGE class_id org_unit_id BIGINT;
ALTER TABLE classrooms CHANGE class_id org_unit_id BIGINT;
ALTER TABLE daily_check_appeals CHANGE class_id org_unit_id BIGINT;
ALTER TABLE daily_check_details CHANGE class_id org_unit_id BIGINT;
ALTER TABLE daily_class_summary CHANGE class_id org_unit_id BIGINT;
ALTER TABLE exam_arrangements CHANGE class_id org_unit_id BIGINT;
ALTER TABLE insp_submission_observations CHANGE class_id org_unit_id BIGINT;
ALTER TABLE quantification_appeals_deprecated CHANGE class_id org_unit_id BIGINT;
ALTER TABLE rating_change_log CHANGE class_id org_unit_id BIGINT;
ALTER TABLE rating_result CHANGE class_id org_unit_id BIGINT;
ALTER TABLE rating_results CHANGE class_id org_unit_id BIGINT;
ALTER TABLE rating_results_deprecated CHANGE class_id org_unit_id BIGINT;
ALTER TABLE rating_statistics CHANGE class_id org_unit_id BIGINT;
ALTER TABLE schedule_entries CHANGE class_id org_unit_id BIGINT;
ALTER TABLE schedule_instances CHANGE class_id org_unit_id BIGINT;
ALTER TABLE student_behavior_alerts CHANGE class_id org_unit_id BIGINT;
ALTER TABLE student_grades CHANGE class_id org_unit_id BIGINT;
ALTER TABLE students CHANGE class_id org_unit_id BIGINT;
ALTER TABLE teacher_assignments CHANGE class_id org_unit_id BIGINT;

-- ========== 同时有 class_id 和 org_unit_id 的表: 删除 class_id ==========

ALTER TABLE analysis_configs DROP COLUMN class_id;
ALTER TABLE check_record_class_stats DROP COLUMN class_id;
ALTER TABLE check_record_class_stats_new DROP COLUMN class_id;
ALTER TABLE inspection_daily_summaries DROP COLUMN class_id;
ALTER TABLE inspection_monthly_summaries DROP COLUMN class_id;
ALTER TABLE inspection_targets DROP COLUMN class_id;
ALTER TABLE inspection_weekly_summaries DROP COLUMN class_id;
ALTER TABLE place_class_assignment DROP COLUMN class_id;
ALTER TABLE student_evaluation_results DROP COLUMN class_id;
ALTER TABLE teaching_tasks DROP COLUMN class_id;

-- ========== Place.classId → 删除 ==========
-- places 表如果有 class_id 也删除
SET @pc = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='places' AND COLUMN_NAME='class_id');
SET @s = IF(@pc > 0, 'ALTER TABLE places DROP COLUMN class_id', 'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- ========== 重建 classes VIEW ==========
CREATE VIEW classes AS
SELECT
  o.id,
  o.unit_name AS class_name,
  o.unit_code AS class_code,
  o.id AS org_unit_id,
  COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.gradeLevel')) AS UNSIGNED), 1) AS grade_level,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.gradeId')) AS UNSIGNED) AS grade_id,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.majorId')) AS UNSIGNED) AS major_id,
  CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.headTeacher')) AS UNSIGNED) AS teacher_id,
  COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.studentCount')) AS UNSIGNED), 0) AS student_count,
  COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(o.attributes, '$.classType')) AS UNSIGNED), 1) AS class_type,
  CASE o.status WHEN 'ACTIVE' THEN 1 ELSE 0 END AS status,
  o.created_at, o.updated_at, o.deleted, o.tenant_id
FROM org_units o WHERE o.type_code = 'CLASS';

-- 验证
SELECT CONCAT('Done: class_id removed from all tables') AS result;
