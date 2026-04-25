-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- V20260501_1: inspection 模块 org_unit_id / created_by 列治理
--
-- 背景: 检查平台 13 张表缺 org_unit_id 列, @DataPermission 拦截器注入的
-- `org_unit_id IN (?,?,...)` 条件对这些表会引发 SQL error. 之前未暴露
-- 是因为测试都用了 superadmin 走捷径(拦截器跳过).
--
-- 本迁移做 4 件事:
--   1. 创建 3 张实际缺失的 summary 表(PO 已存在但无 DDL)
--   2. ADD COLUMN org_unit_id (BIGINT NULL) + idx, 13 张表
--   3. ADD COLUMN created_by (BIGINT NULL), 漏列的若干表
--   4. 回填 org_unit_id (从有该列的关联表反推)
--
-- 回填策略:
--   - tasks/submission_details/evidences/observations/violations: 走 submission
--   - project_inspectors: 走 project (但 project 也无 org_unit, 先回填 project)
--   - corrective_cases: 走 submission
--   - corrective_subtasks: 走 case
--   - alerts: 走 target_id when target_type='ORG_UNIT'
--   - escalation_policies: 走 case
--   - projects: 走聚合 — 取该 project 下 submission 的 org_unit 众数 (近似)
--
-- 标准 information_schema 条件化, 可重复执行.
-- ============================================================

-- ==================== 1. 补 3 张缺失 summary 表 ====================

CREATE TABLE IF NOT EXISTS `insp_inspector_summaries` (
    `id`                          BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`                   BIGINT         NOT NULL DEFAULT 0,
    `project_id`                  BIGINT         NOT NULL,
    `inspector_id`                BIGINT         NOT NULL,
    `inspector_name`              VARCHAR(100)   NULL,
    `period_type`                 VARCHAR(20)    NOT NULL,
    `period_start`                DATE           NOT NULL,
    `period_end`                  DATE           NOT NULL,
    `total_tasks`                 INT            DEFAULT 0,
    `completed_tasks`             INT            DEFAULT 0,
    `cancelled_tasks`             INT            DEFAULT 0,
    `expired_tasks`               INT            DEFAULT 0,
    `avg_completion_time_minutes` DECIMAL(10,2)  NULL,
    `avg_score`                   DECIMAL(8,2)   NULL,
    `total_submissions`           INT            DEFAULT 0,
    `flagged_submissions`         INT            DEFAULT 0,
    `compliance_rate`             DECIMAL(5,2)   NULL,
    `org_unit_id`                 BIGINT         NULL COMMENT '检查员主组织(数据权限用)',
    `created_at`                  DATETIME       DEFAULT CURRENT_TIMESTAMP,
    `updated_at`                  DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                     TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_inspector_period` (`project_id`,`inspector_id`,`period_type`,`period_start`,`deleted`),
    INDEX `idx_inspector` (`inspector_id`),
    INDEX `idx_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查员维度汇总';

CREATE TABLE IF NOT EXISTS `insp_corrective_summaries` (
    `id`                       BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`                BIGINT         NOT NULL DEFAULT 0,
    `project_id`               BIGINT         NOT NULL,
    `period_type`              VARCHAR(20)    NOT NULL,
    `period_start`             DATE           NOT NULL,
    `period_end`               DATE           NOT NULL,
    `total_cases`              INT            DEFAULT 0,
    `open_cases`               INT            DEFAULT 0,
    `in_progress_cases`        INT            DEFAULT 0,
    `closed_cases`             INT            DEFAULT 0,
    `overdue_cases`            INT            DEFAULT 0,
    `avg_resolution_hours`     DECIMAL(10,2)  NULL,
    `compliance_rate`          DECIMAL(5,2)   NULL,
    `org_unit_id`              BIGINT         NULL COMMENT '所属组织(数据权限用)',
    `created_at`               DATETIME       DEFAULT CURRENT_TIMESTAMP,
    `updated_at`               DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                  TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_corrective_period` (`project_id`,`period_type`,`period_start`,`deleted`),
    INDEX `idx_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='整改汇总';

CREATE TABLE IF NOT EXISTS `insp_item_frequency_summaries` (
    `id`                  BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`           BIGINT         NOT NULL DEFAULT 0,
    `project_id`          BIGINT         NOT NULL,
    `template_item_id`    BIGINT         NOT NULL,
    `item_name`           VARCHAR(200)   NULL,
    `period_type`         VARCHAR(20)    NOT NULL,
    `period_start`        DATE           NOT NULL,
    `period_end`          DATE           NOT NULL,
    `occurrence_count`    INT            DEFAULT 0,
    `negative_count`      INT            DEFAULT 0,
    `flagged_count`       INT            DEFAULT 0,
    `avg_score`           DECIMAL(8,2)   NULL,
    `org_unit_id`         BIGINT         NULL COMMENT '所属组织(数据权限用)',
    `created_at`          DATETIME       DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`             TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_item_period` (`project_id`,`template_item_id`,`period_type`,`period_start`,`deleted`),
    INDEX `idx_org_unit` (`org_unit_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查项频次汇总';

-- ==================== 2. ADD org_unit_id + idx (幂等) ====================

-- 通用过程: 给 [tbl] 加 org_unit_id BIGINT NULL + idx_<tbl>_org_unit
DROP PROCEDURE IF EXISTS sp_insp_add_org_unit;
DELIMITER $$
CREATE PROCEDURE sp_insp_add_org_unit(IN p_tbl VARCHAR(64))
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    DECLARE idx_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO col_exists FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name=p_tbl AND column_name='org_unit_id';
    IF col_exists = 0 THEN
        SET @s = CONCAT('ALTER TABLE `', p_tbl, '` ADD COLUMN `org_unit_id` BIGINT NULL COMMENT ''数据权限过滤列''');
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
    SELECT COUNT(*) INTO idx_exists FROM information_schema.statistics
        WHERE table_schema=DATABASE() AND table_name=p_tbl AND index_name=CONCAT('idx_', p_tbl, '_org_unit');
    IF idx_exists = 0 THEN
        SET @s = CONCAT('ALTER TABLE `', p_tbl, '` ADD INDEX `idx_', p_tbl, '_org_unit` (`org_unit_id`)');
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END $$
DELIMITER ;

CALL sp_insp_add_org_unit('insp_projects');
CALL sp_insp_add_org_unit('insp_tasks');
CALL sp_insp_add_org_unit('insp_submission_details');
CALL sp_insp_add_org_unit('insp_evidences');
CALL sp_insp_add_org_unit('insp_project_inspectors');
CALL sp_insp_add_org_unit('insp_corrective_cases');
CALL sp_insp_add_org_unit('insp_corrective_subtasks');
CALL sp_insp_add_org_unit('insp_alerts');
CALL sp_insp_add_org_unit('insp_alert_rules');
CALL sp_insp_add_org_unit('insp_violation_records');
CALL sp_insp_add_org_unit('insp_submission_observations');
-- summary 类(若 DB 已存在但缺列, ALTER 补齐)
CALL sp_insp_add_org_unit('insp_inspector_summaries');
CALL sp_insp_add_org_unit('insp_corrective_summaries');
CALL sp_insp_add_org_unit('insp_item_frequency_summaries');
-- insp_escalation_policies 是 tenant 级 scoring 配置(关联 profile_id, 非 case),
-- 不需要 org_unit 行级过滤, 不加列

DROP PROCEDURE IF EXISTS sp_insp_add_org_unit;

-- ==================== 3. ADD created_by (漏列的若干表) ====================

DROP PROCEDURE IF EXISTS sp_insp_add_created_by;
DELIMITER $$
CREATE PROCEDURE sp_insp_add_created_by(IN p_tbl VARCHAR(64))
BEGIN
    DECLARE col_exists INT DEFAULT 0;
    SELECT COUNT(*) INTO col_exists FROM information_schema.columns
        WHERE table_schema=DATABASE() AND table_name=p_tbl AND column_name='created_by';
    IF col_exists = 0 THEN
        SET @s = CONCAT('ALTER TABLE `', p_tbl, '` ADD COLUMN `created_by` BIGINT NULL COMMENT ''记录创建人(SELF 范围用)''');
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END $$
DELIMITER ;

CALL sp_insp_add_created_by('insp_submissions');
CALL sp_insp_add_created_by('insp_submission_details');
CALL sp_insp_add_created_by('insp_evidences');
CALL sp_insp_add_created_by('insp_project_inspectors');
CALL sp_insp_add_created_by('insp_alerts');
CALL sp_insp_add_created_by('insp_submission_observations');
CALL sp_insp_add_created_by('insp_daily_summaries');
CALL sp_insp_add_created_by('insp_period_summaries');
CALL sp_insp_add_created_by('insp_tasks');

DROP PROCEDURE IF EXISTS sp_insp_add_created_by;

-- ==================== 4. 回填 org_unit_id ====================

-- 4.1 submission_details / evidences / observations / violations: 从 submission 反推
UPDATE insp_submission_details d
JOIN insp_submissions s ON s.id = d.submission_id
SET d.org_unit_id = s.org_unit_id
WHERE d.org_unit_id IS NULL AND s.org_unit_id IS NOT NULL;

UPDATE insp_evidences e
JOIN insp_submissions s ON s.id = e.submission_id
SET e.org_unit_id = s.org_unit_id
WHERE e.org_unit_id IS NULL AND s.org_unit_id IS NOT NULL;

UPDATE insp_submission_observations o
JOIN insp_submissions s ON s.id = o.submission_id
SET o.org_unit_id = s.org_unit_id
WHERE o.org_unit_id IS NULL AND s.org_unit_id IS NOT NULL;

UPDATE insp_violation_records v
JOIN insp_submissions s ON s.id = v.submission_id
SET v.org_unit_id = s.org_unit_id
WHERE v.org_unit_id IS NULL AND s.org_unit_id IS NOT NULL;

-- 4.2 tasks: 从 task 下任意 submission 取众数 org_unit (用 MIN 近似)
UPDATE insp_tasks t
JOIN (SELECT task_id, MIN(org_unit_id) org_unit_id
        FROM insp_submissions
        WHERE org_unit_id IS NOT NULL AND deleted = 0
        GROUP BY task_id) sg ON sg.task_id = t.id
SET t.org_unit_id = sg.org_unit_id
WHERE t.org_unit_id IS NULL;

-- 4.3 projects: 从 project 下 submissions 取众数 (MIN 近似)
UPDATE insp_projects p
JOIN (SELECT t.project_id, MIN(s.org_unit_id) org_unit_id
        FROM insp_submissions s
        JOIN insp_tasks t ON t.id = s.task_id
        WHERE s.org_unit_id IS NOT NULL AND s.deleted = 0
        GROUP BY t.project_id) pg ON pg.project_id = p.id
SET p.org_unit_id = pg.org_unit_id
WHERE p.org_unit_id IS NULL;

-- 4.4 project_inspectors: 走 project (在 project 已回填后)
UPDATE insp_project_inspectors pi
JOIN insp_projects p ON p.id = pi.project_id
SET pi.org_unit_id = p.org_unit_id
WHERE pi.org_unit_id IS NULL AND p.org_unit_id IS NOT NULL;

-- 4.5 corrective_cases: 走 submission, 否则走 project
UPDATE insp_corrective_cases c
LEFT JOIN insp_submissions s ON s.id = c.submission_id
LEFT JOIN insp_projects p ON p.id = c.project_id
SET c.org_unit_id = COALESCE(s.org_unit_id, p.org_unit_id)
WHERE c.org_unit_id IS NULL
  AND COALESCE(s.org_unit_id, p.org_unit_id) IS NOT NULL;

-- 4.6 corrective_subtasks: 走 case
UPDATE insp_corrective_subtasks st
JOIN insp_corrective_cases c ON c.id = st.case_id
SET st.org_unit_id = c.org_unit_id
WHERE st.org_unit_id IS NULL AND c.org_unit_id IS NOT NULL;

-- 4.7 alerts: 当 target_type='ORG_UNIT' 或 'ORG' 时, target_id 即为 org_unit_id
UPDATE insp_alerts
SET org_unit_id = target_id
WHERE org_unit_id IS NULL
  AND target_id IS NOT NULL
  AND target_type IN ('ORG_UNIT','ORG');

-- 4.8 alert_rules: 走关联的 project (规则模型上有 project_id, 全局规则保持 NULL)
UPDATE insp_alert_rules ar
JOIN insp_projects p ON p.id = ar.project_id
SET ar.org_unit_id = p.org_unit_id
WHERE ar.org_unit_id IS NULL AND p.org_unit_id IS NOT NULL;

-- ==================== 5. 注册新数据资源 ====================
-- inspection_alert: 预警是行级数据(target_id 指向某 org/place/user), 单独建模便于
-- 给"安全员/值班"角色细粒度分配, 而不混入 inspection_record 的笼统范畴
INSERT IGNORE INTO data_resources
  (resource_code, resource_name, domain_code, domain_name, registered_by, sort_order, enabled, tenant_id)
VALUES
  ('inspection_alert',     '预警记录',    'inspection', '检查平台', 'InspectionPlugin', 39, 1, 1),
  ('inspection_violation', '违规记录',    'inspection', '检查平台', 'InspectionPlugin', 40, 1, 1),
  ('inspection_observation','评分观察',   'inspection', '检查平台', 'InspectionPlugin', 41, 1, 1);

-- ==================== 6. 验证(注释掉, 手动跑) ====================
-- SELECT 'insp_projects' t, COUNT(*) total, COUNT(org_unit_id) filled FROM insp_projects WHERE deleted=0
-- UNION ALL SELECT 'insp_tasks', COUNT(*), COUNT(org_unit_id) FROM insp_tasks WHERE deleted=0
-- UNION ALL SELECT 'insp_corrective_cases', COUNT(*), COUNT(org_unit_id) FROM insp_corrective_cases WHERE deleted=0;
