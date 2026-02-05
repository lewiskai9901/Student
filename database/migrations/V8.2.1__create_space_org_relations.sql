-- =============================================
-- V8.2.1: 创建场所-组织关系表
-- 支持场所的多组织归属（如共用教室、共管宿舍等）
-- =============================================

-- 场所-组织关系表
CREATE TABLE IF NOT EXISTS space_org_relations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    space_id BIGINT NOT NULL COMMENT '场所ID',
    org_unit_id BIGINT NOT NULL COMMENT '组织单元ID',
    relation_type VARCHAR(32) NOT NULL DEFAULT 'PRIMARY' COMMENT '关系类型: PRIMARY-主归属, SHARED-共用, MANAGED-托管',

    -- 归属属性
    is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否主归属',
    priority_level INT DEFAULT 1 COMMENT '优先级（多组织共用时的优先顺序）',

    -- 使用权限
    can_use BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否有使用权',
    can_manage BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否有管理权',
    can_assign BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否有分配权（可分配座位/床位）',
    can_inspect BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否有检查权',

    -- 时间安排（共用场所的使用时段）
    use_schedule JSON COMMENT '使用时间安排（周几、时段等）',

    -- 有效期
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',

    -- 容量分配（共用场所的容量分配）
    allocated_capacity INT COMMENT '分配的容量（座位/床位数）',

    -- 权重
    weight_ratio DECIMAL(5,2) DEFAULT 100.00 COMMENT '权重比例',

    -- 排序和备注
    sort_order INT DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(500) COMMENT '备注',

    -- 审计字段
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除标记',

    -- 索引
    INDEX idx_space_id (space_id),
    INDEX idx_org_unit_id (org_unit_id),
    INDEX idx_relation_type (relation_type),
    INDEX idx_is_primary (is_primary),
    INDEX idx_deleted (deleted),

    -- 唯一约束：同一场所同一组织同一类型只能有一条记录
    UNIQUE KEY uk_space_org_type (space_id, org_unit_id, relation_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所-组织关系表';

-- 场所组织关系历史表（审计用）
CREATE TABLE IF NOT EXISTS space_org_relation_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    relation_id BIGINT NOT NULL COMMENT '关系ID',
    space_id BIGINT NOT NULL COMMENT '场所ID',
    org_unit_id BIGINT NOT NULL COMMENT '组织单元ID',
    relation_type VARCHAR(32) NOT NULL COMMENT '关系类型',
    action VARCHAR(32) NOT NULL COMMENT '操作类型: CREATE, UPDATE, DELETE',
    old_values JSON COMMENT '旧值',
    new_values JSON COMMENT '新值',
    operated_by BIGINT COMMENT '操作人',
    operated_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',

    INDEX idx_relation_id (relation_id),
    INDEX idx_space_id (space_id),
    INDEX idx_org_unit_id (org_unit_id),
    INDEX idx_operated_at (operated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所-组织关系历史表';

-- =============================================
-- 数据迁移：从现有空间表迁移组织归属关系
-- =============================================

-- 从 spaces 表迁移主归属关系（如果存在 org_unit_id 字段）
-- 注意：需要根据实际的 spaces 表结构调整
INSERT INTO space_org_relations (space_id, org_unit_id, relation_type, is_primary, can_use, can_manage, can_assign, can_inspect, weight_ratio, created_by)
SELECT
    id,
    org_unit_id,
    'PRIMARY',
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    TRUE,
    100.00,
    1
FROM spaces
WHERE org_unit_id IS NOT NULL AND deleted = 0
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 从 dormitories 表迁移（如果存在且有归属信息）
-- INSERT INTO space_org_relations (space_id, org_unit_id, relation_type, is_primary, ...)
-- SELECT ... FROM dormitories WHERE ...

-- 从 classrooms 表迁移（如果存在且有归属信息）
-- INSERT INTO space_org_relations (space_id, org_unit_id, relation_type, is_primary, ...)
-- SELECT ... FROM classrooms WHERE ...
