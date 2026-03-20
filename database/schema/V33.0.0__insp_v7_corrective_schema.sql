-- =====================================================
-- V7 检查平台 - 整改管理 Schema (Phase 4)
-- 1 table: corrective_cases
-- =====================================================

CREATE TABLE IF NOT EXISTS `insp_corrective_cases` (
    `id`                       BIGINT         NOT NULL AUTO_INCREMENT,
    `tenant_id`                BIGINT         NOT NULL DEFAULT 0,
    `case_code`                VARCHAR(50)    NOT NULL,
    `submission_id`            BIGINT         NULL     COMMENT '关联提交',
    `detail_id`                BIGINT         NULL     COMMENT '关联明细项',
    `project_id`               BIGINT         NULL     COMMENT '关联项目',
    `task_id`                  BIGINT         NULL     COMMENT '关联任务',
    `target_type`              VARCHAR(20)    NULL     COMMENT 'ORG|PLACE|USER|ASSET',
    `target_id`                BIGINT         NULL,
    `target_name`              VARCHAR(200)   NULL,
    `issue_description`        TEXT           NOT NULL COMMENT '问题描述',
    `required_action`          TEXT           NULL     COMMENT '要求整改措施',
    `priority`                 VARCHAR(20)    NOT NULL DEFAULT 'MEDIUM' COMMENT 'LOW|MEDIUM|HIGH|CRITICAL',
    `deadline`                 DATETIME       NULL     COMMENT '整改截止时间',
    `assignee_id`              BIGINT         NULL     COMMENT '责任人ID',
    `assignee_name`            VARCHAR(100)   NULL     COMMENT '责任人姓名',
    `escalation_level`         INT            NOT NULL DEFAULT 0 COMMENT '升级层级',
    `status`                   VARCHAR(20)    NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN|ASSIGNED|IN_PROGRESS|SUBMITTED|VERIFIED|REJECTED|CLOSED|ESCALATED',
    `correction_note`          TEXT           NULL     COMMENT '整改说明',
    `correction_evidence_ids`  JSON           NULL     COMMENT '整改证据ID列表',
    `corrected_at`             DATETIME       NULL     COMMENT '提交整改时间',
    `verifier_id`              BIGINT         NULL     COMMENT '验证人ID',
    `verifier_name`            VARCHAR(100)   NULL     COMMENT '验证人姓名',
    `verified_at`              DATETIME       NULL     COMMENT '验证时间',
    `verification_note`        TEXT           NULL     COMMENT '验证说明',
    `created_by`               BIGINT         NULL,
    `created_at`               DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`               DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                  TINYINT        NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_case_code` (`case_code`, `deleted`),
    INDEX `idx_submission` (`submission_id`),
    INDEX `idx_project` (`project_id`),
    INDEX `idx_task` (`task_id`),
    INDEX `idx_assignee` (`assignee_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_priority` (`priority`),
    INDEX `idx_deadline` (`deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
    COMMENT='V7 整改案例';
