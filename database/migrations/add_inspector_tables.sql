-- 检查计划打分人员相关表
-- 执行时间: 2025-12-16
-- 功能: 为检查计划添加打分人员配置功能

-- 1. 检查计划打分人员表
CREATE TABLE IF NOT EXISTS check_plan_inspectors (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    plan_id BIGINT NOT NULL COMMENT '检查计划ID',
    user_id BIGINT NOT NULL COMMENT '打分人员用户ID',
    status TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    remark VARCHAR(255) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    created_by BIGINT COMMENT '创建人ID',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '是否删除：0否 1是',
    UNIQUE KEY uk_plan_user (plan_id, user_id, deleted),
    INDEX idx_user (user_id),
    INDEX idx_plan (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查计划打分人员';

-- 2. 打分人员权限配置表（一个人员可以有多个类别权限配置）
CREATE TABLE IF NOT EXISTS inspector_permissions (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    inspector_id BIGINT NOT NULL COMMENT '打分人员ID（关联check_plan_inspectors）',
    plan_id BIGINT NOT NULL COMMENT '检查计划ID（冗余，便于查询）',
    user_id BIGINT NOT NULL COMMENT '用户ID（冗余，便于查询）',
    category_id VARCHAR(64) NOT NULL COMMENT '检查类别ID',
    category_name VARCHAR(100) COMMENT '检查类别名称（冗余）',
    class_ids JSON COMMENT '可检查的班级ID列表，NULL表示计划范围内全部',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_inspector (inspector_id),
    INDEX idx_plan_user (plan_id, user_id),
    INDEX idx_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打分人员权限配置';

-- 3. 检查任务分配表（日常检查创建时自动生成）
CREATE TABLE IF NOT EXISTS check_task_assignments (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    daily_check_id BIGINT NOT NULL COMMENT '日常检查ID',
    plan_id BIGINT NOT NULL COMMENT '检查计划ID',
    user_id BIGINT NOT NULL COMMENT '被分配的打分人员ID',
    category_ids JSON COMMENT '分配的检查类别ID列表',
    class_ids JSON COMMENT '分配的班级ID列表',
    status TINYINT DEFAULT 0 COMMENT '任务状态：0待处理 1进行中 2已完成',
    notified TINYINT DEFAULT 0 COMMENT '是否已通知：0否 1是',
    notified_at DATETIME COMMENT '通知时间',
    started_at DATETIME COMMENT '开始检查时间',
    completed_at DATETIME COMMENT '完成时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_status (user_id, status),
    INDEX idx_daily_check (daily_check_id),
    INDEX idx_plan (plan_id),
    UNIQUE KEY uk_check_user (daily_check_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查任务分配';
