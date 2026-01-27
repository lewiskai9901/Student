-- =====================================================
-- 统一列名规范: 将所有 department_id 改为 org_unit_id
-- 执行日期: 2026-01-19
-- =====================================================

-- 1. analysis_configs
ALTER TABLE analysis_configs
CHANGE COLUMN department_id org_unit_id BIGINT COMMENT '组织单元ID';

-- 2. check_record_class_stats
ALTER TABLE check_record_class_stats
CHANGE COLUMN department_id org_unit_id BIGINT COMMENT '组织单元ID';

-- 3. check_record_class_stats_new
ALTER TABLE check_record_class_stats_new
CHANGE COLUMN department_id org_unit_id BIGINT COMMENT '组织单元ID';

-- 4. class_size_standards
ALTER TABLE class_size_standards
CHANGE COLUMN department_id org_unit_id BIGINT NOT NULL COMMENT '组织单元ID';

-- 5. courses
ALTER TABLE courses
CHANGE COLUMN department_id org_unit_id BIGINT COMMENT '组织单元ID';

-- 6. dormitories
ALTER TABLE dormitories
CHANGE COLUMN department_id org_unit_id BIGINT COMMENT '组织单元ID';

-- 7. majors
ALTER TABLE majors
CHANGE COLUMN department_id org_unit_id BIGINT COMMENT '组织单元ID';

-- 8. student_evaluation_results
ALTER TABLE student_evaluation_results
CHANGE COLUMN department_id org_unit_id BIGINT COMMENT '组织单元ID';

-- 9. task_approval_configs
ALTER TABLE task_approval_configs
CHANGE COLUMN department_id org_unit_id BIGINT NOT NULL COMMENT '组织单元ID';

-- 10. task_approval_records
ALTER TABLE task_approval_records
CHANGE COLUMN department_id org_unit_id BIGINT COMMENT '组织单元ID';

-- 11. task_assignees
ALTER TABLE task_assignees
CHANGE COLUMN department_id org_unit_id BIGINT COMMENT '组织单元ID';

-- 12. user_departments
ALTER TABLE user_departments
CHANGE COLUMN department_id org_unit_id BIGINT NOT NULL COMMENT '组织单元ID';

-- =====================================================
-- 更新索引名称 (可选，保持索引功能)
-- =====================================================
-- 如有需要，可以重建索引以保持命名一致性
