-- Add expiry and reason fields to user_roles for temporary authorization
SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_roles' AND COLUMN_NAME = 'expires_at') = 0,
    'ALTER TABLE user_roles ADD COLUMN expires_at DATETIME COMMENT ''过期时间（空=永久）''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_roles' AND COLUMN_NAME = 'reason') = 0,
    'ALTER TABLE user_roles ADD COLUMN reason VARCHAR(200) COMMENT ''授权原因''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_roles' AND COLUMN_NAME = 'granted_by') = 0,
    'ALTER TABLE user_roles ADD COLUMN granted_by BIGINT COMMENT ''授权人ID''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user_roles' AND COLUMN_NAME = 'granted_at') = 0,
    'ALTER TABLE user_roles ADD COLUMN granted_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''授权时间''',
    'SELECT 1'));
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Register teaching data modules (if not already present)
INSERT IGNORE INTO data_modules (tenant_id, module_code, module_name, domain_code, domain_name, resource_type, org_unit_field, creator_field, sort_order)
VALUES
(1, 'teaching_task',   '教学任务', 'teaching', '教务管理', NULL, 'org_unit_id', 'created_by', 501),
(1, 'exam_batch',      '考试批次', 'teaching', '教务管理', NULL, NULL,          'created_by', 502),
(1, 'student_grade',   '学生成绩', 'teaching', '教务管理', NULL, 'class_id',    NULL,         503);
