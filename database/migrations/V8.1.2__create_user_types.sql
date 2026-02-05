-- V8.1.2 用户类型配置表
-- 通用用户类型定义，支持教师、学生、管理员等多种用户

CREATE TABLE IF NOT EXISTS user_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    type_code VARCHAR(50) NOT NULL COMMENT '类型编码（唯一标识）',
    type_name VARCHAR(100) NOT NULL COMMENT '类型名称',
    parent_type_code VARCHAR(50) DEFAULT NULL COMMENT '父类型编码',
    level_order INT NOT NULL DEFAULT 0 COMMENT '层级顺序（0=顶级）',
    icon VARCHAR(50) DEFAULT NULL COMMENT '图标名称',
    color VARCHAR(20) DEFAULT NULL COMMENT '颜色代码',
    description VARCHAR(500) DEFAULT NULL COMMENT '类型描述',

    -- 用户特性
    can_login TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可登录系统',
    can_be_inspector TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可作为检查员',
    can_be_inspected TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可被检查',
    can_manage_org TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可管理组织',
    can_view_reports TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可查看报表',
    requires_class TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要班级归属',
    requires_dormitory TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要宿舍归属',

    -- 默认权限配置
    default_role_codes VARCHAR(500) DEFAULT NULL COMMENT '默认角色编码（逗号分隔）',

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户类型配置表';

-- 预置用户类型数据
INSERT INTO user_types (type_code, type_name, parent_type_code, level_order, icon, color, description,
    can_login, can_be_inspector, can_be_inspected, can_manage_org, can_view_reports,
    requires_class, requires_dormitory, default_role_codes, is_system, sort_order) VALUES

-- 顶级类型
('ADMIN', '管理员', NULL, 0, 'Setting', '#f5222d', '系统管理人员', 1, 1, 0, 1, 1, 0, 0, 'admin', 1, 1),
('TEACHER', '教职工', NULL, 0, 'User', '#1890ff', '教职工人员', 1, 1, 0, 0, 1, 0, 0, 'teacher', 1, 2),
('STUDENT', '学生', NULL, 0, 'UserFilled', '#52c41a', '在校学生', 1, 0, 1, 0, 0, 1, 1, 'student', 1, 3),
('EXTERNAL', '外部人员', NULL, 0, 'Avatar', '#8c8c8c', '临时或外部人员', 0, 0, 0, 0, 0, 0, 0, NULL, 1, 4),

-- 管理员子类型
('SUPER_ADMIN', '超级管理员', 'ADMIN', 1, 'Setting', '#f5222d', '系统最高管理员', 1, 1, 0, 1, 1, 0, 0, 'super_admin', 1, 10),
('SYSTEM_ADMIN', '系统管理员', 'ADMIN', 1, 'Tools', '#fa541c', '系统配置管理员', 1, 0, 0, 1, 1, 0, 0, 'system_admin', 1, 11),
('ORG_ADMIN', '组织管理员', 'ADMIN', 1, 'OfficeBuilding', '#fa8c16', '组织机构管理员', 1, 0, 0, 1, 1, 0, 0, 'org_admin', 1, 12),

-- 教职工子类型
('TEACHING_STAFF', '教学人员', 'TEACHER', 1, 'Reading', '#1890ff', '从事教学工作的教师', 1, 1, 0, 0, 1, 0, 0, 'teacher', 1, 20),
('CLASS_TEACHER', '班主任', 'TEACHER', 1, 'Stamp', '#13c2c2', '班级管理教师', 1, 1, 0, 1, 1, 0, 0, 'class_teacher', 1, 21),
('COUNSELOR', '辅导员', 'TEACHER', 1, 'Service', '#722ed1', '学生辅导员', 1, 1, 0, 1, 1, 0, 0, 'counselor', 1, 22),
('INSPECTOR', '检查员', 'TEACHER', 1, 'View', '#eb2f96', '量化检查专员', 1, 1, 0, 0, 1, 0, 0, 'inspector', 1, 23),
('DORM_MANAGER', '宿管员', 'TEACHER', 1, 'House', '#faad14', '宿舍管理人员', 1, 1, 0, 0, 1, 0, 0, 'dorm_manager', 1, 24),
('ADMIN_STAFF', '行政人员', 'TEACHER', 1, 'Folder', '#2f54eb', '行政管理人员', 1, 0, 0, 0, 1, 0, 0, 'admin_staff', 1, 25),

-- 学生子类型
('UNDERGRADUATE', '本科生', 'STUDENT', 1, 'Notebook', '#52c41a', '本科在读学生', 1, 0, 1, 0, 0, 1, 1, 'student', 1, 30),
('GRADUATE', '研究生', 'STUDENT', 1, 'Document', '#52c41a', '研究生在读学生', 1, 0, 1, 0, 0, 1, 1, 'student', 1, 31),
('EXCHANGE', '交换生', 'STUDENT', 1, 'Promotion', '#52c41a', '交换访问学生', 1, 0, 1, 0, 0, 1, 0, 'student', 1, 32),

-- 外部人员子类型
('VISITOR', '访客', 'EXTERNAL', 1, 'Visitor', '#8c8c8c', '临时来访人员', 0, 0, 0, 0, 0, 0, 0, NULL, 1, 40),
('CONTRACTOR', '外包人员', 'EXTERNAL', 1, 'Suitcase', '#8c8c8c', '外包服务人员', 0, 0, 0, 0, 0, 0, 0, NULL, 1, 41);
