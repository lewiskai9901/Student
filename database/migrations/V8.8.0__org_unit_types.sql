-- =============================================
-- V8.8.0: 组织类型管理表
-- =============================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 创建组织类型表
CREATE TABLE IF NOT EXISTS org_unit_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    type_code VARCHAR(50) NOT NULL UNIQUE COMMENT '类型编码',
    type_name VARCHAR(100) NOT NULL COMMENT '类型名称',
    parent_type_code VARCHAR(50) COMMENT '父类型编码',
    level_order INT DEFAULT 0 COMMENT '层级顺序(1=一级,2=二级...)',
    icon VARCHAR(50) COMMENT '图标',
    color VARCHAR(20) COMMENT '颜色',
    description VARCHAR(500) COMMENT '描述',
    is_academic BOOLEAN DEFAULT TRUE COMMENT '是否教学单位',
    can_be_inspected BOOLEAN DEFAULT TRUE COMMENT '是否可被检查',
    can_have_children BOOLEAN DEFAULT TRUE COMMENT '是否可有子级',
    max_depth INT COMMENT '最大子级深度',
    is_system BOOLEAN DEFAULT FALSE COMMENT '是否系统预置',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_type_code (type_code),
    INDEX idx_parent_type_code (parent_type_code),
    INDEX idx_is_academic (is_academic),
    INDEX idx_is_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织类型表';

-- 预置系统类型（与原枚举保持一致）
INSERT INTO org_unit_types (type_code, type_name, parent_type_code, level_order, is_academic, can_have_children, is_system, sort_order) VALUES
-- 教学单位层级
('SCHOOL', '学校', NULL, 1, TRUE, TRUE, TRUE, 1),
('COLLEGE', '系部', 'SCHOOL', 2, TRUE, TRUE, TRUE, 2),
('DEPARTMENT', '专业', 'COLLEGE', 3, TRUE, TRUE, TRUE, 3),
('TEACHING_GROUP', '班级', 'DEPARTMENT', 4, TRUE, FALSE, TRUE, 4),
-- 职能部门
('STUDENT_AFFAIRS', '学工处', 'SCHOOL', 2, FALSE, FALSE, TRUE, 10),
('ACADEMIC_AFFAIRS', '教务处', 'SCHOOL', 2, FALSE, FALSE, TRUE, 11),
('LOGISTICS', '后勤处', 'SCHOOL', 2, FALSE, FALSE, TRUE, 12),
('FINANCE', '财务处', 'SCHOOL', 2, FALSE, FALSE, TRUE, 13),
('GENERAL_OFFICE', '校办', 'SCHOOL', 2, FALSE, FALSE, TRUE, 14),
('HR', '人事处', 'SCHOOL', 2, FALSE, FALSE, TRUE, 15)
ON DUPLICATE KEY UPDATE type_name = VALUES(type_name);

-- 删除V6实体类型相关表（如果存在）
DROP TABLE IF EXISTS entity_groups;
DROP TABLE IF EXISTS entity_types;
