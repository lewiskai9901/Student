-- 操作日志表
CREATE TABLE IF NOT EXISTS operation_logs (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '操作用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    real_name VARCHAR(50) COMMENT '真实姓名',

    operation_module VARCHAR(50) NOT NULL COMMENT '操作模块(student/class/dormitory/quantification/system等)',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型(create/update/delete/query/export/login/logout等)',
    operation_name VARCHAR(100) NOT NULL COMMENT '操作名称(添加学生/修改班级/删除宿舍等)',

    request_method VARCHAR(10) COMMENT '请求方法(GET/POST/PUT/DELETE)',
    request_url VARCHAR(500) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数(JSON格式)',

    response_status INT COMMENT '响应状态码',
    response_time INT COMMENT '响应时间(毫秒)',
    error_message TEXT COMMENT '错误信息',

    ip_address VARCHAR(50) COMMENT 'IP地址',
    user_agent VARCHAR(500) COMMENT '用户代理',
    browser VARCHAR(50) COMMENT '浏览器',
    os VARCHAR(50) COMMENT '操作系统',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志(0-未删除,1-已删除)',

    INDEX idx_user_id (user_id),
    INDEX idx_username (username),
    INDEX idx_operation_module (operation_module),
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 操作日志权限
INSERT INTO permissions (id, permission_code, permission_name, resource_type, parent_id, created_at, updated_at, deleted)
VALUES
    (1000001, 'system:operlog', '操作日志', 1, NULL, NOW(), NOW(), 0),
    (1000002, 'system:operlog:view', '查看操作日志', 2, 1000001, NOW(), NOW(), 0),
    (1000003, 'system:operlog:delete', '删除操作日志', 2, 1000001, NOW(), NOW(), 0),
    (1000004, 'system:operlog:export', '导出操作日志', 2, 1000001, NOW(), NOW(), 0),
    (1000005, 'system:operlog:clear', '清空操作日志', 2, 1000001, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE
    permission_name = VALUES(permission_name),
    updated_at = NOW();

-- 给超级管理员角色(role_id=1)添加操作日志权限
INSERT INTO role_permissions (role_id, permission_id, created_at)
SELECT 1, id, NOW()
FROM permissions
WHERE permission_code LIKE 'system:operlog%'
ON DUPLICATE KEY UPDATE created_at = NOW();
