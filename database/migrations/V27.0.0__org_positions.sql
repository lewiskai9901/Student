-- V27.0.0 Organization Positions & Change Logs
-- 岗位模型 + 人岗关系 + 组织变更日志

-- ============================================================
-- 1. 岗位表
-- ============================================================
CREATE TABLE positions (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    position_code   VARCHAR(50)  NOT NULL COMMENT '岗位编码',
    position_name   VARCHAR(100) NOT NULL COMMENT '岗位名称',
    org_unit_id     BIGINT       NOT NULL COMMENT '所属组织单元',
    job_level       VARCHAR(50)  DEFAULT NULL COMMENT '职级(HIGH/MIDDLE/BASE/EXECUTIVE)',
    headcount       INT          DEFAULT 1 COMMENT '编制数',
    reports_to_id   BIGINT       DEFAULT NULL COMMENT '汇报岗位ID',
    responsibilities TEXT        DEFAULT NULL COMMENT '岗位职责',
    requirements    TEXT         DEFAULT NULL COMMENT '任职要求',
    sort_order      INT          DEFAULT 0,
    is_key_position TINYINT(1)   DEFAULT 0 COMMENT '是否关键岗位',
    enabled         TINYINT(1)   DEFAULT 1,
    tenant_id       BIGINT       NOT NULL DEFAULT 1,
    created_at      DATETIME     DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_at      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by      BIGINT,
    deleted         TINYINT      DEFAULT 0,
    version         INT          DEFAULT 0 COMMENT '乐观锁版本号',
    INDEX idx_org_unit_id (org_unit_id),
    INDEX idx_reports_to (reports_to_id),
    INDEX idx_tenant (tenant_id),
    INDEX idx_deleted (deleted)
) COMMENT='岗位表';

-- ============================================================
-- 2. 人岗关系表
-- ============================================================
CREATE TABLE user_positions (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id           BIGINT       NOT NULL,
    position_id       BIGINT       NOT NULL,
    is_primary        TINYINT(1)   DEFAULT 1 COMMENT '是否主岗',
    appointment_type  VARCHAR(30)  DEFAULT 'FORMAL' COMMENT 'FORMAL/ACTING/CONCURRENT/PROBATION',
    start_date        DATE         NOT NULL COMMENT '任命日期',
    end_date          DATE         DEFAULT NULL COMMENT '离任日期(NULL=在任)',
    appointment_reason VARCHAR(500) DEFAULT NULL COMMENT '任命原因',
    departure_reason  VARCHAR(500) DEFAULT NULL COMMENT '离任原因',
    tenant_id         BIGINT       NOT NULL DEFAULT 1,
    created_at        DATETIME     DEFAULT CURRENT_TIMESTAMP,
    created_by        BIGINT,
    updated_at        DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_position_id (position_id),
    INDEX idx_is_primary (is_primary),
    INDEX idx_end_date (end_date),
    INDEX idx_tenant (tenant_id)
) COMMENT='人岗关系表（支持历史）';

-- ============================================================
-- 3. 组织变更日志表
-- ============================================================
CREATE TABLE org_change_logs (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    entity_type    VARCHAR(50)  NOT NULL COMMENT 'ORG_UNIT/POSITION/USER_POSITION/CLASS/GRADE',
    entity_id      BIGINT       NOT NULL,
    change_type    VARCHAR(30)  NOT NULL COMMENT 'CREATE/UPDATE/DELETE/ENABLE/DISABLE/MERGE/SPLIT/MOVE',
    changes        JSON         NOT NULL COMMENT '[{field, oldValue, newValue}]',
    reason         VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
    operator_id    BIGINT       NOT NULL,
    operator_name  VARCHAR(50),
    operated_at    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    tenant_id      BIGINT       NOT NULL DEFAULT 1,
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_operator (operator_id),
    INDEX idx_operated_at (operated_at),
    INDEX idx_tenant (tenant_id)
) COMMENT='组织变更日志';

-- ============================================================
-- 4. org_units 新增字段
-- ============================================================
ALTER TABLE org_units ADD COLUMN attributes JSON DEFAULT NULL COMMENT '扩展属性';
ALTER TABLE org_units ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号';
ALTER TABLE org_units ADD COLUMN headcount INT DEFAULT NULL COMMENT '编制数';
