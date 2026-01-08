-- 公告通知表
CREATE TABLE IF NOT EXISTS announcements (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT NOT NULL COMMENT '公告内容',
    announcement_type VARCHAR(20) NOT NULL DEFAULT 'notice' COMMENT '公告类型(notice-通知,announcement-公告,warning-警告)',
    priority INT NOT NULL DEFAULT 1 COMMENT '优先级(1-普通,2-重要,3-紧急)',
    publisher_id BIGINT NOT NULL COMMENT '发布人ID',
    publisher_name VARCHAR(50) NOT NULL COMMENT '发布人姓名',
    publish_time DATETIME COMMENT '发布时间',
    start_time DATETIME COMMENT '生效开始时间',
    end_time DATETIME COMMENT '生效结束时间',
    target_type VARCHAR(20) NOT NULL DEFAULT 'all' COMMENT '目标类型(all-全体,role-角色,user-指定用户)',
    target_ids TEXT COMMENT '目标ID列表(JSON数组)',
    attachment_url VARCHAR(500) COMMENT '附件URL',
    is_published TINYINT NOT NULL DEFAULT 0 COMMENT '是否已发布(0-草稿,1-已发布)',
    is_pinned TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶(0-否,1-是)',
    view_count INT DEFAULT 0 COMMENT '浏览次数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志(0-未删除,1-已删除)',

    INDEX idx_publisher_id (publisher_id),
    INDEX idx_announcement_type (announcement_type),
    INDEX idx_is_published (is_published),
    INDEX idx_is_pinned (is_pinned),
    INDEX idx_publish_time (publish_time),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告通知表';

-- 公告阅读记录表
CREATE TABLE IF NOT EXISTS announcement_reads (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    announcement_id BIGINT NOT NULL COMMENT '公告ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    read_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    UNIQUE KEY uk_announcement_user (announcement_id, user_id),
    INDEX idx_user_id (user_id),
    INDEX idx_announcement_id (announcement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告阅读记录表';

-- 公告权限
INSERT INTO permissions (id, permission_code, permission_name, resource_type, parent_id, created_at, updated_at, deleted)
VALUES
    (1000020, 'system:announcement', '公告管理', 1, NULL, NOW(), NOW(), 0),
    (1000021, 'system:announcement:view', '查看公告', 2, 1000020, NOW(), NOW(), 0),
    (1000022, 'system:announcement:add', '发布公告', 2, 1000020, NOW(), NOW(), 0),
    (1000023, 'system:announcement:edit', '编辑公告', 2, 1000020, NOW(), NOW(), 0),
    (1000024, 'system:announcement:delete', '删除公告', 2, 1000020, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    updated_at = NOW();

-- 给超级管理员角色添加公告权限
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT 1, id, NOW()
FROM permissions
WHERE permission_code LIKE 'system:announcement%'
ON DUPLICATE KEY UPDATE created_at = NOW();
