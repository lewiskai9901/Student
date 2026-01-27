-- ================================================================
-- 统一字段命名: department_id -> org_unit_id
-- 将 classes 表中的 department_id 列重命名为 org_unit_id
-- ================================================================

-- 1. 重命名 classes 表的 department_id 列
ALTER TABLE classes CHANGE COLUMN department_id org_unit_id BIGINT NOT NULL COMMENT '所属组织单元ID';

-- 2. 更新索引名称（如果有的话）
-- 先删除旧索引，再创建新索引
ALTER TABLE classes DROP INDEX IF EXISTS idx_department_id;
ALTER TABLE classes ADD INDEX idx_org_unit_id (org_unit_id);

-- 3. 验证修改结果
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'student_management'
  AND TABLE_NAME = 'classes'
  AND COLUMN_NAME = 'org_unit_id';
