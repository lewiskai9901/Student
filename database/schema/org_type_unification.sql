-- ============================================================
-- 组织类型统一迁移脚本
-- 将 org_units.type_code 与 org_units.unit_type 同步
-- 删除旧 org_types 表
-- ============================================================

-- 1. 同步 type_code: 将 unit_type 的值写入 type_code（如果 type_code 为空）
UPDATE org_units SET type_code = unit_type WHERE (type_code IS NULL OR type_code = '') AND unit_type IS NOT NULL AND unit_type != '';

-- 2. 删除旧表 (V8.1.0 死代码)
DROP TABLE IF EXISTS org_types;

-- 3. 确认 org_unit_types 表数据完整性
-- 检查是否有启用的预置类型
SELECT type_code, type_name, level_order, is_academic, is_enabled
FROM org_unit_types
WHERE deleted = 0
ORDER BY level_order, sort_order;
