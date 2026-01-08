-- 微信推送记录表
-- Version: 4.5.0
-- Date: 2025-12-25
-- Description: 创建微信模板消息推送记录表

CREATE TABLE IF NOT EXISTS wechat_push_record (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    business_type VARCHAR(50) NOT NULL COMMENT '业务类型: ANNOUNCEMENT-公告, NOTIFICATION-通报',
    business_id BIGINT NOT NULL COMMENT '业务ID',
    user_id BIGINT NOT NULL COMMENT '目标用户ID',
    openid VARCHAR(64) NOT NULL COMMENT '微信openid',
    template_id VARCHAR(64) NOT NULL COMMENT '模板ID',
    content TEXT COMMENT '发送内容JSON',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING-待发送, SUCCESS-成功, FAILED-失败',
    error_code VARCHAR(20) COMMENT '错误码',
    error_msg VARCHAR(500) COMMENT '错误信息',
    msg_id VARCHAR(64) COMMENT '微信返回的消息ID',
    send_time DATETIME COMMENT '发送时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除标志',
    INDEX idx_business (business_type, business_id),
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='微信推送记录表';

-- 添加微信推送相关权限
INSERT INTO permissions (permission_code, permission_name, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT * FROM (
    SELECT 'wechat:push:view' as permission_code, '查看推送记录' as permission_name,
           '查看微信推送记录' as permission_desc, 1 as sort_order, 1 as status,
           NOW() as created_at, NOW() as updated_at, 0 as deleted
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'wechat:push:view');

INSERT INTO permissions (permission_code, permission_name, permission_desc, sort_order, status, created_at, updated_at, deleted)
SELECT * FROM (
    SELECT 'wechat:push:send' as permission_code, '发送推送' as permission_name,
           '发送微信模板消息推送' as permission_desc, 2 as sort_order, 1 as status,
           NOW() as created_at, NOW() as updated_at, 0 as deleted
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM permissions WHERE permission_code = 'wechat:push:send');
