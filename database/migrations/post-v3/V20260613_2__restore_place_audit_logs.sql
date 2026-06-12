-- ============================================================================
-- V20260613_2: 恢复 place_audit_logs 表 (baseline_v3 squash 遗漏)
--
-- 背景: P6-5 建的 CloudTrail 风格场所审计表 (V20.0.0) 在 baseline_v3 squash 时
-- 被丢失 (squash 却保留了引用它的 trg_cascade_org_unit_update 触发器 — 触发器
-- 已由 V20260612_2 删除)。PlaceEventHandler 的全部审计写入自 2026-06-01 起
-- 静默失败 (WARN 吞掉)。2026-06-13 场所归属关系化 E2E 钓出此缺口。
--
-- DDL 还原自 V20.0.0__place_audit_system.sql + 补 tenant_id (PlaceEventHandler
-- INSERT 含 tenant_id 列)。幂等: CREATE TABLE IF NOT EXISTS。
-- ============================================================================

CREATE TABLE IF NOT EXISTS place_audit_logs (
  event_id VARCHAR(64) PRIMARY KEY COMMENT '全局唯一事件ID (UUID)',
  request_id VARCHAR(64) NOT NULL COMMENT '请求ID（同一请求的多个操作共享）',

  resource_type VARCHAR(50) NOT NULL COMMENT '资源类型：PLACE',
  resource_id BIGINT NOT NULL COMMENT '场所ID',
  resource_name VARCHAR(200) COMMENT '场所名称（冗余，便于查询）',

  event_name VARCHAR(100) NOT NULL COMMENT '事件名称：CREATE/UPDATE/DELETE/AssignOrganization/ChangeStatus',
  event_type VARCHAR(50) NOT NULL COMMENT '事件类型：ApiCall/ConsoleAccess/SystemAction',
  event_source VARCHAR(100) NOT NULL COMMENT '事件来源：place-service',
  event_time DATETIME(6) NOT NULL COMMENT '事件时间（微秒精度）',

  user_id BIGINT COMMENT '用户ID',
  user_name VARCHAR(100) COMMENT '用户名',
  user_type VARCHAR(50) COMMENT '用户类型：IAM_USER/ASSUMED_ROLE/SYSTEM',
  access_key_id VARCHAR(100) COMMENT 'JWT Token ID',
  session_id VARCHAR(100) COMMENT '会话ID',
  mfa_authenticated BOOLEAN COMMENT '是否MFA认证',

  source_ip VARCHAR(50) NOT NULL COMMENT '来源IP地址',
  user_agent VARCHAR(500) COMMENT '用户代理（浏览器/客户端）',
  referer VARCHAR(500) COMMENT '来源页面URL',
  api_endpoint VARCHAR(200) NOT NULL COMMENT 'API端点：/api/v9/places/{id}',
  http_method VARCHAR(10) COMMENT 'HTTP方法：GET/POST/PUT/DELETE',

  request_parameters JSON COMMENT '完整请求参数',
  response_elements JSON COMMENT '完整响应内容',
  before_snapshot JSON COMMENT '变更前完整状态快照',
  after_snapshot JSON COMMENT '变更后完整状态快照',
  changed_fields JSON COMMENT '变更字段列表 [{fieldName, oldValue, newValue}]',

  is_rollback BOOLEAN DEFAULT FALSE COMMENT '是否回滚操作',
  related_event_id VARCHAR(64) COMMENT '关联事件ID（回滚时指向原事件）',
  reason TEXT COMMENT '变更原因/备注',
  tags JSON COMMENT '自定义标签 {key: value}',

  tenant_id BIGINT NOT NULL DEFAULT 1 COMMENT '租户ID',

  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  ttl_expires_at DATETIME COMMENT '日志过期时间（合规要求：保留7年）',

  INDEX idx_resource (resource_type, resource_id, event_time DESC) COMMENT '按资源查询审计历史',
  INDEX idx_user (user_id, event_time DESC) COMMENT '按用户查询操作历史',
  INDEX idx_request (request_id) COMMENT '按请求ID关联多个操作',
  INDEX idx_event_name (event_name, event_time DESC) COMMENT '按操作类型查询',
  INDEX idx_source_ip (source_ip, event_time DESC) COMMENT '按IP查询（安全审计）',
  INDEX idx_event_time (event_time DESC) COMMENT '按时间倒序查询',
  INDEX idx_ttl (ttl_expires_at) COMMENT 'TTL过期清理索引'
) ENGINE=InnoDB
  ROW_FORMAT=COMPRESSED
  COMMENT='场所审计日志 - 完整5W1H记录（对标AWS CloudTrail）';
