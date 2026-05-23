-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- V20260524_3: 调度组「检查员指派策略」显式化, 干掉"空 = 全员"falsy magic
--
-- 背景: 旧实现 inspector_ids JSON 空数组 → 业务语义"全员可领取"
--   * 管理员清空成员列表会从"3 人小队"突变为"全员", 无警告
--   * 语义靠 falsy 判断, 容易误操作
--
-- 新模型:
--   assign_strategy 枚举:
--     SPECIFIC     - 限定: 必须搭配 insp_plan_inspectors 至少 1 人
--     OPEN_TO_ALL  - 开放: 项目全员可领取 (此时不存任何 plan_inspectors 行)
--
-- 回填: 现有数据按"insp_plan_inspectors 有行 → SPECIFIC, 否则 OPEN_TO_ALL".
-- ============================================================

-- Step 1: 加列
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'insp_inspection_plans'
      AND column_name = 'assign_strategy');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE insp_inspection_plans ADD COLUMN assign_strategy VARCHAR(20) NOT NULL DEFAULT ''OPEN_TO_ALL''
        COMMENT ''检查员指派策略: SPECIFIC=限定到 insp_plan_inspectors / OPEN_TO_ALL=项目全员可领'' AFTER inspector_ids',
    'SELECT ''[idempotent] assign_strategy 已存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Step 2: 回填: 有 inspector 关系 → SPECIFIC; 否则 OPEN_TO_ALL
UPDATE insp_inspection_plans p
SET p.assign_strategy = CASE
    WHEN EXISTS (
        SELECT 1 FROM insp_plan_inspectors pi
        WHERE pi.plan_id = p.id AND pi.deleted = 0
    ) THEN 'SPECIFIC'
    ELSE 'OPEN_TO_ALL'
END
WHERE p.deleted = 0;

-- Step 3: 验证
SELECT assign_strategy, COUNT(*) FROM insp_inspection_plans WHERE deleted = 0 GROUP BY assign_strategy;
