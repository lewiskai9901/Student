-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- V20260524_5: 物理 DROP insp_inspection_plans 的两个旧 JSON 列
--
-- 背景:
--   V20260524_2 引入 insp_plan_inspectors 关系表 + 停写 inspector_ids 列
--   V20260524_4 引入 insp_plan_sections   关系表 + 停写 section_ids 列
--   关系表是 source of truth, 旧 JSON 列已 2 周无写入, 现安全 DROP.
--
-- 配合代码侧: InspectionPlan.getInspectorIds() / getSectionIds() 仍保留
-- 作为 List 列表的 JSON 串序列化视图 (出参兼容旧消费), 但 DB 端列已不存在.
-- ============================================================

-- Step 1: DROP inspector_ids 列 (idempotent)
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'insp_inspection_plans'
      AND column_name = 'inspector_ids');
SET @sql := IF(@col_exists > 0,
    'ALTER TABLE insp_inspection_plans DROP COLUMN inspector_ids',
    'SELECT ''[idempotent] inspector_ids 已不存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Step 2: DROP section_ids 列 (idempotent)
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'insp_inspection_plans'
      AND column_name = 'section_ids');
SET @sql := IF(@col_exists > 0,
    'ALTER TABLE insp_inspection_plans DROP COLUMN section_ids',
    'SELECT ''[idempotent] section_ids 已不存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 验证
SHOW COLUMNS FROM insp_inspection_plans WHERE Field IN ('inspector_ids', 'section_ids');
