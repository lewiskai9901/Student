-- ============================================================
-- 统一场所管理系统数据库迁移脚本
-- 版本: V20260122_2
-- 描述: 创建统一的场所管理表结构，支持校区/楼宇/楼层/房间层级
-- ============================================================

-- ============================================================
-- 1. 场所类型配置表（支持动态扩展房间类型）
-- ============================================================
DROP TABLE IF EXISTS space_type_config;
CREATE TABLE space_type_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type_code VARCHAR(32) NOT NULL UNIQUE COMMENT '类型编码',
    type_name VARCHAR(64) NOT NULL COMMENT '类型名称',
    type_category VARCHAR(32) NOT NULL COMMENT '类型分类: BUILDING/ROOM',
    icon VARCHAR(64) COMMENT '图标名称(lucide图标)',
    color VARCHAR(16) COMMENT '主题色(tailwind类名)',
    has_capacity TINYINT DEFAULT 1 COMMENT '是否有容量概念',
    has_occupancy TINYINT DEFAULT 0 COMMENT '是否有入住/使用人员',
    has_gender TINYINT DEFAULT 0 COMMENT '是否区分性别',
    default_capacity INT COMMENT '默认容量',
    attribute_schema JSON COMMENT '扩展属性JSON Schema',
    sort_order INT DEFAULT 0 COMMENT '排序',
    enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '场所类型配置表';

-- ============================================================
-- 2. 场所主表（统一抽象）
-- ============================================================
DROP TABLE IF EXISTS space;
CREATE TABLE space (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    -- 基本信息
    space_code VARCHAR(64) NOT NULL COMMENT '场所编码(唯一)',
    space_name VARCHAR(128) NOT NULL COMMENT '场所名称',
    space_type VARCHAR(32) NOT NULL COMMENT '场所类型: CAMPUS/BUILDING/FLOOR/ROOM',
    room_type VARCHAR(32) COMMENT '房间类型(仅ROOM有): DORMITORY/CLASSROOM/OFFICE...',
    building_type VARCHAR(32) COMMENT '楼宇类型(仅BUILDING有): TEACHING/DORMITORY/OFFICE/MIXED',

    -- 层级关系
    parent_id BIGINT COMMENT '父级ID',
    path VARCHAR(512) COMMENT '物化路径: /1/2/3/',
    level INT NOT NULL DEFAULT 0 COMMENT '层级深度(0=根)',

    -- 位置信息（冗余字段，加速查询）
    campus_id BIGINT COMMENT '所属校区ID',
    building_id BIGINT COMMENT '所属楼宇ID',
    floor_number INT COMMENT '楼层号',

    -- 容量信息
    capacity INT COMMENT '容量/座位数/床位数',
    current_occupancy INT DEFAULT 0 COMMENT '当前占用数',

    -- 归属信息
    org_unit_id BIGINT COMMENT '所属组织单元',
    responsible_user_id BIGINT COMMENT '负责人ID',

    -- 状态
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0停用 1正常 2维修中',

    -- 扩展属性（JSON格式，存储类型特有的属性）
    attributes JSON COMMENT '扩展属性',

    -- 描述
    description VARCHAR(512) COMMENT '描述/备注',

    -- 审计字段
    created_by BIGINT COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT COMMENT '更新人',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,

    -- 唯一约束
    UNIQUE KEY uk_space_code (space_code),

    -- 索引
    INDEX idx_parent_id (parent_id),
    INDEX idx_space_type (space_type),
    INDEX idx_room_type (room_type),
    INDEX idx_building_type (building_type),
    INDEX idx_building_id (building_id),
    INDEX idx_floor_number (floor_number),
    INDEX idx_org_unit_id (org_unit_id),
    INDEX idx_status (status),
    INDEX idx_path (path(255)),
    INDEX idx_deleted (deleted)
) COMMENT '场所统一表';

-- ============================================================
-- 3. 宿舍扩展表（宿舍特有属性）
-- ============================================================
DROP TABLE IF EXISTS space_dormitory_ext;
CREATE TABLE space_dormitory_ext (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_id BIGINT NOT NULL UNIQUE COMMENT '场所ID',

    -- 宿舍特有属性
    gender_type TINYINT NOT NULL DEFAULT 1 COMMENT '性别类型: 1男 2女 3混合',
    bed_count INT COMMENT '床位数(可能与capacity不同)',
    facilities VARCHAR(255) COMMENT '设施配置(空调/热水器/独卫等)',

    -- 分配信息
    assigned_class_ids VARCHAR(512) COMMENT '指定班级ID列表(逗号分隔)',
    supervisor_id BIGINT COMMENT '宿管员ID',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_space_id (space_id),
    INDEX idx_gender_type (gender_type),
    INDEX idx_supervisor_id (supervisor_id)
) COMMENT '宿舍扩展属性表';

-- ============================================================
-- 4. 教室扩展表（教室特有属性）
-- ============================================================
DROP TABLE IF EXISTS space_classroom_ext;
CREATE TABLE space_classroom_ext (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_id BIGINT NOT NULL UNIQUE COMMENT '场所ID',

    -- 教室特有属性
    classroom_category VARCHAR(32) COMMENT '教室分类: NORMAL/MULTIMEDIA/SMART',
    assigned_class_id BIGINT COMMENT '固定使用班级ID',
    has_projector TINYINT DEFAULT 0 COMMENT '是否有投影仪',
    has_air_conditioner TINYINT DEFAULT 0 COMMENT '是否有空调',
    has_computer TINYINT DEFAULT 0 COMMENT '是否有电脑',
    equipment_info VARCHAR(255) COMMENT '设备配置说明',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_space_id (space_id),
    INDEX idx_assigned_class_id (assigned_class_id),
    INDEX idx_classroom_category (classroom_category)
) COMMENT '教室扩展属性表';

-- ============================================================
-- 5. 实验室/实训室扩展表
-- ============================================================
DROP TABLE IF EXISTS space_lab_ext;
CREATE TABLE space_lab_ext (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_id BIGINT NOT NULL UNIQUE COMMENT '场所ID',

    -- 实验室特有属性
    lab_category VARCHAR(32) COMMENT '实验室分类: PHYSICS/CHEMISTRY/COMPUTER/TRAINING',
    safety_level TINYINT DEFAULT 1 COMMENT '安全等级: 1普通 2中等 3高',
    major_id BIGINT COMMENT '所属专业ID',
    equipment_list TEXT COMMENT '设备清单(JSON)',
    safety_notice TEXT COMMENT '安全须知',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_space_id (space_id),
    INDEX idx_major_id (major_id),
    INDEX idx_lab_category (lab_category)
) COMMENT '实验室/实训室扩展属性表';

-- ============================================================
-- 6. 办公室扩展表
-- ============================================================
DROP TABLE IF EXISTS space_office_ext;
CREATE TABLE space_office_ext (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_id BIGINT NOT NULL UNIQUE COMMENT '场所ID',

    -- 办公室特有属性
    office_type VARCHAR(32) COMMENT '办公室类型: PRIVATE/SHARED/OPEN',
    department_id BIGINT COMMENT '所属部门ID',
    workstation_count INT COMMENT '工位数量',
    phone_number VARCHAR(32) COMMENT '办公电话',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_space_id (space_id),
    INDEX idx_department_id (department_id)
) COMMENT '办公室扩展属性表';

-- ============================================================
-- 7. 场所占用表（学生入住/工位分配）
-- ============================================================
DROP TABLE IF EXISTS space_occupant;
CREATE TABLE space_occupant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_id BIGINT NOT NULL COMMENT '场所ID(宿舍/办公室)',
    occupant_type VARCHAR(32) NOT NULL COMMENT '占用者类型: STUDENT/TEACHER/STAFF',
    occupant_id BIGINT NOT NULL COMMENT '占用者ID',

    -- 床位/工位信息
    position_no INT COMMENT '位置编号(床位号/工位号)',

    -- 时间信息
    check_in_date DATE COMMENT '入住日期',
    check_out_date DATE COMMENT '退出日期',

    -- 状态
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0已退出 1在住',

    remark VARCHAR(255) COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_space_position_active (space_id, position_no, status),
    INDEX idx_space_id (space_id),
    INDEX idx_occupant (occupant_type, occupant_id),
    INDEX idx_status (status)
) COMMENT '场所占用表(学生入住/工位分配)';

-- ============================================================
-- 8. 场所-组织单元分配表（支持按楼层分配）
-- ============================================================
DROP TABLE IF EXISTS space_org_assignment;
CREATE TABLE space_org_assignment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    space_id BIGINT NOT NULL COMMENT '场所ID(通常是楼宇)',
    org_unit_id BIGINT NOT NULL COMMENT '组织单元ID',

    -- 范围限定（可选）
    floor_start INT COMMENT '起始楼层',
    floor_end INT COMMENT '结束楼层',

    -- 分配类型
    assignment_type VARCHAR(32) DEFAULT 'EXCLUSIVE' COMMENT '分配类型: EXCLUSIVE独占/SHARED共享',

    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0无效 1有效',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_space_org (space_id, org_unit_id),
    INDEX idx_space_id (space_id),
    INDEX idx_org_unit_id (org_unit_id)
) COMMENT '场所-组织单元分配表';

-- ============================================================
-- 初始化场所类型配置
-- ============================================================
INSERT INTO space_type_config (type_code, type_name, type_category, icon, color, has_capacity, has_occupancy, has_gender, default_capacity, sort_order) VALUES
-- 楼宇类型
('TEACHING', '教学楼', 'BUILDING', 'GraduationCap', 'blue', 0, 0, 0, NULL, 1),
('DORMITORY_BUILDING', '宿舍楼', 'BUILDING', 'Home', 'teal', 0, 0, 0, NULL, 2),
('OFFICE_BUILDING', '办公楼', 'BUILDING', 'Building2', 'gray', 0, 0, 0, NULL, 3),
('MIXED', '综合楼', 'BUILDING', 'Layers', 'purple', 0, 0, 0, NULL, 4),

-- 房间类型 - 宿舍
('DORMITORY', '学生宿舍', 'ROOM', 'BedDouble', 'teal', 1, 1, 1, 6, 10),
('STAFF_DORMITORY', '教职工宿舍', 'ROOM', 'Bed', 'cyan', 1, 1, 0, 2, 11),

-- 房间类型 - 教室
('CLASSROOM', '普通教室', 'ROOM', 'School', 'blue', 1, 0, 0, 50, 20),
('MULTIMEDIA', '多媒体教室', 'ROOM', 'Monitor', 'indigo', 1, 0, 0, 60, 21),
('SMART_CLASSROOM', '智慧教室', 'ROOM', 'Cpu', 'violet', 1, 0, 0, 40, 22),

-- 房间类型 - 实验室
('LAB', '实验室', 'ROOM', 'FlaskConical', 'amber', 1, 0, 0, 30, 30),
('COMPUTER_LAB', '计算机房', 'ROOM', 'Monitor', 'sky', 1, 0, 0, 50, 31),
('TRAINING', '实训室', 'ROOM', 'Wrench', 'orange', 1, 0, 0, 40, 32),

-- 房间类型 - 办公
('OFFICE', '办公室', 'ROOM', 'Briefcase', 'slate', 1, 1, 0, 4, 40),
('MEETING', '会议室', 'ROOM', 'Users', 'emerald', 1, 0, 0, 20, 41),

-- 房间类型 - 其他
('LIBRARY', '图书馆/阅览室', 'ROOM', 'BookOpen', 'amber', 1, 0, 0, 100, 50),
('STORAGE', '仓库', 'ROOM', 'Package', 'stone', 0, 0, 0, NULL, 60),
('UTILITY', '功能房', 'ROOM', 'Settings', 'zinc', 0, 0, 0, NULL, 70),
('BATHROOM', '卫生间', 'ROOM', 'Bath', 'gray', 0, 0, 1, NULL, 71),
('POWER_ROOM', '配电室', 'ROOM', 'Zap', 'yellow', 0, 0, 0, NULL, 72);

-- ============================================================
-- 初始化默认校区
-- ============================================================
INSERT INTO space (space_code, space_name, space_type, parent_id, path, level, status, created_at) VALUES
('CAMPUS_MAIN', '主校区', 'CAMPUS', NULL, '/1/', 0, 1, NOW());

-- 更新路径（使用实际ID）
UPDATE space SET path = CONCAT('/', id, '/') WHERE space_type = 'CAMPUS';

-- ============================================================
-- 修改资产表，添加 space_id 字段
-- ============================================================
-- 检查字段是否存在，如果不存在则添加
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE()
               AND TABLE_NAME = 'asset'
               AND COLUMN_NAME = 'space_id');

SET @sql := IF(@exist = 0,
    'ALTER TABLE asset ADD COLUMN space_id BIGINT COMMENT ''场所ID'' AFTER responsible_user_name, ADD INDEX idx_space_id (space_id)',
    'SELECT ''Column space_id already exists''');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============================================================
-- 创建视图：兼容旧的楼宇查询
-- ============================================================
CREATE OR REPLACE VIEW v_buildings AS
SELECT
    s.id,
    s.space_code AS building_no,
    s.space_name AS building_name,
    s.building_type,
    s.status,
    s.description,
    s.created_at,
    s.updated_at,
    (SELECT COUNT(*) FROM space c WHERE c.parent_id = s.id AND c.space_type = 'FLOOR' AND c.deleted = 0) AS total_floors,
    (SELECT COUNT(*) FROM space r WHERE r.building_id = s.id AND r.space_type = 'ROOM' AND r.deleted = 0) AS total_rooms
FROM space s
WHERE s.space_type = 'BUILDING' AND s.deleted = 0;

-- ============================================================
-- 创建视图：兼容旧的宿舍查询
-- ============================================================
CREATE OR REPLACE VIEW v_dormitories AS
SELECT
    s.id,
    s.space_code AS dormitory_no,
    s.space_name,
    s.building_id,
    (SELECT space_name FROM space WHERE id = s.building_id) AS building_name,
    (SELECT space_code FROM space WHERE id = s.building_id) AS building_no,
    s.floor_number,
    s.capacity AS max_occupancy,
    s.current_occupancy,
    s.org_unit_id,
    (SELECT name FROM org_unit WHERE id = s.org_unit_id) AS org_unit_name,
    s.status,
    e.gender_type,
    e.bed_count,
    e.facilities,
    e.assigned_class_ids,
    e.supervisor_id,
    s.created_at,
    s.updated_at
FROM space s
LEFT JOIN space_dormitory_ext e ON s.id = e.space_id
WHERE s.space_type = 'ROOM' AND s.room_type = 'DORMITORY' AND s.deleted = 0;

-- ============================================================
-- 创建视图：兼容旧的教室查询
-- ============================================================
CREATE OR REPLACE VIEW v_classrooms AS
SELECT
    s.id,
    s.space_code AS classroom_no,
    s.space_name AS classroom_name,
    s.building_id,
    (SELECT space_name FROM space WHERE id = s.building_id) AS building_name,
    (SELECT space_code FROM space WHERE id = s.building_id) AS building_no,
    s.floor_number AS floor,
    s.capacity,
    s.room_type AS classroom_type,
    s.status,
    e.classroom_category,
    e.assigned_class_id AS class_id,
    e.has_projector,
    e.has_air_conditioner,
    e.has_computer,
    e.equipment_info,
    s.created_at,
    s.updated_at
FROM space s
LEFT JOIN space_classroom_ext e ON s.id = e.space_id
WHERE s.space_type = 'ROOM'
  AND s.room_type IN ('CLASSROOM', 'MULTIMEDIA', 'SMART_CLASSROOM')
  AND s.deleted = 0;

-- ============================================================
-- 完成
-- ============================================================
SELECT 'Unified Space Management tables created successfully!' AS result;
