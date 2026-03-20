-- V8.1.2 用户类型配置表
-- 通用用户类型定义，支持教师、学生、管理员等多种用户

CREATE TABLE IF NOT EXISTS user_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    type_code VARCHAR(50) NOT NULL COMMENT '类型编码（唯一标识）',
    type_name VARCHAR(100) NOT NULL COMMENT '类型名称',
    parent_type_code VARCHAR(50) DEFAULT NULL COMMENT '父类型编码',
    level_order INT NOT NULL DEFAULT 0 COMMENT '层级顺序（0=顶级）',
    description VARCHAR(500) DEFAULT NULL COMMENT '类型描述',

    -- 数据约束
    requires_org TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要班级归属',
    requires_place TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要宿舍归属',

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
INSERT INTO user_types (type_code, type_name, parent_type_code, level_order, description,
    requires_org, requires_place, default_role_codes, is_system, sort_order) VALUES

-- 顶级类型（default_role_codes 必须匹配 roles 表中的 role_code）
('ADMIN', '管理员', NULL, 0, '系统管理人员', 0, 0, 'SUPER_ADMIN', 0, 1),
('TEACHER', '教职工', NULL, 0, '教职工人员', 0, 0, 'SUBJECT_TEACHER', 0, 2),
('STUDENT', '学生', NULL, 0, '在校学生', 1, 1, 'STUDENT', 0, 3),
('EXTERNAL', '外部人员', NULL, 0, '临时或外部人员', 0, 0, NULL, 0, 4),

-- 管理员子类型（仅SUPER_ADMIN为系统内置，不可删除）
('SUPER_ADMIN', '超级管理员', 'ADMIN', 1, '系统最高管理员', 0, 0, 'SUPER_ADMIN', 1, 10),
('SYSTEM_ADMIN', '系统管理员', 'ADMIN', 1, '系统配置管理员', 0, 0, 'SCHOOL_ADMIN', 0, 11),
('ORG_ADMIN', '组织管理员', 'ADMIN', 1, '组织机构管理员', 0, 0, 'ACADEMIC_DIRECTOR,SUBJECT_TEACHER,INSPECTOR,DORMITORY_MANAGER', 0, 12),

-- 教职工子类型
('TEACHING_STAFF', '教学人员', 'TEACHER', 1, '从事教学工作的教师', 0, 0, 'SUBJECT_TEACHER', 0, 20),
('CLASS_TEACHER', '班主任', 'TEACHER', 1, '班级管理教师', 0, 0, 'CLASS_TEACHER', 0, 21),
('COUNSELOR', '辅导员', 'TEACHER', 1, '学生辅导员', 0, 0, 'GRADE_DIRECTOR', 0, 22),
('INSPECTOR', '检查员', 'TEACHER', 1, '量化检查专员', 0, 0, 'INSPECTOR', 0, 23),
('DORM_MANAGER', '宿管员', 'TEACHER', 1, '宿舍管理人员', 0, 0, 'DORMITORY_MANAGER', 0, 24),
('ADMIN_STAFF', '行政人员', 'TEACHER', 1, '行政管理人员', 0, 0, 'SCHOOL_ADMIN', 0, 25),

-- 学生子类型
('UNDERGRADUATE', '本科生', 'STUDENT', 1, '本科在读学生', 1, 1, 'STUDENT', 0, 30),
('GRADUATE', '研究生', 'STUDENT', 1, '研究生在读学生', 1, 1, 'STUDENT', 0, 31),
('EXCHANGE', '交换生', 'STUDENT', 1, '交换访问学生', 1, 0, 'STUDENT', 0, 32),

-- 外部人员子类型
('VISITOR', '访客', 'EXTERNAL', 1, '临时来访人员', 0, 0, NULL, 0, 40),
('CONTRACTOR', '外包人员', 'EXTERNAL', 1, '外包服务人员', 0, 0, NULL, 0, 41);
