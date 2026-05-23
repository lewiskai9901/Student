-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- V20260524_4: 调度组分区列表 JSON 拆为关系表 (smell C)
--
-- 旧: insp_inspection_plans.section_ids TEXT 存 JSON [101,102,103]
-- 新: insp_plan_sections (plan_id, section_id) 复合主键
-- ============================================================

CREATE TABLE IF NOT EXISTS `insp_plan_sections` (
    `plan_id`     BIGINT       NOT NULL,
    `section_id`  BIGINT       NOT NULL,
    `tenant_id`   BIGINT       NOT NULL DEFAULT 0,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`plan_id`, `section_id`, `deleted`),
    INDEX `idx_plan`    (`plan_id`),
    INDEX `idx_section` (`section_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='调度组-分区关系表 (替代 insp_inspection_plans.section_ids JSON)';

-- 回填: 解析旧 JSON
INSERT IGNORE INTO insp_plan_sections (plan_id, section_id, tenant_id, created_at, deleted)
SELECT
    p.id, jt.sid, COALESCE(p.tenant_id, 0), NOW(), 0
FROM insp_inspection_plans p
CROSS JOIN JSON_TABLE(
    COALESCE(NULLIF(p.section_ids, ''), '[]'),
    '$[*]' COLUMNS (sid BIGINT PATH '$')
) AS jt
WHERE p.deleted = 0
  AND p.section_ids IS NOT NULL
  AND p.section_ids <> ''
  AND p.section_ids <> '[]';

-- 旧列允许 NULL (DDL 还要求 NOT NULL, 改为可空便于停写)
ALTER TABLE insp_inspection_plans MODIFY COLUMN section_ids TEXT NULL;

-- 验证
SELECT COUNT(*) AS plan_section_relations FROM insp_plan_sections WHERE deleted = 0;

-- 告警: section_id 已不在分区表 (已删除/未存在)
SELECT ps.plan_id, ps.section_id, '[WARN] section 不在 insp_template_sections, 调度组指向死分区' AS msg
FROM insp_plan_sections ps
LEFT JOIN insp_template_sections ts ON ts.id = ps.section_id AND ts.deleted = 0
WHERE ps.deleted = 0 AND ts.id IS NULL;
