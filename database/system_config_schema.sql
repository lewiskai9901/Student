-- 系统配置表
CREATE TABLE IF NOT EXISTS system_configs (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键(唯一)',
    config_value TEXT NOT NULL COMMENT '配置值',
    config_type VARCHAR(20) NOT NULL DEFAULT 'string' COMMENT '值类型(string/number/boolean/json)',
    config_group VARCHAR(50) NOT NULL COMMENT '配置分组(system/business/ui等)',
    config_label VARCHAR(100) NOT NULL COMMENT '配置标签(中文名称)',
    config_desc VARCHAR(500) COMMENT '配置描述',
    is_system TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统内置(0-否,1-是,系统内置不可删除)',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态(1-启用,0-禁用)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志(0-未删除,1-已删除)',

    INDEX idx_config_key (config_key),
    INDEX idx_config_group (config_group),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 插入默认配置
INSERT INTO system_configs (id, config_key, config_value, config_type, config_group, config_label, config_desc, is_system, sort_order, status) VALUES
-- 系统基础配置
(1, 'system.name', '学生管理系统', 'string', 'system', '系统名称', '系统显示名称', 1, 1, 1),
(2, 'system.version', '1.0.0', 'string', 'system', '系统版本', '当前版本号', 1, 2, 1),
(3, 'system.logo', '/logo.png', 'string', 'system', '系统Logo', 'Logo图片路径', 1, 3, 1),
(4, 'system.copyright', 'Copyright © 2025 学生管理系统', 'string', 'system', '版权信息', '页脚版权', 1, 4, 1),

-- 业务配置
(10, 'business.max_file_size', '10', 'number', 'business', '最大文件大小', '文件上传最大大小(MB)', 0, 10, 1),
(11, 'business.allowed_file_types', 'jpg,jpeg,png,pdf,doc,docx,xls,xlsx', 'string', 'business', '允许的文件类型', '允许上传的文件扩展名', 0, 11, 1),
(12, 'business.password_min_length', '6', 'number', 'business', '密码最小长度', '用户密码最小长度', 0, 12, 1),
(13, 'business.session_timeout', '30', 'number', 'business', '会话超时时间', '会话超时时间(分钟)', 0, 13, 1),

-- UI配置
(20, 'ui.theme_color', '#409EFF', 'string', 'ui', '主题色', '系统主题颜色', 0, 20, 1),
(21, 'ui.page_size_options', '10,20,50,100', 'string', 'ui', '分页选项', '分页大小选项', 0, 21, 1),
(22, 'ui.default_page_size', '10', 'number', 'ui', '默认分页大小', '默认每页数量', 0, 22, 1),
(23, 'ui.show_breadcrumb', 'true', 'boolean', 'ui', '显示面包屑', '是否显示面包屑', 0, 23, 1)
ON DUPLICATE KEY UPDATE
    config_value = VALUES(config_value),
    updated_at = NOW();

-- 系统配置权限
INSERT INTO permissions (id, permission_code, permission_name, resource_type, parent_id, created_at, updated_at, deleted)
VALUES
    (1000010, 'system:config', '系统配置', 1, NULL, NOW(), NOW(), 0),
    (1000011, 'system:config:view', '查看系统配置', 2, 1000010, NOW(), NOW(), 0),
    (1000012, 'system:config:edit', '编辑系统配置', 2, 1000010, NOW(), NOW(), 0),
    (1000013, 'system:config:delete', '删除系统配置', 2, 1000010, NOW(), NOW(), 0),
    (1000014, 'system:config:add', '添加系统配置', 2, 1000010, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    updated_at = NOW();

-- 给超级管理员角色添加系统配置权限
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT 1, id, NOW()
FROM permissions
WHERE permission_code LIKE 'system:config%'
ON DUPLICATE KEY UPDATE created_at = NOW();
