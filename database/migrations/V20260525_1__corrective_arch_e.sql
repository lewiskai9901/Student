-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- V20260525_1: 整改架构 E — 整改规则从模板提升到项目级
--
-- 双表存储:
--   * insp_project_corrective_rules — 按 scoring_mode 批量规则 (主)
--   * insp_project_item_overrides    — 按 template_item_id 个例覆盖
--
-- 引擎查询优先级: 个例 > 按题型 > 智能默认兜底
-- 老 insp_template_items.corrective_override 字段保留 (向下兼容预设), 不再被引擎消费
--
-- 设计文档: docs/plans/2026-05-25-corrective-architecture-E.md
-- ============================================================

-- Step 1: insp_project_corrective_rules (按题型批量规则)
SET @tbl_exists := (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'insp_project_corrective_rules');
SET @sql := IF(@tbl_exists = 0,
'CREATE TABLE insp_project_corrective_rules (
    id BIGINT NOT NULL COMMENT ''雪花 ID'',
    project_id BIGINT NOT NULL COMMENT ''关联项目 ID'',
    scoring_mode VARCHAR(50) NOT NULL COMMENT ''评分模式: PASS_FAIL/RATING_SCALE/...'',
    rule_json JSON NOT NULL COMMENT ''规则 JSON, 同 ItemRule schema'',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户 ID'',
    org_unit_id BIGINT NULL COMMENT ''组织 ID (横切关注点)'',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除'',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_mode (project_id, scoring_mode, deleted),
    KEY idx_project (project_id, deleted),
    KEY idx_org (org_unit_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT=''项目级按题型整改规则 (架构 E)''',
    'SELECT ''[idempotent] insp_project_corrective_rules 已存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Step 2: insp_project_item_overrides (按题目个例覆盖)
SET @tbl_exists := (
    SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'insp_project_item_overrides');
SET @sql := IF(@tbl_exists = 0,
'CREATE TABLE insp_project_item_overrides (
    id BIGINT NOT NULL COMMENT ''雪花 ID'',
    project_id BIGINT NOT NULL COMMENT ''关联项目 ID'',
    template_item_id BIGINT NOT NULL COMMENT ''关联模板检查项 ID'',
    rule_json JSON NOT NULL COMMENT ''规则 JSON, 同 ItemRule schema'',
    tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT ''租户 ID'',
    org_unit_id BIGINT NULL COMMENT ''组织 ID'',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT ''逻辑删除'',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_project_item (project_id, template_item_id, deleted),
    KEY idx_project (project_id, deleted),
    KEY idx_item (template_item_id, deleted),
    KEY idx_org (org_unit_id, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT=''项目级题目个例整改规则 (架构 E)''',
    'SELECT ''[idempotent] insp_project_item_overrides 已存在''');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
