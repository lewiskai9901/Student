-- I4: inspection_audit_logs 加 org_unit_id 列 + 索引
-- 之前查询 audit log 无 org 维度过滤, 跨组织全可见.
-- 用 actor 的主归属 org 作 fallback, 重要调用方可显式传 entity 的 org_unit_id.

-- 条件化执行 — 列存在时跳过
SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
                   WHERE table_schema = DATABASE()
                     AND table_name = 'inspection_audit_logs'
                     AND column_name = 'org_unit_id');

SET @sql := IF(@col_exists = 0,
    'ALTER TABLE `inspection_audit_logs` ADD COLUMN `org_unit_id` BIGINT NULL COMMENT ''actor 主归属组织 / entity 所在组织 (I4 加, 数据权限收窄用)'' AFTER `actor_user_name`',
    'SELECT ''column org_unit_id exists'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 条件化建索引
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
                   WHERE table_schema = DATABASE()
                     AND table_name = 'inspection_audit_logs'
                     AND index_name = 'idx_org_action');

SET @sql := IF(@idx_exists = 0,
    'CREATE INDEX `idx_org_action` ON `inspection_audit_logs` (`org_unit_id`, `action`)',
    'SELECT ''index idx_org_action exists'' AS msg');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
