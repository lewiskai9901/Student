-- 将 applicable_scope 从 ENUM 改为 VARCHAR，支持逗号分隔多选（ORG/PLACE/USER/ALL）
ALTER TABLE inspection_templates
  MODIFY COLUMN applicable_scope VARCHAR(50) DEFAULT 'ALL' COMMENT '适用范围: ALL/ORG/PLACE/USER，逗号分隔多选';

-- 将旧值（DEPARTMENT/GRADE/CUSTOM等）统一重置为 ALL
UPDATE inspection_templates SET applicable_scope = 'ALL' WHERE applicable_scope IS NOT NULL;

-- 删除不再需要的 applicable_config 列 — MySQL 8.0 条件化
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='inspection_templates' AND COLUMN_NAME='applicable_config');
SET @s := IF(@c>0, "ALTER TABLE inspection_templates DROP COLUMN applicable_config", "SELECT 1");
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
