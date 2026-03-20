-- V39: V7 Platform BC tables (notification rules, report templates, webhooks, audit trail)

CREATE TABLE IF NOT EXISTS insp_notification_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT,
    project_id BIGINT,
    rule_name VARCHAR(200) NOT NULL,
    event_type VARCHAR(100) NOT NULL COMMENT 'Event class simple name',
    `condition` TEXT COMMENT 'JSON condition expression',
    channels VARCHAR(500) NOT NULL COMMENT 'JSON array: IN_APP, EMAIL, SMS, WECHAT',
    recipient_type VARCHAR(20) NOT NULL COMMENT 'ROLE/USER/DYNAMIC',
    recipient_config TEXT COMMENT 'JSON recipient configuration',
    is_enabled TINYINT(1) DEFAULT 1,
    priority INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_notif_rule_project (project_id),
    INDEX idx_notif_rule_event (event_type, is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知规则';

CREATE TABLE IF NOT EXISTS insp_report_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT,
    template_name VARCHAR(200) NOT NULL,
    template_code VARCHAR(50) NOT NULL,
    report_type VARCHAR(50) NOT NULL COMMENT 'DAILY_SUMMARY/PERIOD_REPORT/CORRECTIVE_REPORT/INSPECTOR_REPORT/CUSTOM',
    format_config TEXT COMMENT 'JSON layout configuration',
    header_config TEXT COMMENT 'JSON header/footer config',
    is_default TINYINT(1) DEFAULT 0,
    is_enabled TINYINT(1) DEFAULT 1,
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    UNIQUE INDEX uk_report_template_code (template_code),
    INDEX idx_report_template_type (report_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报告模板';

CREATE TABLE IF NOT EXISTS insp_webhook_subscriptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT,
    project_id BIGINT,
    subscription_name VARCHAR(200) NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    secret VARCHAR(200) COMMENT 'HMAC signing secret',
    event_types TEXT NOT NULL COMMENT 'JSON array of event type names',
    is_enabled TINYINT(1) DEFAULT 1,
    retry_count INT DEFAULT 3,
    last_triggered_at DATETIME,
    last_status VARCHAR(20) COMMENT 'SUCCESS/FAILED',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_webhook_project (project_id),
    INDEX idx_webhook_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Webhook订阅';

CREATE TABLE IF NOT EXISTS insp_audit_trail (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(100),
    action VARCHAR(100) NOT NULL COMMENT 'CREATE_TEMPLATE/PUBLISH_PROJECT/SUBMIT_INSPECTION etc.',
    resource_type VARCHAR(50) NOT NULL COMMENT 'InspTemplate/InspProject/InspTask etc.',
    resource_id BIGINT,
    resource_name VARCHAR(200),
    details TEXT COMMENT 'JSON with before/after snapshots',
    ip_address VARCHAR(50),
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_audit_resource (resource_type, resource_id),
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_time (occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志';
