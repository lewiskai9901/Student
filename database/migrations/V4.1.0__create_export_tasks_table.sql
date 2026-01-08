-- ============================================
-- 导出中心 - 异步导出任务表
-- Version: 4.1.0
-- Date: 2024-12-20
-- ============================================

-- 导出任务表
CREATE TABLE IF NOT EXISTS export_tasks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_code VARCHAR(50) NOT NULL COMMENT '任务编号',
    export_type VARCHAR(50) NOT NULL COMMENT '导出类型：realtime_deduction/rating_report/statistics',
    export_format VARCHAR(20) NOT NULL DEFAULT 'EXCEL' COMMENT '导出格式：EXCEL/WORD/PDF',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/COMPLETED/FAILED/CANCELLED',
    progress INT DEFAULT 0 COMMENT '进度百分比(0-100)',
    total_count INT DEFAULT 0 COMMENT '总记录数',
    processed_count INT DEFAULT 0 COMMENT '已处理记录数',
    file_name VARCHAR(200) COMMENT '文件名',
    file_path VARCHAR(500) COMMENT '文件存储路径',
    file_size BIGINT COMMENT '文件大小(字节)',
    download_url VARCHAR(500) COMMENT '下载链接',
    expire_time DATETIME COMMENT '链接过期时间',
    request_data TEXT COMMENT '请求参数(JSON)',
    error_message VARCHAR(1000) COMMENT '错误信息',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    completed_at DATETIME COMMENT '完成时间',

    INDEX idx_task_code (task_code),
    INDEX idx_created_by (created_by),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出任务表';

-- 添加导出权限
INSERT IGNORE INTO permissions (permission_name, permission_code, parent_id, permission_type, status, created_at) VALUES
('导出管理', 'check:export', NULL, 'menu', 1, NOW()),
('导出预览', 'check:export:preview', NULL, 'button', 1, NOW()),
('同步导出', 'check:export:sync', NULL, 'button', 1, NOW()),
('异步导出', 'check:export:async', NULL, 'button', 1, NOW()),
('下载文件', 'check:export:download', NULL, 'button', 1, NOW());

-- 为管理员角色添加导出权限
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions WHERE permission_code LIKE 'check:export%';

-- 为德育干事角色添加导出权限
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.role_code = 'moral_education_officer'
AND p.permission_code LIKE 'check:export%';
