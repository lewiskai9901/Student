-- V8.1.1 场所类型配置表
-- 通用场所类型定义，支持宿舍、教室、活动室等多种场所

CREATE TABLE IF NOT EXISTS space_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    type_code VARCHAR(50) NOT NULL COMMENT '类型编码（唯一标识）',
    type_name VARCHAR(100) NOT NULL COMMENT '类型名称',
    parent_type_code VARCHAR(50) DEFAULT NULL COMMENT '父类型编码',
    level_order INT NOT NULL DEFAULT 0 COMMENT '层级顺序（0=顶级）',
    icon VARCHAR(50) DEFAULT NULL COMMENT '图标名称',
    color VARCHAR(20) DEFAULT NULL COMMENT '颜色代码',
    description VARCHAR(500) DEFAULT NULL COMMENT '类型描述',

    -- 场所特性
    can_have_beds TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可有床位（宿舍类）',
    can_have_seats TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可有座位（教室类）',
    can_be_inspected TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可被检查',
    can_be_borrowed TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可借用',
    requires_approval TINYINT(1) NOT NULL DEFAULT 0 COMMENT '借用是否需审批',

    -- 容量配置
    default_capacity INT DEFAULT NULL COMMENT '默认容量',
    capacity_unit VARCHAR(20) DEFAULT NULL COMMENT '容量单位（人/床/座）',

    -- 系统字段
    is_system TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否系统预置',
    is_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',

    UNIQUE KEY uk_type_code (type_code),
    KEY idx_parent_type_code (parent_type_code),
    KEY idx_is_enabled (is_enabled),
    KEY idx_sort_order (sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场所类型配置表';

-- 预置场所类型数据
INSERT INTO space_types (type_code, type_name, parent_type_code, level_order, icon, color, description,
    can_have_beds, can_have_seats, can_be_inspected, can_be_borrowed, requires_approval,
    default_capacity, capacity_unit, is_system, sort_order) VALUES

-- 顶级类型
('DORMITORY', '宿舍区', NULL, 0, 'Home', '#1890ff', '学生宿舍区域', 0, 0, 1, 0, 0, NULL, NULL, 1, 1),
('TEACHING', '教学区', NULL, 0, 'School', '#52c41a', '教学设施区域', 0, 0, 1, 0, 0, NULL, NULL, 1, 2),
('ACTIVITY', '活动区', NULL, 0, 'Trophy', '#722ed1', '活动场所区域', 0, 0, 1, 1, 1, NULL, NULL, 1, 3),
('OFFICE', '办公区', NULL, 0, 'Briefcase', '#fa8c16', '办公场所区域', 0, 0, 0, 0, 0, NULL, NULL, 1, 4),

-- 宿舍类型（DORMITORY的子类型）
('DORM_BUILDING', '宿舍楼', 'DORMITORY', 1, 'Building', '#1890ff', '宿舍楼栋', 0, 0, 1, 0, 0, NULL, NULL, 1, 10),
('DORM_FLOOR', '宿舍楼层', 'DORM_BUILDING', 2, 'Layers', '#1890ff', '宿舍楼层', 0, 0, 1, 0, 0, NULL, NULL, 1, 11),
('DORM_ROOM', '宿舍房间', 'DORM_FLOOR', 3, 'Home', '#1890ff', '学生宿舍房间', 1, 0, 1, 0, 0, 6, '床', 1, 12),

-- 教学类型（TEACHING的子类型）
('TEACH_BUILDING', '教学楼', 'TEACHING', 1, 'Building', '#52c41a', '教学楼栋', 0, 0, 1, 0, 0, NULL, NULL, 1, 20),
('CLASSROOM', '教室', 'TEACH_BUILDING', 2, 'Book', '#52c41a', '普通教室', 0, 1, 1, 1, 0, 50, '座', 1, 21),
('LAB', '实验室', 'TEACH_BUILDING', 2, 'Flask', '#52c41a', '实验教学室', 0, 1, 1, 1, 1, 30, '座', 1, 22),
('COMPUTER_ROOM', '机房', 'TEACH_BUILDING', 2, 'Monitor', '#52c41a', '计算机教室', 0, 1, 1, 1, 1, 60, '座', 1, 23),
('LIBRARY', '图书馆', 'TEACHING', 1, 'BookOpen', '#52c41a', '图书馆阅览室', 0, 1, 0, 0, 0, 200, '座', 1, 24),

-- 活动类型（ACTIVITY的子类型）
('MEETING_ROOM', '会议室', 'ACTIVITY', 1, 'Users', '#722ed1', '会议活动室', 0, 1, 0, 1, 1, 20, '座', 1, 30),
('LECTURE_HALL', '报告厅', 'ACTIVITY', 1, 'Mic', '#722ed1', '大型报告厅', 0, 1, 0, 1, 1, 300, '座', 1, 31),
('GYM', '体育馆', 'ACTIVITY', 1, 'Activity', '#722ed1', '室内体育馆', 0, 0, 0, 1, 1, NULL, NULL, 1, 32),
('PLAYGROUND', '运动场', 'ACTIVITY', 1, 'Sun', '#722ed1', '室外运动场地', 0, 0, 0, 1, 0, NULL, NULL, 1, 33);
