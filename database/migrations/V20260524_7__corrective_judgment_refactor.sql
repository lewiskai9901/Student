-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- V20260524_7: 整改判定体系彻底重构 — 分层模型
--
-- 旧: insp_projects.corrective_strictness 单字段耦合 "判定阈值 + 建单流程"
-- 新:
--   * insp_template_items.item_rule (JSON) — 题目级"红线/排除/响应映射"
--   * insp_projects.corrective_enabled — 总开关 (取代 OFF strictness)
--   * insp_projects.corrective_strictness_adj — -2~+2 整体调档
--   * insp_projects.corrective_auto_create_level — HIGH/MEDIUM/LOW/NONE 自动建单门槛
--
-- 设计文档: docs/plans/2026-05-24-corrective-judgment-refactor.md
-- ============================================================

-- Step 1: 给 insp_template_items 加 item_rule JSON 列
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'insp_template_items'
      AND column_name = 'item_rule');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE insp_template_items ADD COLUMN item_rule JSON NULL
        COMMENT ''整改规则: {criticality, neverCorrect, baseSeverityMap, deadlineOverrideDays}'' AFTER scoring_config',
    'SELECT ''[idempotent] item_rule 已存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Step 2: 给 insp_projects 加 3 个新字段
SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'insp_projects'
      AND column_name = 'corrective_enabled');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE insp_projects ADD COLUMN corrective_enabled TINYINT NOT NULL DEFAULT 1
        COMMENT ''整改引擎总开关 (取代 OFF strictness)'' AFTER corrective_strictness',
    'SELECT ''[idempotent] corrective_enabled 已存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'insp_projects'
      AND column_name = 'corrective_strictness_adj');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE insp_projects ADD COLUMN corrective_strictness_adj INT NOT NULL DEFAULT 0
        COMMENT ''整体严格度调档 -2~+2 (升降一档)'' AFTER corrective_enabled',
    'SELECT ''[idempotent] corrective_strictness_adj 已存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'insp_projects'
      AND column_name = 'corrective_auto_create_level');
SET @sql := IF(@col_exists = 0,
    'ALTER TABLE insp_projects ADD COLUMN corrective_auto_create_level VARCHAR(10) NOT NULL DEFAULT ''NONE''
        COMMENT ''自动建单门槛: HIGH/MEDIUM/LOW/NONE'' AFTER corrective_strictness_adj',
    'SELECT ''[idempotent] corrective_auto_create_level 已存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Step 3: 回填 — 把旧 strictness 映射到新字段
--   STRICT  -> enabled=1, adj=+1, auto_level='LOW'
--   NORMAL  -> enabled=1, adj=0,  auto_level='NONE'
--   LENIENT -> enabled=1, adj=-1, auto_level='NONE'
--   OFF     -> enabled=0
UPDATE insp_projects
SET corrective_enabled = CASE WHEN corrective_strictness = 'OFF' THEN 0 ELSE 1 END,
    corrective_strictness_adj = CASE
        WHEN corrective_strictness = 'STRICT'  THEN 1
        WHEN corrective_strictness = 'LENIENT' THEN -1
        ELSE 0 END,
    corrective_auto_create_level = CASE
        WHEN corrective_strictness = 'STRICT'  THEN 'LOW'
        ELSE 'NONE' END
WHERE deleted = 0;

-- Step 4: 验证 — 输出回填分布
SELECT corrective_strictness, corrective_enabled, corrective_strictness_adj, corrective_auto_create_level, COUNT(*) AS cnt
FROM insp_projects WHERE deleted = 0
GROUP BY corrective_strictness, corrective_enabled, corrective_strictness_adj, corrective_auto_create_level;
