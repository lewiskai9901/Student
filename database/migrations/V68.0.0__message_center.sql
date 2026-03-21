-- 消息中心：站内消息 + 订阅规则 + 消息模板

-- 站内消息
CREATE TABLE IF NOT EXISTS msg_notifications (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  user_id BIGINT NOT NULL COMMENT '接收人',
  title VARCHAR(200) NOT NULL,
  content TEXT NULL,
  msg_type VARCHAR(30) DEFAULT 'SYSTEM' COMMENT 'SYSTEM/EVENT/EVALUATION/MANUAL',
  source_event_type VARCHAR(50) NULL COMMENT '来源事件类型',
  source_ref_type VARCHAR(30) NULL,
  source_ref_id BIGINT NULL,
  is_read TINYINT DEFAULT 0,
  read_at DATETIME NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_user (user_id, is_read, created_at DESC),
  INDEX idx_tenant (tenant_id)
);

-- 订阅规则（事件→通知谁）
CREATE TABLE IF NOT EXISTS msg_subscription_rules (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  rule_name VARCHAR(100) NOT NULL,
  event_category VARCHAR(30) NULL COMMENT '事件大类，NULL=全部',
  event_type VARCHAR(50) NULL COMMENT '事件类型，NULL=大类下全部',
  target_mode VARCHAR(20) NOT NULL COMMENT 'BY_ROLE/BY_ORG_ADMIN/BY_USER/BY_RELATED',
  target_config TEXT NULL COMMENT 'JSON: 角色编码/组织ID/用户ID列表',
  channel VARCHAR(20) DEFAULT 'IN_APP' COMMENT 'IN_APP/EMAIL/WECHAT',
  template_id BIGINT NULL,
  is_enabled TINYINT DEFAULT 1,
  sort_order INT DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_tenant (tenant_id),
  INDEX idx_event (event_category, event_type)
);

-- 消息模板
CREATE TABLE IF NOT EXISTS msg_templates (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  template_code VARCHAR(50) NOT NULL,
  template_name VARCHAR(100) NOT NULL,
  title_template VARCHAR(200) NOT NULL COMMENT '标题模板，支持变量 {{subjectName}}',
  content_template TEXT NULL COMMENT '内容模板',
  is_system TINYINT DEFAULT 0,
  is_enabled TINYINT DEFAULT 1,
  created_by BIGINT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_code (tenant_id, template_code)
);

-- 预置模板
INSERT IGNORE INTO msg_templates (template_code, template_name, title_template, content_template, is_system) VALUES
('VIOLATION', '违规通知', '{{subjectName}} 发生违规', '{{subjectName}} 于 {{time}} 被记录违规：{{description}}', 1),
('GRADE', '检查评级', '{{subjectName}} 检查评级：{{grade}}', '{{subjectName}} 检查得分 {{score}}，评级 {{grade}}', 1),
('EVALUATION', '评选结果', '{{subjectName}} 评选结果：{{levelName}}', '{{subjectName}} 在 {{campaignName}} 中获评 {{levelName}}（排名第{{rank}}）', 1),
('SYSTEM', '系统通知', '{{title}}', '{{content}}', 1);
