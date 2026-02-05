-- V8.1.0 创建组织类型配置表
-- 支持灵活定义组织层级类型（学校、院系、系部等）

-- ==================== 组织类型表 ====================
CREATE TABLE IF NOT EXISTS org_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    type_code VARCHAR(50) NOT NULL COMMENT '类型编码（唯一标识）',
    type_name VARCHAR(100) NOT NULL COMMENT '类型名称',
    parent_type_code VARCHAR(50) DEFAULT NULL COMMENT '父类型编码（层级关系）',
    level_order INT NOT NULL DEFAULT 0 COMMENT '层级顺序（0=顶级）',
    icon VARCHAR(50) DEFAULT NULL COMMENT '图标标识',
    color VARCHAR(20) DEFAULT NULL COMMENT '显示颜色',
    description VARCHAR(500) DEFAULT NULL COMMENT '类型描述',

    -- 特性配置
    can_have_classes TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可包含班级',
    can_have_students TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否可直接包含学生',
    can_be_inspected TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可被检查',
    can_have_leader TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否有负责人',

    -- 系统标识
    is_system TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否系统预置（不可删除）',
    is_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序顺序',

    -- 审计字段
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',

    UNIQUE KEY uk_type_code (type_code),
    INDEX idx_parent_type (parent_type_code),
    INDEX idx_level_order (level_order),
    INDEX idx_is_enabled (is_enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织类型配置表';

-- ==================== 初始化系统预置类型 ====================
INSERT INTO org_types (type_code, type_name, parent_type_code, level_order, icon, color, description,
                       can_have_classes, can_have_students, can_be_inspected, can_have_leader, is_system, sort_order) VALUES
-- 顶级类型
('SCHOOL', '学校', NULL, 0, 'School', '#1890ff', '学校/教育机构', 0, 0, 0, 1, 1, 1),

-- 二级类型（院系）
('COLLEGE', '学院', 'SCHOOL', 1, 'Building', '#52c41a', '二级学院/分院', 0, 0, 1, 1, 1, 2),
('DEPARTMENT', '系部', 'SCHOOL', 1, 'Briefcase', '#722ed1', '教学系部', 1, 0, 1, 1, 1, 3),

-- 三级类型（专业/教研室）
('MAJOR', '专业', 'COLLEGE', 2, 'BookOpen', '#fa8c16', '专业方向', 1, 0, 1, 1, 1, 4),
('TEACHING_GROUP', '教研室', 'DEPARTMENT', 2, 'Users', '#13c2c2', '教学研究组', 0, 0, 0, 1, 1, 5),

-- 职能部门类型
('FUNCTIONAL', '职能部门', 'SCHOOL', 1, 'Settings', '#8c8c8c', '行政职能部门', 0, 0, 0, 1, 1, 10),
('ADMINISTRATIVE', '行政单位', 'SCHOOL', 1, 'FileText', '#595959', '行政管理单位', 0, 0, 0, 1, 1, 11);

-- ==================== 更新 org_units 表添加类型关联 ====================
-- 检查是否已有 type_code 字段
SET @column_exists = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'org_units'
    AND COLUMN_NAME = 'type_code'
);

-- 如果不存在则添加
SET @sql = IF(@column_exists = 0,
    'ALTER TABLE org_units ADD COLUMN type_code VARCHAR(50) DEFAULT NULL COMMENT ''组织类型编码'' AFTER unit_category',
    'SELECT ''Column type_code already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加外键索引
CREATE INDEX IF NOT EXISTS idx_org_units_type_code ON org_units(type_code);

-- ==================== 迁移现有数据到新类型系统 ====================
-- 根据 unit_category 设置 type_code
UPDATE org_units SET type_code = 'COLLEGE' WHERE unit_category = 'ACADEMIC' AND type_code IS NULL;
UPDATE org_units SET type_code = 'FUNCTIONAL' WHERE unit_category = 'FUNCTIONAL' AND type_code IS NULL;
UPDATE org_units SET type_code = 'ADMINISTRATIVE' WHERE unit_category = 'ADMINISTRATIVE' AND type_code IS NULL;
UPDATE org_units SET type_code = 'DEPARTMENT' WHERE unit_category IS NULL AND type_code IS NULL;
