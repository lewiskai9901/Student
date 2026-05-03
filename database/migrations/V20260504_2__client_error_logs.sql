-- ============================================================
-- P3 客户端错误日志 (前端 Vue / window.onerror / unhandledrejection)
-- 自建错误上报, 无需 Sentry SaaS 依赖, 适合内网部署
-- ============================================================

CREATE TABLE IF NOT EXISTS client_error_logs (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id       BIGINT      NOT NULL DEFAULT 0,
  user_id         BIGINT      NULL COMMENT '触发错误的用户 ID, 未登录为 NULL',
  level           VARCHAR(16) NOT NULL DEFAULT 'ERROR' COMMENT 'ERROR | WARN | INFO',
  source          VARCHAR(32) NOT NULL DEFAULT 'JS' COMMENT 'JS | VUE | HTTP | UNHANDLED',
  message         VARCHAR(1024) NOT NULL,
  stack           TEXT        NULL,
  url             VARCHAR(512) NULL COMMENT '触发错误的页面 URL',
  route_path      VARCHAR(256) NULL COMMENT 'Vue Router 路径',
  user_agent      VARCHAR(512) NULL,
  fingerprint     VARCHAR(64) NULL COMMENT '错误指纹 (message hash) 用于去重统计',
  occurrence_count INT        NOT NULL DEFAULT 1 COMMENT '同指纹合并次数',
  first_occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_occurred_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  resolved        TINYINT(1)  NOT NULL DEFAULT 0,
  resolved_by     BIGINT      NULL,
  resolved_at     DATETIME    NULL,
  created_at      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_cel_user_time (user_id, last_occurred_at),
  INDEX idx_cel_fingerprint (fingerprint, resolved),
  INDEX idx_cel_level_time (level, last_occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端错误日志';
