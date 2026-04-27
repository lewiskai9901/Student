-- C: 通用 inspection 审计日志表 — 落地高敏感动作 (reject/approve/unassign/extend/upgrade)
-- 不绑具体聚合, 用 entity_type + entity_id + action 三元组定位

CREATE TABLE IF NOT EXISTS `inspection_audit_logs` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`       BIGINT       NOT NULL DEFAULT 0,
    `entity_type`     VARCHAR(40)  NOT NULL COMMENT '实体类型: CorrectiveCase / InspAppeal / InspTask / InspProject',
    `entity_id`       BIGINT       NOT NULL COMMENT '实体 ID',
    `entity_code`     VARCHAR(50)  NULL     COMMENT '业务编号 (caseCode/appealCode/taskCode/projectCode)',
    `action`          VARCHAR(40)  NOT NULL COMMENT '动作: APPEAL_APPROVED/APPEAL_REJECTED/CASE_UNASSIGNED/TASK_REJECTED/TASK_DEADLINE_EXTENDED/PROJECT_TEMPLATE_UPGRADED 等',
    `actor_user_id`   BIGINT       NULL     COMMENT '操作人 ID (system 自动则 null)',
    `actor_user_name` VARCHAR(100) NULL,
    `reason`          TEXT         NULL     COMMENT '动作原因 / 备注',
    `payload`         JSON         NULL     COMMENT '动作上下文 (前后值/相关 ID 等)',
    `occurred_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_entity` (`entity_type`, `entity_id`),
    INDEX `idx_actor` (`actor_user_id`),
    INDEX `idx_action_time` (`action`, `occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='检查平台高敏感动作审计日志 (review #14)';
