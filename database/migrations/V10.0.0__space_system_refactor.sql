-- =====================================================
-- V10.0.0 空间管理系统重构
-- 设计理念：固定层级 + 可配置分类
-- =====================================================

-- =====================================================
-- 1. 分类配置表（替代原 space_types）
-- =====================================================
CREATE TABLE IF NOT EXISTS space_categories (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 基础信息
    category_code VARCHAR(50) NOT NULL COMMENT '分类编码',
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    apply_to_level ENUM('BUILDING', 'ROOM') NOT NULL COMMENT '适用层级：楼栋或房间',
    icon VARCHAR(50) COMMENT '图标',
    color VARCHAR(20) COMMENT '颜色',
    description VARCHAR(500) COMMENT '描述',

    -- 行为特性
    has_capacity BOOLEAN DEFAULT FALSE COMMENT '是否有容量',
    capacity_unit VARCHAR(20) COMMENT '容量单位：人/床位/座位',
    default_capacity INT COMMENT '默认容量',
    bookable BOOLEAN DEFAULT FALSE COMMENT '是否可预订',
    assignable BOOLEAN DEFAULT FALSE COMMENT '是否可分配给组织/班级',
    occupiable BOOLEAN DEFAULT FALSE COMMENT '是否可入住',
    has_gender BOOLEAN DEFAULT FALSE COMMENT '是否区分性别（宿舍用）',

    -- 系统字段
    is_system BOOLEAN DEFAULT FALSE COMMENT '是否系统预置',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    sort_order INT DEFAULT 0 COMMENT '排序号',

    -- 审计字段
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    UNIQUE KEY uk_category_code (category_code),
    INDEX idx_apply_to_level (apply_to_level),
    INDEX idx_enabled (is_enabled),
    INDEX idx_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间分类配置表';


-- =====================================================
-- 2. 空间表（重构）
-- =====================================================
-- 先备份旧表
-- RENAME TABLE spaces TO spaces_backup_v9;

-- 创建新表
CREATE TABLE IF NOT EXISTS spaces_v10 (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- ========== 基础信息 ==========
    space_code VARCHAR(50) NOT NULL COMMENT '空间编码（自动生成）',
    space_name VARCHAR(100) NOT NULL COMMENT '空间名称',
    space_level ENUM('CAMPUS', 'BUILDING', 'FLOOR', 'ROOM') NOT NULL COMMENT '空间层级',
    category_id BIGINT COMMENT '分类ID（楼栋和房间需要）',
    description VARCHAR(500) COMMENT '描述',

    -- ========== 层级关系 ==========
    parent_id BIGINT COMMENT '父级空间ID',
    path VARCHAR(500) COMMENT '物化路径，如 /1/2/3/',
    level INT DEFAULT 0 COMMENT '层级深度（0=校区，1=楼，2=层，3=房间）',

    -- ========== 校区属性 (CAMPUS) ==========
    address VARCHAR(500) COMMENT '地址',

    -- ========== 楼栋属性 (BUILDING) ==========
    building_no INT COMMENT '楼栋编号（数字）',
    floor_count INT COMMENT '楼层数',

    -- ========== 楼层属性 (FLOOR) ==========
    floor_number INT COMMENT '楼层号（数字，负数为地下层）',

    -- ========== 房间属性 (ROOM) ==========
    room_no INT COMMENT '房间编号（数字）',
    capacity INT COMMENT '容量（床位数/座位数）',
    current_occupancy INT DEFAULT 0 COMMENT '当前占用数',
    gender_type TINYINT DEFAULT 0 COMMENT '性别限制：0不限/1男/2女',

    -- ========== 归属关系 ==========
    org_unit_id BIGINT COMMENT '归属组织单元ID',
    class_id BIGINT COMMENT '归属班级ID（宿舍分配）',
    responsible_user_id BIGINT COMMENT '负责人ID',

    -- ========== 状态 ==========
    status TINYINT DEFAULT 1 COMMENT '状态：0停用/1正常/2维护中',

    -- ========== 审计字段 ==========
    created_by BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,

    UNIQUE KEY uk_space_code (space_code),
    INDEX idx_space_level (space_level),
    INDEX idx_category_id (category_id),
    INDEX idx_parent_id (parent_id),
    INDEX idx_path (path(255)),
    INDEX idx_building_no (building_no),
    INDEX idx_floor_number (floor_number),
    INDEX idx_room_no (room_no),
    INDEX idx_org_unit_id (org_unit_id),
    INDEX idx_class_id (class_id),
    INDEX idx_status (status),
    INDEX idx_deleted (deleted),

    -- 外键约束
    CONSTRAINT fk_space_category FOREIGN KEY (category_id) REFERENCES space_categories(id),
    CONSTRAINT fk_space_parent FOREIGN KEY (parent_id) REFERENCES spaces_v10(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='空间表';


-- =====================================================
-- 3. 预置分类数据
-- =====================================================

-- 楼栋分类
INSERT INTO space_categories (category_code, category_name, apply_to_level, icon, color, description,
    has_capacity, bookable, assignable, occupiable, has_gender, is_system, sort_order) VALUES
('TEACHING_BUILDING', '教学楼', 'BUILDING', 'School', '#52c41a', '教学用途楼栋',
    FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, 1),
('DORMITORY_BUILDING', '宿舍楼', 'BUILDING', 'Home', '#1890ff', '学生宿舍楼栋',
    FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, 2),
('OFFICE_BUILDING', '办公楼', 'BUILDING', 'OfficeBuilding', '#fa8c16', '行政办公楼栋',
    FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, 3),
('COMPLEX_BUILDING', '综合楼', 'BUILDING', 'Building', '#722ed1', '多功能综合楼栋',
    FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, 4),
('LAB_BUILDING', '实验楼', 'BUILDING', 'Experiment', '#13c2c2', '实验教学楼栋',
    FALSE, FALSE, TRUE, FALSE, FALSE, TRUE, 5);

-- 房间分类
INSERT INTO space_categories (category_code, category_name, apply_to_level, icon, color, description,
    has_capacity, capacity_unit, default_capacity, bookable, assignable, occupiable, has_gender, is_system, sort_order) VALUES
('DORMITORY', '学生宿舍', 'ROOM', 'Bed', '#1890ff', '学生住宿房间',
    TRUE, '床位', 6, FALSE, TRUE, TRUE, TRUE, TRUE, 10),
('CLASSROOM', '普通教室', 'ROOM', 'Book', '#52c41a', '普通教学教室',
    TRUE, '座位', 50, TRUE, TRUE, FALSE, FALSE, TRUE, 11),
('MULTIMEDIA_ROOM', '多媒体教室', 'ROOM', 'Monitor', '#52c41a', '配备多媒体设备的教室',
    TRUE, '座位', 60, TRUE, TRUE, FALSE, FALSE, TRUE, 12),
('LAB', '实验室', 'ROOM', 'Flask', '#13c2c2', '实验教学用房',
    TRUE, '座位', 30, TRUE, TRUE, FALSE, FALSE, TRUE, 13),
('COMPUTER_ROOM', '机房', 'ROOM', 'Desktop', '#13c2c2', '计算机教学用房',
    TRUE, '座位', 50, TRUE, TRUE, FALSE, FALSE, TRUE, 14),
('MEETING_ROOM', '会议室', 'ROOM', 'Users', '#722ed1', '会议用房',
    TRUE, '座位', 20, TRUE, FALSE, FALSE, FALSE, TRUE, 15),
('OFFICE', '办公室', 'ROOM', 'Briefcase', '#fa8c16', '行政办公用房',
    TRUE, '人', 4, FALSE, TRUE, TRUE, FALSE, TRUE, 16),
('WAREHOUSE', '仓库', 'ROOM', 'Box', '#8c8c8c', '物资存储用房',
    FALSE, NULL, NULL, FALSE, TRUE, FALSE, FALSE, TRUE, 17),
('ACTIVITY_ROOM', '活动室', 'ROOM', 'Trophy', '#eb2f96', '学生活动用房',
    TRUE, '人', 30, TRUE, FALSE, FALSE, FALSE, TRUE, 18);


-- =====================================================
-- 4. 权限数据
-- =====================================================

-- 分类管理权限
INSERT INTO permission (code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    'system:place-category',
    '空间分类管理',
    'menu',
    (SELECT id FROM permission p2 WHERE code = 'system' LIMIT 1),
    CONCAT((SELECT path FROM permission p3 WHERE code = 'system' LIMIT 1),
           (SELECT COALESCE(MAX(id), 0) + 1 FROM permission), '/'),
    51,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'system:place-category');

INSERT INTO permission (code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    'system:place-category:view',
    '查看空间分类',
    'button',
    (SELECT id FROM permission p2 WHERE code = 'system:place-category' LIMIT 1),
    CONCAT((SELECT path FROM permission p3 WHERE code = 'system:place-category' LIMIT 1),
           (SELECT COALESCE(MAX(id), 0) + 1 FROM permission), '/'),
    1,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'system:place-category:view');

INSERT INTO permission (code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    'system:place-category:add',
    '新增空间分类',
    'button',
    (SELECT id FROM permission p2 WHERE code = 'system:place-category' LIMIT 1),
    CONCAT((SELECT path FROM permission p3 WHERE code = 'system:place-category' LIMIT 1),
           (SELECT COALESCE(MAX(id), 0) + 1 FROM permission), '/'),
    2,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'system:place-category:add');

INSERT INTO permission (code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    'system:place-category:edit',
    '编辑空间分类',
    'button',
    (SELECT id FROM permission p2 WHERE code = 'system:place-category' LIMIT 1),
    CONCAT((SELECT path FROM permission p3 WHERE code = 'system:place-category' LIMIT 1),
           (SELECT COALESCE(MAX(id), 0) + 1 FROM permission), '/'),
    3,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'system:place-category:edit');

INSERT INTO permission (code, name, type, parent_id, path, sort_order, enabled, created_at, updated_at)
SELECT
    'system:place-category:delete',
    '删除空间分类',
    'button',
    (SELECT id FROM permission p2 WHERE code = 'system:place-category' LIMIT 1),
    CONCAT((SELECT path FROM permission p3 WHERE code = 'system:place-category' LIMIT 1),
           (SELECT COALESCE(MAX(id), 0) + 1 FROM permission), '/'),
    4,
    1,
    NOW(),
    NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM permission WHERE code = 'system:place-category:delete');
