-- =====================================================
-- 场所审计系统 - 完整5W1H审计日志
-- 对标: AWS CloudTrail, Google Cloud Audit Logs
-- 创建时间: 2026-02-13
-- =====================================================

-- 1. 创建完整审计日志表
CREATE TABLE IF NOT EXISTS place_audit_logs (
  -- ===== 唯一标识 =====
  event_id VARCHAR(64) PRIMARY KEY COMMENT '全局唯一事件ID (UUID)',
  request_id VARCHAR(64) NOT NULL COMMENT '请求ID（同一请求的多个操作共享）',

  -- ===== 资源信息 =====
  resource_type VARCHAR(50) NOT NULL COMMENT '资源类型：PLACE',
  resource_id BIGINT NOT NULL COMMENT '场所ID',
  resource_name VARCHAR(200) COMMENT '场所名称（冗余，便于查询）',

  -- ===== 操作信息 =====
  event_name VARCHAR(100) NOT NULL COMMENT '事件名称：CREATE/UPDATE/DELETE/AssignOrganization/ChangeStatus',
  event_type VARCHAR(50) NOT NULL COMMENT '事件类型：ApiCall/ConsoleAccess/SystemAction',
  event_source VARCHAR(100) NOT NULL COMMENT '事件来源：place-service',
  event_time DATETIME(6) NOT NULL COMMENT '事件时间（微秒精度）',

  -- ===== 用户身份（5W: Who） =====
  user_id BIGINT COMMENT '用户ID',
  user_name VARCHAR(100) COMMENT '用户名',
  user_type VARCHAR(50) COMMENT '用户类型：IAM_USER/ASSUMED_ROLE/SYSTEM',
  access_key_id VARCHAR(100) COMMENT 'JWT Token ID',
  session_id VARCHAR(100) COMMENT '会话ID',
  mfa_authenticated BOOLEAN COMMENT '是否MFA认证',

  -- ===== 请求上下文（5W: Where, How） =====
  source_ip VARCHAR(50) NOT NULL COMMENT '来源IP地址',
  user_agent VARCHAR(500) COMMENT '用户代理（浏览器/客户端）',
  referer VARCHAR(500) COMMENT '来源页面URL',
  api_endpoint VARCHAR(200) NOT NULL COMMENT 'API端点：/api/v9/places/{id}',
  http_method VARCHAR(10) COMMENT 'HTTP方法：GET/POST/PUT/DELETE',

  -- ===== 变更内容（5W: What） =====
  request_parameters JSON COMMENT '完整请求参数',
  response_elements JSON COMMENT '完整响应内容',
  before_snapshot JSON COMMENT '变更前完整状态快照',
  after_snapshot JSON COMMENT '变更后完整状态快照',
  changed_fields JSON COMMENT '变更字段列表 [{fieldName, oldValue, newValue}]',

  -- ===== 元数据（5W: Why, When） =====
  is_rollback BOOLEAN DEFAULT FALSE COMMENT '是否回滚操作',
  related_event_id VARCHAR(64) COMMENT '关联事件ID（回滚时指向原事件）',
  reason TEXT COMMENT '变更原因/备注',
  tags JSON COMMENT '自定义标签 {key: value}',

  -- ===== 审计元信息 =====
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  ttl_expires_at DATETIME COMMENT '日志过期时间（合规要求：保留7年）',

  -- ===== 索引优化 =====
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

-- 2. 插入测试审计数据（用于开发验证）
-- 注意：表已存在，跳过测试数据插入
/*
INSERT INTO place_audit_logs (
  event_id,
  request_id,
  resource_type,
  resource_id,
  resource_name,
  event_name,
  event_type,
  event_source,
  event_time,
  user_id,
  user_name,
  user_type,
  source_ip,
  api_endpoint,
  http_method,
  changed_fields,
  reason,
  created_at,
  ttl_expires_at
) VALUES (
  UUID(),
  UUID(),
  'PLACE',
  1,
  '测试宿舍楼',
  'AssignOrganization',
  'ApiCall',
  'place-service',
  NOW(6),
  1,
  'admin',
  'IAM_USER',
  '127.0.0.1',
  '/api/v9/places/1',
  'PUT',
  JSON_ARRAY(
    JSON_OBJECT('fieldName', 'orgUnitId', 'oldValue', '100', 'newValue', '200')
  ),
  '组织调整测试数据',
  NOW(),
  DATE_ADD(NOW(), INTERVAL 7 YEAR)
);
*/

-- 3. 创建视图：最近审计日志（便于查询）
CREATE OR REPLACE VIEW v_recent_place_audits AS
SELECT
  event_id,
  request_id,
  resource_id,
  resource_name,
  event_name,
  event_time,
  user_name,
  source_ip,
  changed_fields,
  reason
FROM place_audit_logs
WHERE event_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)
ORDER BY event_time DESC;

-- 4. 创建存储过程：清理过期审计日志
DELIMITER $$
CREATE PROCEDURE cleanup_expired_audit_logs()
BEGIN
  DECLARE deleted_count INT DEFAULT 0;

  -- 删除已过期的日志
  DELETE FROM place_audit_logs
  WHERE ttl_expires_at IS NOT NULL
    AND ttl_expires_at < NOW();

  SET deleted_count = ROW_COUNT();

  -- 记录清理日志
  INSERT INTO place_audit_logs (
    event_id,
    request_id,
    resource_type,
    resource_id,
    event_name,
    event_type,
    event_source,
    event_time,
    user_type,
    source_ip,
    api_endpoint,
    reason
  ) VALUES (
    UUID(),
    UUID(),
    'SYSTEM',
    0,
    'CleanupExpiredAuditLogs',
    'SystemAction',
    'audit-cleanup-job',
    NOW(6),
    'SYSTEM',
    '127.0.0.1',
    '/system/audit/cleanup',
    CONCAT('清理过期审计日志：', deleted_count, '条')
  );

  SELECT CONCAT('已清理 ', deleted_count, ' 条过期审计日志') AS result;
END$$
DELIMITER ;

-- 5. 创建定时任务：每天凌晨3点清理过期日志（可选）
-- CREATE EVENT IF NOT EXISTS evt_cleanup_audit_logs
-- ON SCHEDULE EVERY 1 DAY STARTS '2026-01-01 03:00:00'
-- DO CALL cleanup_expired_audit_logs();

COMMIT;

-- 验证脚本
-- SELECT COUNT(*) AS audit_count FROM place_audit_logs;
-- SELECT * FROM v_recent_place_audits LIMIT 10;
-- CALL cleanup_expired_audit_logs();
