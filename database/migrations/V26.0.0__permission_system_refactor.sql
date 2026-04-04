-- V26.0.0 Permission System Refactor
-- 权限系统全面重构：去硬编码 + 多租户 + jCasbin + 动态模块配置

-- ============================================================
-- 1. 租户表
-- ============================================================
CREATE TABLE IF NOT EXISTS tenants (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_code VARCHAR(50)  NOT NULL UNIQUE,
    tenant_name VARCHAR(200) NOT NULL,
    domain      VARCHAR(200) DEFAULT NULL COMMENT '绑定域名',
    config      JSON         DEFAULT NULL COMMENT '租户配置',
    enabled     TINYINT(1)   DEFAULT 1,
    created_at  DATETIME     DEFAULT NOW(),
    updated_at  DATETIME     DEFAULT NOW() ON UPDATE NOW()
) COMMENT='租户表';

INSERT INTO tenants (id, tenant_code, tenant_name) VALUES (1, 'default', '默认租户');

-- ============================================================
-- 2. 现有表加 tenant_id (MySQL 8.0 compatible - no IF NOT EXISTS for ALTER)
-- ============================================================
-- NOTE: Run these one at a time if column already exists
ALTER TABLE users ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE roles ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE permissions ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE user_roles ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE role_permissions ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE access_relations ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE org_units ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE classes ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE students ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;

-- 索引
CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_roles_tenant ON roles(tenant_id);
CREATE INDEX idx_permissions_tenant ON permissions(tenant_id);
CREATE INDEX idx_user_roles_tenant ON user_roles(tenant_id);
CREATE INDEX idx_role_permissions_tenant ON role_permissions(tenant_id);
CREATE INDEX idx_access_relations_tenant ON access_relations(tenant_id);
CREATE INDEX idx_org_units_tenant ON org_units(tenant_id);
CREATE INDEX idx_classes_tenant ON classes(tenant_id);
CREATE INDEX idx_students_tenant ON students(tenant_id);

-- ============================================================
-- 3. roles 表添加 role_type 列 (VARCHAR, 不再约束枚举值)
-- ============================================================
ALTER TABLE roles ADD COLUMN role_type VARCHAR(50) DEFAULT 'CUSTOM';
UPDATE roles SET role_type = 'SUPER_ADMIN' WHERE role_code = 'SUPER_ADMIN';

-- ============================================================
-- 4. data_modules 表（动态数据模块配置）
-- ============================================================
DROP TABLE IF EXISTS module_scope_item_types;
DROP TABLE IF EXISTS scope_item_types;
DROP TABLE IF EXISTS data_modules;

CREATE TABLE data_modules (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id      BIGINT       NOT NULL DEFAULT 1,
    module_code    VARCHAR(50)  NOT NULL,
    module_name    VARCHAR(100) NOT NULL,
    domain_code    VARCHAR(50)  NOT NULL COMMENT '所属领域标识',
    domain_name    VARCHAR(100) NOT NULL COMMENT '领域显示名',
    resource_type  VARCHAR(50)  DEFAULT NULL COMMENT '对应 access_relations.resource_type',
    org_unit_field VARCHAR(50)  DEFAULT 'org_unit_id' COMMENT '表中的组织字段名',
    creator_field  VARCHAR(50)  DEFAULT 'created_by',
    sort_order     INT          DEFAULT 0,
    enabled        TINYINT(1)   DEFAULT 1,
    UNIQUE KEY uk_tenant_code (tenant_id, module_code)
) COMMENT='动态数据模块配置';

-- 插入默认模块配置
INSERT INTO data_modules (tenant_id, module_code, module_name, domain_code, domain_name, resource_type, org_unit_field, creator_field, sort_order) VALUES
-- Organization domain
(1, 'org_unit',             '组织单元',   'organization', '组织管理', 'org_unit',             'parent_id',   'created_by', 1),
-- Student domain
(1, 'student',              '学生信息',   'student',      '学生管理', 'student',              'org_unit_id', 'created_by', 10),
(1, 'school_class',         '班级管理',   'student',      '学生管理', 'school_class',         'org_unit_id', 'created_by', 11),
(1, 'attendance',           '考勤记录',   'student',      '学生管理', 'attendance',           'class_id',    'recorded_by', 12),
-- Teaching domain
(1, 'teaching_task',        '教学任务',   'teaching',     '教学管理', 'teaching_task',        'org_unit_id', 'created_by', 20),
(1, 'exam_batch',           '考试管理',   'teaching',     '教学管理', 'exam_batch',           '',            'created_by', 21),
(1, 'student_grade',        '学生成绩',   'teaching',     '教学管理', 'student_grade',        'class_id',    '',           22),
-- Inspection domain
(1, 'inspection_template',  '检查模板',   'inspection',   '检查平台', 'inspection_template',  'org_unit_id', 'created_by', 30),
(1, 'inspection_record',    '检查记录',   'inspection',   '检查平台', 'inspection_record',    'org_unit_id', 'created_by', 31),
-- Place domain
(1, 'place',                '场所管理',   'place',        '场所管理', 'place',                'org_unit_id', 'created_by', 40);

-- ============================================================
-- 5. scope_item_types 表（动态范围项类型配置）
-- ============================================================
CREATE TABLE scope_item_types (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id        BIGINT       NOT NULL DEFAULT 1,
    item_type_code   VARCHAR(50)  NOT NULL,
    item_type_name   VARCHAR(100) NOT NULL,
    ref_table        VARCHAR(100) NOT NULL COMMENT '引用表名',
    ref_id_field     VARCHAR(50)  DEFAULT 'id',
    ref_name_field   VARCHAR(50)  DEFAULT 'name',
    ref_parent_field VARCHAR(50)  DEFAULT NULL,
    support_children TINYINT(1)   DEFAULT 0,
    sort_order       INT          DEFAULT 0,
    UNIQUE KEY uk_tenant_code (tenant_id, item_type_code)
) COMMENT='动态范围项类型配置';

INSERT INTO scope_item_types (tenant_id, item_type_code, item_type_name, ref_table, ref_id_field, ref_name_field, ref_parent_field, support_children, sort_order) VALUES
(1, 'ORG_UNIT', '部门',   'org_units', 'id', 'name',       'parent_id', 1, 1),
(1, 'CLASS',    '班级',   'classes',   'id', 'class_name', NULL,        0, 2),
(1, 'GRADE',    '年级',   'grades',    'id', 'name',       NULL,        0, 3),
(1, 'BUILDING', '楼栋',   'place',     'id', 'place_name', NULL,        0, 4),
(1, 'MAJOR',    '专业',   'majors',    'id', 'name',       NULL,        0, 5);

-- ============================================================
-- 6. module_scope_item_types 关联表
-- ============================================================
CREATE TABLE module_scope_item_types (
    module_code    VARCHAR(50) NOT NULL,
    item_type_code VARCHAR(50) NOT NULL,
    tenant_id      BIGINT      NOT NULL DEFAULT 1,
    PRIMARY KEY (tenant_id, module_code, item_type_code)
) COMMENT='模块可用的范围项类型';

INSERT INTO module_scope_item_types (tenant_id, module_code, item_type_code) VALUES
-- student
(1, 'student', 'ORG_UNIT'), (1, 'student', 'CLASS'), (1, 'student', 'GRADE'), (1, 'student', 'MAJOR'),
-- school_class
(1, 'school_class', 'ORG_UNIT'), (1, 'school_class', 'CLASS'), (1, 'school_class', 'GRADE'), (1, 'school_class', 'MAJOR'),
-- dormitory
(1, 'dormitory_building', 'ORG_UNIT'), (1, 'dormitory_building', 'BUILDING'),
(1, 'dormitory_room', 'ORG_UNIT'), (1, 'dormitory_room', 'BUILDING'),
(1, 'dormitory_checkin', 'ORG_UNIT'), (1, 'dormitory_checkin', 'CLASS'), (1, 'dormitory_checkin', 'BUILDING'),
-- inspection
(1, 'inspection_template', 'ORG_UNIT'),
(1, 'inspection_project', 'ORG_UNIT'),
(1, 'inspection_task', 'ORG_UNIT'),
(1, 'inspection_record', 'ORG_UNIT'), (1, 'inspection_record', 'CLASS'),
(1, 'inspection_personal', 'ORG_UNIT'), (1, 'inspection_personal', 'CLASS'),
(1, 'inspection_appeal', 'ORG_UNIT'), (1, 'inspection_appeal', 'CLASS'),
(1, 'inspection_summary', 'ORG_UNIT'), (1, 'inspection_summary', 'CLASS'),
(1, 'inspection_corrective', 'ORG_UNIT'), (1, 'inspection_corrective', 'CLASS'),
-- access
(1, 'system_user', 'ORG_UNIT');

-- ============================================================
-- 7. role_data_permissions_v5 + role_data_scope_items 加 tenant_id
-- ============================================================
ALTER TABLE role_data_permissions_v5 ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
ALTER TABLE role_data_scope_items ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1;
CREATE INDEX idx_rdp_tenant ON role_data_permissions_v5(tenant_id);

-- ============================================================
-- 8. Casbin 策略表 (jcasbin jdbc-adapter 会自动创建 casbin_rule)
-- 这里提前建表确保存在
-- ============================================================
CREATE TABLE IF NOT EXISTS casbin_rule (
    id    BIGINT PRIMARY KEY AUTO_INCREMENT,
    ptype VARCHAR(100) NOT NULL DEFAULT '',
    v0    VARCHAR(100) NOT NULL DEFAULT '',
    v1    VARCHAR(100) NOT NULL DEFAULT '',
    v2    VARCHAR(100) NOT NULL DEFAULT '',
    v3    VARCHAR(100) NOT NULL DEFAULT '',
    v4    VARCHAR(100) NOT NULL DEFAULT '',
    v5    VARCHAR(100) NOT NULL DEFAULT ''
) COMMENT='Casbin 策略规则表';
