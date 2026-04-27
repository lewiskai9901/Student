-- P1#8: 申诉流程 — 让 inspection:appeal:create/review/view 三个权限点落到真表/真服务上
-- 通用化 schema: 不绑班级, 用 SubmissionDetail 作为申诉对象, subject_type/id 走通用模型

CREATE TABLE IF NOT EXISTS `inspection_appeals` (
    `id`                     BIGINT       NOT NULL AUTO_INCREMENT,
    `tenant_id`              BIGINT       NOT NULL DEFAULT 0,
    `org_unit_id`            BIGINT       NULL COMMENT '组织单元 ID, 用于数据权限过滤',
    `appeal_code`            VARCHAR(50)  NOT NULL COMMENT '申诉编号',
    `submission_detail_id`   BIGINT       NOT NULL COMMENT '关联的扣分明细 ID',
    `submission_id`          BIGINT       NULL COMMENT '冗余: 关联的 submission ID',
    `task_id`                BIGINT       NULL COMMENT '冗余: 关联的 task ID',
    `project_id`             BIGINT       NULL COMMENT '冗余: 关联的 project ID',
    `subject_type`           VARCHAR(20)  NULL COMMENT '申诉主体类型 USER/ORG/PLACE/ASSET',
    `subject_id`             BIGINT       NULL COMMENT '申诉主体 ID',
    `submitter_user_id`      BIGINT       NOT NULL COMMENT '提交申诉的用户 ID',
    `submitter_name`         VARCHAR(100) NULL,
    `reason`                 TEXT         NOT NULL COMMENT '申诉理由',
    `attachments`            JSON         NULL COMMENT '证据附件 URL 列表',
    `expected_adjustment`    DECIMAL(10,2) NULL COMMENT '期望分数调整',
    `final_adjustment`       DECIMAL(10,2) NULL COMMENT '审核后实际分数调整',
    `status`                 VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING|APPROVED|REJECTED|WITHDRAWN',
    `reviewer_id`            BIGINT       NULL,
    `reviewer_name`          VARCHAR(100) NULL,
    `reviewer_comment`       TEXT         NULL,
    `reviewed_at`            DATETIME     NULL,
    `created_at`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`                TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_appeal_code` (`appeal_code`, `deleted`),
    INDEX `idx_submission_detail` (`submission_detail_id`),
    INDEX `idx_submitter` (`submitter_user_id`),
    INDEX `idx_status_created` (`status`, `created_at`),
    INDEX `idx_org_unit` (`org_unit_id`),
    INDEX `idx_project` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='检查申诉 (P1#8)';
