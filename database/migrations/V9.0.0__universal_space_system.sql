-- =====================================================
-- V9.0.0 通用空间管理系统
-- 设计目标：适用于学校、医院、公司、政府、工厂等各类场景
-- =====================================================

-- 1. 空间类型配置表
CREATE TABLE IF NOT EXISTS space_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 基础信息
    type_code VARCHAR(50) NOT NULL COMMENT '类型编码',
    type_name VARCHAR(100) NOT NULL COMMENT '类型名称',
    icon VARCHAR(50) COMMENT '图标',
    description VARCHAR(500) COMMENT '描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_system BOOLEAN DEFAULT FALSE COMMENT '是否系统预置',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',

    -- 层级关系
    allowed_child_types JSON COMMENT '允许的子类型编码列表',
    is_root_type BOOLEAN DEFAULT FALSE COMMENT '是否可作为根节点',

    -- 行为特性
    has_capacity BOOLEAN DEFAULT FALSE COMMENT '是否有容量',
    bookable BOOLEAN DEFAULT FALSE COMMENT '是否可预订',
    assignable BOOLEAN DEFAULT FALSE COMMENT '是否可分配给组织',
    occupiable BOOLEAN DEFAULT FALSE COMMENT '是否可入住/占用',

    -- 容量配置
    capacity_unit VARCHAR(20) COMMENT '容量单位(人/床位/工位/平方米)',
    default_capacity INT COMMENT '默认容量',

    -- 扩展属性
    attribute_schema JSON COMMENT '扩展属性JSON Schema',

    -- 审计字段
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    UNIQUE KEY uk_type_code (type_code),
    INDEX idx_is_root (is_root_type),
    INDEX idx_enabled (is_enabled),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间类型配置表';

-- 2. 空间实例表
CREATE TABLE IF NOT EXISTS spaces (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 基础信息
    space_code VARCHAR(50) NOT NULL COMMENT '空间编码',
    space_name VARCHAR(100) NOT NULL COMMENT '空间名称',
    type_code VARCHAR(50) NOT NULL COMMENT '空间类型编码',
    description VARCHAR(500) COMMENT '描述',

    -- 层级关系
    parent_id BIGINT COMMENT '父级ID',
    path VARCHAR(500) COMMENT '物化路径',
    level INT DEFAULT 0 COMMENT '层级深度',

    -- 容量（当类型has_capacity=true时使用）
    capacity INT COMMENT '容量',
    current_occupancy INT DEFAULT 0 COMMENT '当前占用数',

    -- 归属（当类型assignable=true时使用）
    org_unit_id BIGINT COMMENT '所属组织单元ID',
    responsible_user_id BIGINT COMMENT '负责人ID',

    -- 状态
    status TINYINT DEFAULT 1 COMMENT '状态: 0-停用 1-正常 2-维护中',

    -- 扩展属性
    attributes JSON COMMENT '扩展属性值',

    -- 审计字段
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    UNIQUE KEY uk_space_code (space_code),
    INDEX idx_type_code (type_code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_path (path(255)),
    INDEX idx_org_unit (org_unit_id),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间实例表';

-- 3. 空间占用记录表
CREATE TABLE IF NOT EXISTS space_occupants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    space_id BIGINT NOT NULL COMMENT '空间ID',
    occupant_type VARCHAR(20) NOT NULL COMMENT '占用者类型',
    occupant_id BIGINT NOT NULL COMMENT '占用者ID',
    occupant_name VARCHAR(100) COMMENT '占用者名称(冗余)',
    position_no VARCHAR(20) COMMENT '位置号(床位号/工位号)',

    check_in_time DATETIME NOT NULL COMMENT '入住时间',
    check_out_time DATETIME COMMENT '退出时间',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-已退出 1-在住',

    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    INDEX idx_space_id (space_id),
    INDEX idx_occupant (occupant_type, occupant_id),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间占用记录表';

-- 4. 空间预订记录表
CREATE TABLE IF NOT EXISTS space_bookings (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    space_id BIGINT NOT NULL COMMENT '空间ID',
    booker_id BIGINT NOT NULL COMMENT '预订人ID',
    booker_name VARCHAR(100) COMMENT '预订人名称(冗余)',
    title VARCHAR(200) COMMENT '预订标题',

    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',

    attendees JSON COMMENT '参与人列表',
    status TINYINT DEFAULT 1 COMMENT '状态: 0-已取消 1-待使用 2-使用中 3-已完成',

    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    INDEX idx_space_id (space_id),
    INDEX idx_booker (booker_id),
    INDEX idx_time_range (start_time, end_time),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间预订记录表';

-- 5. 空间分配记录表（空间与组织的多对多关系）
CREATE TABLE IF NOT EXISTS space_assignments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    space_id BIGINT NOT NULL COMMENT '空间ID',
    org_unit_id BIGINT NOT NULL COMMENT '组织单元ID',
    assignment_type VARCHAR(20) DEFAULT 'PRIMARY' COMMENT '分配类型: PRIMARY-主要 SHARED-共享',

    allocated_capacity INT COMMENT '分配的容量',
    priority INT DEFAULT 0 COMMENT '优先级',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',

    remark VARCHAR(500) COMMENT '备注',
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    UNIQUE KEY uk_space_org (space_id, org_unit_id, deleted),
    INDEX idx_space_id (space_id),
    INDEX idx_org_unit (org_unit_id),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间分配记录表';

-- =====================================================
-- 预置数据：通用空间类型（用户可自定义扩展）
-- =====================================================

INSERT INTO space_types (type_code, type_name, icon, description, is_root_type, allowed_child_types,
                         has_capacity, bookable, assignable, occupiable,
                         capacity_unit, is_system, sort_order) VALUES
-- 根节点类型
('SITE', '场地', 'MapPin', '顶级场地/园区/院区', TRUE, '["BUILDING"]',
 FALSE, FALSE, FALSE, FALSE, NULL, TRUE, 1),

-- 建筑类型
('BUILDING', '建筑', 'Building', '楼宇/建筑物', FALSE, '["FLOOR"]',
 FALSE, FALSE, TRUE, FALSE, NULL, TRUE, 2),

-- 楼层类型
('FLOOR', '楼层', 'Layers', '楼层', FALSE, '["ROOM","AREA"]',
 FALSE, FALSE, TRUE, FALSE, NULL, TRUE, 3),

-- 房间类型（通用，可容纳、可预订、可分配）
('ROOM', '房间', 'DoorOpen', '通用房间', FALSE, '[]',
 TRUE, TRUE, TRUE, FALSE, '人', TRUE, 4),

-- 区域类型（开放空间，可包含工位）
('AREA', '区域', 'Grid3x3', '开放区域/工位区', FALSE, '["STATION"]',
 TRUE, FALSE, TRUE, FALSE, '平方米', TRUE, 5),

-- 工位类型（最小单元，可预订、可分配、可占用）
('STATION', '工位', 'Armchair', '工位/座位', FALSE, '[]',
 FALSE, TRUE, TRUE, TRUE, NULL, TRUE, 6);

-- =====================================================
-- 权限数据
-- =====================================================

-- 空间类型管理权限
INSERT INTO permission (id, code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p2),
    'system:space-type',
    '空间类型管理',
    'menu',
    (SELECT id FROM permission p3 WHERE code = 'system' LIMIT 1),
    CONCAT((SELECT path FROM permission p4 WHERE code = 'system' LIMIT 1), (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p5), '/'),
    50,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'system:space-type');

INSERT INTO permission (id, code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p2),
    'system:space-type:view',
    '查看空间类型',
    'button',
    (SELECT id FROM permission p3 WHERE code = 'system:space-type' LIMIT 1),
    CONCAT((SELECT path FROM permission p4 WHERE code = 'system:space-type' LIMIT 1), (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p5), '/'),
    1,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'system:space-type:view');

INSERT INTO permission (id, code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p2),
    'system:space-type:add',
    '新增空间类型',
    'button',
    (SELECT id FROM permission p3 WHERE code = 'system:space-type' LIMIT 1),
    CONCAT((SELECT path FROM permission p4 WHERE code = 'system:space-type' LIMIT 1), (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p5), '/'),
    2,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'system:space-type:add');

INSERT INTO permission (id, code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p2),
    'system:space-type:edit',
    '编辑空间类型',
    'button',
    (SELECT id FROM permission p3 WHERE code = 'system:space-type' LIMIT 1),
    CONCAT((SELECT path FROM permission p4 WHERE code = 'system:space-type' LIMIT 1), (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p5), '/'),
    3,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'system:space-type:edit');

INSERT INTO permission (id, code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p2),
    'system:space-type:delete',
    '删除空间类型',
    'button',
    (SELECT id FROM permission p3 WHERE code = 'system:space-type' LIMIT 1),
    CONCAT((SELECT path FROM permission p4 WHERE code = 'system:space-type' LIMIT 1), (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p5), '/'),
    4,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'system:space-type:delete');

-- 空间管理权限
INSERT INTO permission (id, code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p2),
    'space',
    '空间管理',
    'menu',
    NULL,
    CONCAT('/', (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p5), '/'),
    30,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'space');

INSERT INTO permission (id, code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p2),
    'space:view',
    '查看空间',
    'button',
    (SELECT id FROM permission p3 WHERE code = 'space' LIMIT 1),
    CONCAT((SELECT path FROM permission p4 WHERE code = 'space' LIMIT 1), (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p5), '/'),
    1,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'space:view');

INSERT INTO permission (id, code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p2),
    'space:add',
    '新增空间',
    'button',
    (SELECT id FROM permission p3 WHERE code = 'space' LIMIT 1),
    CONCAT((SELECT path FROM permission p4 WHERE code = 'space' LIMIT 1), (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p5), '/'),
    2,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'space:add');

INSERT INTO permission (id, code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p2),
    'space:edit',
    '编辑空间',
    'button',
    (SELECT id FROM permission p3 WHERE code = 'space' LIMIT 1),
    CONCAT((SELECT path FROM permission p4 WHERE code = 'space' LIMIT 1), (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p5), '/'),
    3,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'space:edit');

INSERT INTO permission (id, code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p2),
    'space:delete',
    '删除空间',
    'button',
    (SELECT id FROM permission p3 WHERE code = 'space' LIMIT 1),
    CONCAT((SELECT path FROM permission p4 WHERE code = 'space' LIMIT 1), (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p5), '/'),
    4,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'space:delete');

INSERT INTO permission (id, code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p2),
    'space:assign',
    '分配空间',
    'button',
    (SELECT id FROM permission p3 WHERE code = 'space' LIMIT 1),
    CONCAT((SELECT path FROM permission p4 WHERE code = 'space' LIMIT 1), (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p5), '/'),
    5,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'space:assign');

INSERT INTO permission (id, code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p2),
    'space:booking',
    '空间预订',
    'button',
    (SELECT id FROM permission p3 WHERE code = 'space' LIMIT 1),
    CONCAT((SELECT path FROM permission p4 WHERE code = 'space' LIMIT 1), (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p5), '/'),
    6,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'space:booking');

INSERT INTO permission (id, code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p2),
    'space:occupancy',
    '入住管理',
    'button',
    (SELECT id FROM permission p3 WHERE code = 'space' LIMIT 1),
    CONCAT((SELECT path FROM permission p4 WHERE code = 'space' LIMIT 1), (SELECT COALESCE(MAX(id), 0) + 1 FROM permission p5), '/'),
    7,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'space:occupancy');
