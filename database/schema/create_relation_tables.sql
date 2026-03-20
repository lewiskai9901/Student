-- 创建关系表（用于已运行的数据库）
-- 这些表在 V8.2.0 ~ V8.2.2 迁移中定义但未执行

-- 1. 用户-组织关系表
CREATE TABLE IF NOT EXISTS user_org_relations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    org_unit_id BIGINT NOT NULL COMMENT '组织单元ID',
    relation_type VARCHAR(30) NOT NULL COMMENT '关系类型: PRIMARY, SECONDARY, TEMPORARY, SUPERVISING',
    position_title VARCHAR(100) DEFAULT NULL COMMENT '职务名称',
    position_level INT DEFAULT NULL COMMENT '职务级别',
    is_primary TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主归属',
    is_leader TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否领导',
    can_manage TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否有管理权限',
    can_approve TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否有审批权限',
    start_date DATE DEFAULT NULL COMMENT '开始日期',
    end_date DATE DEFAULT NULL COMMENT '结束日期',
    weight_ratio DECIMAL(5,2) DEFAULT 100.00 COMMENT '权重比例',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    KEY idx_user_id (user_id),
    KEY idx_org_unit_id (org_unit_id),
    KEY idx_relation_type (relation_type),
    KEY idx_is_primary (is_primary),
    KEY idx_is_leader (is_leader),
    UNIQUE KEY uk_user_org_primary (user_id, org_unit_id, relation_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-组织关系表';

-- 2. 用户组织关系历史表
CREATE TABLE IF NOT EXISTS user_org_relation_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    relation_id BIGINT NOT NULL COMMENT '关系ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    org_unit_id BIGINT NOT NULL COMMENT '组织单元ID',
    relation_type VARCHAR(30) NOT NULL COMMENT '关系类型',
    position_title VARCHAR(100) DEFAULT NULL COMMENT '职务名称',
    is_primary TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否主归属',
    is_leader TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否领导',
    action VARCHAR(20) NOT NULL COMMENT '操作: CREATE, UPDATE, DELETE',
    change_reason VARCHAR(500) DEFAULT NULL COMMENT '变更原因',
    changed_by BIGINT DEFAULT NULL COMMENT '操作人',
    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_relation_id (relation_id),
    KEY idx_user_id (user_id),
    KEY idx_changed_at (changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-组织关系历史表';

-- 3. 场所-组织关系表
CREATE TABLE IF NOT EXISTS place_org_relations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    place_id BIGINT NOT NULL COMMENT '场所ID',
    org_unit_id BIGINT NOT NULL COMMENT '组织单元ID',
    relation_type VARCHAR(32) NOT NULL DEFAULT 'PRIMARY' COMMENT '关系类型: PRIMARY, SHARED, MANAGED',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否主归属',
    priority_level INT DEFAULT 1 COMMENT '优先级',
    can_use BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否有使用权',
    can_manage BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否有管理权',
    can_assign BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否有分配权',
    can_inspect BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否有检查权',
    use_schedule JSON COMMENT '使用时间安排',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    allocated_capacity INT COMMENT '分配的容量',
    weight_ratio DECIMAL(5,2) DEFAULT 100.00 COMMENT '权重比例',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_place_id (place_id),
    INDEX idx_org_unit_id (org_unit_id),
    INDEX idx_relation_type (relation_type),
    INDEX idx_is_primary (is_primary),
    INDEX idx_deleted (deleted),
    UNIQUE KEY uk_place_org_type (place_id, org_unit_id, relation_type, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所-组织关系表';

-- 4. 场所组织关系历史表
CREATE TABLE IF NOT EXISTS place_org_relation_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    relation_id BIGINT NOT NULL COMMENT '关系ID',
    place_id BIGINT NOT NULL COMMENT '场所ID',
    org_unit_id BIGINT NOT NULL COMMENT '组织单元ID',
    relation_type VARCHAR(32) NOT NULL COMMENT '关系类型',
    action VARCHAR(32) NOT NULL COMMENT '操作类型: CREATE, UPDATE, DELETE',
    old_values JSON COMMENT '旧值',
    new_values JSON COMMENT '新值',
    operated_by BIGINT COMMENT '操作人',
    operated_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_relation_id (relation_id),
    INDEX idx_place_id (place_id),
    INDEX idx_org_unit_id (org_unit_id),
    INDEX idx_operated_at (operated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所-组织关系历史表';

-- 5. 用户-场所关系表
CREATE TABLE IF NOT EXISTS user_place_relations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    place_id BIGINT NOT NULL COMMENT '场所ID',
    relation_type VARCHAR(32) NOT NULL DEFAULT 'ASSIGNED' COMMENT '关系类型: ASSIGNED, MANAGED, TEMPORARY',
    position_code VARCHAR(50) COMMENT '位置编码',
    position_name VARCHAR(100) COMMENT '位置名称',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否主要场所',
    can_use BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否有使用权',
    can_manage BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否有管理权',
    start_date DATE COMMENT '开始日期',
    end_date DATE COMMENT '结束日期',
    fee_amount DECIMAL(10,2) COMMENT '费用金额',
    fee_paid BOOLEAN DEFAULT FALSE COMMENT '是否已缴费',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_user_id (user_id),
    INDEX idx_place_id (place_id),
    INDEX idx_relation_type (relation_type),
    INDEX idx_position_code (position_code),
    INDEX idx_is_primary (is_primary),
    INDEX idx_deleted (deleted),
    UNIQUE KEY uk_user_place_type (user_id, place_id, relation_type, deleted),
    UNIQUE KEY uk_place_position (place_id, position_code, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-场所关系表';

-- 6. 用户场所关系历史表
CREATE TABLE IF NOT EXISTS user_place_relation_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    relation_id BIGINT NOT NULL COMMENT '关系ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    place_id BIGINT NOT NULL COMMENT '场所ID',
    relation_type VARCHAR(32) NOT NULL COMMENT '关系类型',
    action VARCHAR(32) NOT NULL COMMENT '操作类型: CREATE, UPDATE, DELETE, CHECK_IN, CHECK_OUT',
    old_values JSON COMMENT '旧值',
    new_values JSON COMMENT '新值',
    operated_by BIGINT COMMENT '操作人',
    operated_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_relation_id (relation_id),
    INDEX idx_user_id (user_id),
    INDEX idx_place_id (place_id),
    INDEX idx_operated_at (operated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户-场所关系历史表';
