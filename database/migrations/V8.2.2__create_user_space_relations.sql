-- =============================================
-- V8.2.2: 创建用户-场所关系表
-- 支持用户的场所分配（如学生宿舍床位、教师办公室等）
-- =============================================

-- 用户-场所关系表
CREATE TABLE IF NOT EXISTS user_space_relations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    space_id BIGINT NOT NULL COMMENT '场所ID',
    relation_type VARCHAR(32) NOT NULL DEFAULT 'ASSIGNED' COMMENT '关系类型: ASSIGNED-分配, MANAGED-管理, TEMPORARY-临时',

    -- 分配信息
    position_code VARCHAR(50) COMMENT '位置编码（如床位号、座位号）',
    position_name VARCHAR(100) COMMENT '位置名称',

    -- 归属属性
    is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否主要场所',

    -- 使用权限
    can_use BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否有使用权',
    can_manage BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否有管理权',

    -- 有效期
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',

    -- 费用相关（如宿舍费用）
    fee_amount DECIMAL(10,2) COMMENT '费用金额',
    fee_paid BOOLEAN DEFAULT FALSE COMMENT '是否已缴费',

    -- 排序和备注
    sort_order INT DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(500) COMMENT '备注',

    -- 审计字段
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除标记',

    -- 索引
    INDEX idx_user_id (user_id),
    INDEX idx_space_id (space_id),
    INDEX idx_relation_type (relation_type),
    INDEX idx_position_code (position_code),
    INDEX idx_is_primary (is_primary),
    INDEX idx_deleted (deleted),

    -- 唯一约束：同一用户同一场所同一类型只能有一条记录
    UNIQUE KEY uk_user_space_type (user_id, space_id, relation_type, deleted),
    -- 位置唯一约束：同一场所的同一位置只能分配给一个用户
    UNIQUE KEY uk_space_position (space_id, position_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-场所关系表';

-- 用户场所关系历史表（审计用）
CREATE TABLE IF NOT EXISTS user_space_relation_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    relation_id BIGINT NOT NULL COMMENT '关系ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    space_id BIGINT NOT NULL COMMENT '场所ID',
    relation_type VARCHAR(32) NOT NULL COMMENT '关系类型',
    action VARCHAR(32) NOT NULL COMMENT '操作类型: CREATE, UPDATE, DELETE, CHECK_IN, CHECK_OUT',
    old_values JSON COMMENT '旧值',
    new_values JSON COMMENT '新值',
    operated_by BIGINT COMMENT '操作人',
    operated_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    INDEX idx_relation_id (relation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_space_id (space_id),
    INDEX idx_operated_at (operated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-场所关系历史表';

-- =============================================
-- 数据迁移：从现有宿舍分配表迁移
-- =============================================

-- 从学生宿舍分配表迁移（如果存在）
-- 注意：需要根据实际的表结构调整
-- INSERT INTO user_space_relations (user_id, space_id, relation_type, position_code, position_name, is_primary, ...)
-- SELECT student_id, dormitory_id, 'ASSIGNED', bed_number, CONCAT('床位', bed_number), TRUE, ...
-- FROM student_dormitory_assignments WHERE deleted = 0;

-- 从教师办公室分配表迁移（如果存在）
-- INSERT INTO user_space_relations (user_id, space_id, relation_type, is_primary, ...)
-- SELECT teacher_id, office_id, 'ASSIGNED', TRUE, ...
-- FROM teacher_office_assignments WHERE deleted = 0;
