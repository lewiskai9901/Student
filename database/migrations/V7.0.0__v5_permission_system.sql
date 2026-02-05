-- =============================================================================
-- V5 权限系统重构 - 数据库迁移脚本
-- 版本: 7.0.0
-- 日期: 2026-01-31
-- 说明: 创建V5权限系统所需的配置表
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. 数据范围类型配置表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS data_scope_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scope_code VARCHAR(50) NOT NULL COMMENT '范围代码',
    scope_name VARCHAR(100) NOT NULL COMMENT '范围名称',
    scope_level INT DEFAULT 0 COMMENT '优先级(越大越高)',
    calc_type VARCHAR(50) NOT NULL COMMENT '计算类型: NONE/USER_ORG/USER_ORG_TREE/CUSTOM_CONFIG/CREATOR',
    description VARCHAR(500) COMMENT '描述',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_scope_code (scope_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据范围类型配置表';

-- 初始化数据范围类型
INSERT INTO data_scope_types (scope_code, scope_name, scope_level, calc_type, description, sort_order) VALUES
('ALL', '全部数据', 100, 'NONE', '可访问系统中所有数据', 1),
('DEPARTMENT_AND_BELOW', '本部门及下级', 80, 'USER_ORG_TREE', '可访问用户所属部门及下级部门的数据', 2),
('DEPARTMENT', '仅本部门', 60, 'USER_ORG', '只能访问用户所属部门的数据', 3),
('CUSTOM', '自定义范围', 40, 'CUSTOM_CONFIG', '管理员配置的指定范围', 4),
('SELF', '仅本人', 20, 'CREATOR', '只能访问自己创建或负责的数据', 5)
ON DUPLICATE KEY UPDATE scope_name = VALUES(scope_name), description = VALUES(description);

-- -----------------------------------------------------------------------------
-- 2. 数据模块配置表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS data_modules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module_code VARCHAR(50) NOT NULL COMMENT '模块代码',
    module_name VARCHAR(100) NOT NULL COMMENT '模块名称',
    domain_code VARCHAR(50) COMMENT '领域代码',
    domain_name VARCHAR(100) COMMENT '领域名称',
    filter_fields JSON COMMENT '过滤字段配置 {"org_unit":"org_unit_id","class":"class_id"}',
    main_table VARCHAR(100) COMMENT '主表名',
    enable_data_permission TINYINT DEFAULT 1 COMMENT '是否启用数据权限',
    description VARCHAR(500) COMMENT '描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_module_code (module_code),
    INDEX idx_domain_code (domain_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据模块配置表';

-- 初始化数据模块
INSERT INTO data_modules (module_code, module_name, domain_code, domain_name, filter_fields, main_table, sort_order) VALUES
-- Organization 领域
('student', '学生信息', 'organization', '组织管理', '{"org_unit":"org_unit_id","class":"class_id","creator":"created_by"}', 'students', 101),
('school_class', '班级管理', 'organization', '组织管理', '{"org_unit":"org_unit_id"}', 'classes', 102),
('org_unit', '组织架构', 'organization', '组织管理', '{"org_unit":"id"}', 'org_units', 103),
-- Space 领域
('dormitory_building', '楼栋管理', 'space', '场所管理', '{"org_unit":"org_unit_id"}', 'space', 201),
('dormitory_room', '房间管理', 'space', '场所管理', '{"org_unit":"org_unit_id","building":"building_id"}', 'space', 202),
('dormitory_checkin', '入住管理', 'space', '场所管理', '{"org_unit":"org_unit_id","class":"class_id"}', 'space_occupants', 203),
-- Inspection 领域
('inspection_template', '检查模板', 'inspection', '量化检查', '{"org_unit":"org_unit_id","creator":"created_by"}', 'inspection_templates', 301),
('inspection_project', '检查项目', 'inspection', '量化检查', '{"org_unit":"org_unit_id","creator":"created_by"}', 'inspection_projects', 302),
('inspection_task', '检查任务', 'inspection', '量化检查', '{"org_unit":"org_unit_id"}', 'inspection_tasks', 303),
('inspection_record', '检查记录', 'inspection', '量化检查', '{"org_unit":"org_unit_id","class":"class_id"}', 'target_inspection_records', 304),
('inspection_personal', '个人检查记录', 'inspection', '量化检查', '{"student":"target_id"}', 'target_inspection_records', 305),
('inspection_appeal', '申诉管理', 'inspection', '量化检查', '{"org_unit":"org_unit_id","class":"class_id","creator":"created_by"}', 'appeals', 306),
('inspection_summary', '检查汇总', 'inspection', '量化检查', '{"org_unit":"org_unit_id","class":"class_id"}', 'daily_summaries', 307),
('inspection_corrective', '整改管理', 'inspection', '量化检查', '{"org_unit":"org_unit_id","class":"class_id"}', 'corrective_orders', 308),
-- Access 领域
('system_user', '用户管理', 'access', '权限管理', '{"org_unit":"org_unit_id"}', 'users', 401),
('system_role', '角色管理', 'access', '权限管理', NULL, 'roles', 402)
ON DUPLICATE KEY UPDATE module_name = VALUES(module_name), filter_fields = VALUES(filter_fields);

-- -----------------------------------------------------------------------------
-- 3. 自定义范围项类型配置表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scope_item_types (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_type_code VARCHAR(50) NOT NULL COMMENT '类型代码',
    item_type_name VARCHAR(100) NOT NULL COMMENT '类型名称',
    ref_table VARCHAR(100) COMMENT '引用表名',
    ref_id_field VARCHAR(100) COMMENT '引用ID字段',
    ref_name_field VARCHAR(100) COMMENT '引用名称字段',
    ref_parent_field VARCHAR(100) COMMENT '引用父级字段(树形结构)',
    applicable_modules JSON COMMENT '适用的模块列表',
    filter_field_key VARCHAR(50) COMMENT '过滤字段键名',
    support_children TINYINT DEFAULT 0 COMMENT '是否支持包含子级',
    sort_order INT DEFAULT 0 COMMENT '排序',
    is_enabled TINYINT DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_item_type_code (item_type_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自定义范围项类型配置表';

-- 初始化范围项类型
INSERT INTO scope_item_types (item_type_code, item_type_name, ref_table, ref_id_field, ref_name_field, ref_parent_field, applicable_modules, filter_field_key, support_children, sort_order) VALUES
('ORG_UNIT', '部门', 'org_units', 'id', 'name', 'parent_id', '["student","school_class","inspection_record","inspection_template","system_user"]', 'org_unit', 1, 1),
('CLASS', '班级', 'classes', 'id', 'class_name', NULL, '["student","inspection_record","dormitory_checkin"]', 'class', 0, 2),
('GRADE', '年级', 'grades', 'id', 'name', NULL, '["student","school_class"]', 'grade', 0, 3),
('BUILDING', '楼栋', 'space', 'id', 'space_name', NULL, '["dormitory_room","dormitory_checkin"]', 'building', 0, 4),
('MAJOR', '专业', 'majors', 'id', 'name', NULL, '["student","school_class"]', 'major', 0, 5)
ON DUPLICATE KEY UPDATE item_type_name = VALUES(item_type_name), applicable_modules = VALUES(applicable_modules);

-- -----------------------------------------------------------------------------
-- 4. 角色-数据权限配置表 (V5版本，如果不存在则创建)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role_data_permissions_v5 (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    module_code VARCHAR(50) NOT NULL COMMENT '模块代码',
    scope_code VARCHAR(50) NOT NULL COMMENT '数据范围代码',
    description VARCHAR(200) COMMENT '描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_module (role_id, module_code),
    INDEX idx_role_id (role_id),
    CONSTRAINT fk_rdp_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色-数据权限配置表(V5)';

-- -----------------------------------------------------------------------------
-- 5. 自定义数据范围明细表
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role_data_scope_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_data_permission_id BIGINT NOT NULL COMMENT '角色数据权限ID',
    item_type_code VARCHAR(50) NOT NULL COMMENT '范围项类型代码',
    scope_id BIGINT NOT NULL COMMENT '范围ID(部门/班级/楼栋等的ID)',
    scope_name VARCHAR(100) COMMENT '范围名称(冗余存储)',
    include_children TINYINT DEFAULT 0 COMMENT '是否包含子级',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_permission_scope (role_data_permission_id, item_type_code, scope_id),
    INDEX idx_role_data_permission_id (role_data_permission_id),
    CONSTRAINT fk_rdsi_permission FOREIGN KEY (role_data_permission_id) REFERENCES role_data_permissions_v5(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自定义数据范围明细表';

-- -----------------------------------------------------------------------------
-- 6. 为超级管理员角色初始化数据权限(ALL)
-- -----------------------------------------------------------------------------
INSERT INTO role_data_permissions_v5 (role_id, module_code, scope_code, description)
SELECT r.id, dm.module_code, 'ALL', CONCAT('超级管理员-', dm.module_name)
FROM roles r
CROSS JOIN data_modules dm
WHERE r.role_code = 'SUPER_ADMIN' AND dm.is_enabled = 1
ON DUPLICATE KEY UPDATE scope_code = 'ALL';

-- =============================================================================
-- 迁移完成
-- =============================================================================
