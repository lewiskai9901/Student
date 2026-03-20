-- V30.0.0 统一活动事件表
-- 替代 org_change_logs, place_audit_logs, audit_logs, operation_logs 等分散的审计日志表

CREATE TABLE IF NOT EXISTS activity_events (
    id              BIGINT NOT NULL COMMENT 'Snowflake PK',
    request_id      VARCHAR(64)     COMMENT '请求追踪ID（同一请求产生的多条事件共享）',

    -- 资源标识
    resource_type   VARCHAR(50) NOT NULL  COMMENT 'ORG_UNIT / PLACE / USER / ROLE / STUDENT / INSPECTION ...',
    resource_id     VARCHAR(64) DEFAULT '' COMMENT '资源ID',
    resource_name   VARCHAR(200)          COMMENT '资源显示名（冗余，方便查看）',

    -- 动作
    action          VARCHAR(50) NOT NULL  COMMENT 'CREATE / UPDATE / DELETE / FREEZE / ASSIGN / LOGIN ...',
    action_label    VARCHAR(200)          COMMENT '可读描述: "更新组织单元" / "分配负责人"',
    result          VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS / FAILURE / PARTIAL',
    error_message   TEXT,

    -- 变更追踪
    changed_fields  JSON          COMMENT '[{fieldName, oldValue, newValue}] 字段级 diff',
    before_snapshot JSON          COMMENT '变更前快照（可选，关键资源用）',
    after_snapshot  JSON          COMMENT '变更后快照（可选）',

    -- 操作人
    user_id         BIGINT        COMMENT '操作人ID（系统动作为 null）',
    user_name       VARCHAR(100)  COMMENT '操作人姓名',

    -- HTTP 上下文
    source_ip       VARCHAR(50),
    user_agent      VARCHAR(500),
    api_endpoint    VARCHAR(200)  COMMENT '/api/org-units/123',
    http_method     VARCHAR(10)   COMMENT 'GET / POST / PUT / DELETE',

    -- 元数据
    reason          TEXT          COMMENT '操作原因/备注',
    module          VARCHAR(50)   COMMENT '模块分组: organization / place / access / inspection / user',
    tags            JSON          COMMENT '扩展标签 KV',

    -- 时间
    occurred_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    INDEX idx_resource (resource_type, resource_id, occurred_at DESC),
    INDEX idx_user (user_id, occurred_at DESC),
    INDEX idx_module_time (module, occurred_at DESC),
    INDEX idx_action_time (action, occurred_at DESC),
    INDEX idx_occurred_at (occurred_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一活动事件表';
