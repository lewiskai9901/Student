-- Phase 5 W5.2: 关系审批 pending 表
CREATE TABLE IF NOT EXISTS pending_relation_approvals (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    -- pending 关系 5 元组
    resource_type   VARCHAR(50) NOT NULL,
    resource_id     BIGINT      NOT NULL,
    relation        VARCHAR(50) NOT NULL,
    subject_type    VARCHAR(30) NOT NULL,
    subject_id      BIGINT      NOT NULL,
    -- 期望落库的关系字段(grant 后 INSERT 用)
    access_level    VARCHAR(20),
    valid_from      DATETIME,
    valid_to        DATETIME,
    metadata        JSON,
    remark          VARCHAR(500),
    -- 审批
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                      COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
    requested_by    BIGINT      NOT NULL,
    requested_at    DATETIME    DEFAULT CURRENT_TIMESTAMP,
    approver_id     BIGINT      NULL,
    approved_at     DATETIME    NULL,
    rejection_reason VARCHAR(500) NULL,
    -- 关联生成的 access_relation id (审批通过后回填)
    granted_relation_id BIGINT NULL,
    tenant_id       BIGINT      NOT NULL DEFAULT 1,
    INDEX idx_pending_status (status, requested_at),
    INDEX idx_pending_subject (subject_type, subject_id, status),
    INDEX idx_pending_resource (resource_type, resource_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关系授权审批 pending 队列';

-- 给 relation_types 加 approval_required 列(关系类型字典层声明)
SET @stmt := IF(
  (SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='relation_types' AND column_name='approval_required') = 0,
  'ALTER TABLE relation_types ADD COLUMN approval_required TINYINT NOT NULL DEFAULT 0 AFTER metadata_schema',
  'SELECT 1');
PREPARE s FROM @stmt; EXECUTE s; DEALLOCATE PREPARE s;
