-- ============================================
-- Phase 4: 自动排班引擎 + 数据分析中心
-- Version: V20260127_4
-- Date: 2026-01-27
-- ============================================

-- 1. 排班策略表
CREATE TABLE IF NOT EXISTS schedule_policies (
    id BIGINT PRIMARY KEY,
    policy_code VARCHAR(50) NOT NULL UNIQUE COMMENT '策略编码',
    policy_name VARCHAR(100) NOT NULL COMMENT '策略名称',
    policy_type VARCHAR(30) NOT NULL COMMENT '策略类型: DAILY/WEEKLY/CUSTOM',
    rotation_algorithm VARCHAR(30) NOT NULL COMMENT '轮询算法: ROUND_ROBIN/RANDOM/LOAD_BALANCED',
    template_id BIGINT COMMENT '关联检查模板ID',
    inspector_pool JSON COMMENT '检查员ID列表',
    schedule_config JSON COMMENT '排班配置(cron/星期/日期)',
    excluded_dates JSON COMMENT '排除日期(节假日等)',
    is_enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_by BIGINT COMMENT '创建人ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_policy_code (policy_code),
    INDEX idx_template_id (template_id),
    INDEX idx_is_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班策略表';

-- 2. 排班执行记录表
CREATE TABLE IF NOT EXISTS schedule_executions (
    id BIGINT PRIMARY KEY,
    policy_id BIGINT NOT NULL COMMENT '策略ID',
    execution_date DATE NOT NULL COMMENT '执行日期',
    assigned_inspectors JSON COMMENT '分配的检查员ID列表',
    session_id BIGINT COMMENT '关联检查会话ID',
    status VARCHAR(20) NOT NULL DEFAULT 'PLANNED' COMMENT '状态: PLANNED/EXECUTED/SKIPPED/FAILED',
    failure_reason VARCHAR(500) COMMENT '失败原因',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_policy_date (policy_id, execution_date),
    INDEX idx_execution_date (execution_date),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班执行记录表';

-- 3. 分析快照表 (CQRS读模型缓存)
CREATE TABLE IF NOT EXISTS analytics_snapshots (
    id BIGINT PRIMARY KEY,
    snapshot_type VARCHAR(50) NOT NULL COMMENT '快照类型: CLASS_RANKING/DEPARTMENT_TREND/INSPECTOR_WORKLOAD/VIOLATION_DISTRIBUTION',
    snapshot_scope VARCHAR(50) COMMENT '作用域标识(如department_id, class_id)',
    scope_id BIGINT COMMENT '作用域ID',
    snapshot_date DATE NOT NULL COMMENT '快照日期',
    data_json JSON NOT NULL COMMENT '快照数据',
    generated_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    INDEX idx_type_date (snapshot_type, snapshot_date),
    INDEX idx_scope (snapshot_type, snapshot_scope, scope_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析快照表';

-- 添加排班权限
INSERT IGNORE INTO permissions (permission_name, permission_code, parent_id, permission_type, status, created_at) VALUES
('排班管理', 'schedule:policy:view', NULL, 'menu', 1, NOW()),
('排班策略管理', 'schedule:policy:manage', NULL, 'button', 1, NOW()),
('数据分析', 'analytics:view', NULL, 'menu', 1, NOW()),
('检查导出', 'inspection:export:view', NULL, 'menu', 1, NOW()),
('创建导出', 'inspection:export:create', NULL, 'button', 1, NOW());

-- 为管理员角色添加排班和分析权限
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT 1, id FROM permissions WHERE permission_code IN (
    'schedule:policy:view', 'schedule:policy:manage',
    'analytics:view',
    'inspection:export:view', 'inspection:export:create'
);
