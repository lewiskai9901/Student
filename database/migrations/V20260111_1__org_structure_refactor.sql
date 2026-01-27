-- ================================================================
-- 组织架构重构迁移脚本
-- 版本: V20260111_1
-- 描述: 支持职能部门与教学单位分离，模块化数据权限
-- ================================================================

-- ================================================================
-- 1. 修改 org_units 表
-- ================================================================
-- 添加组织类别字段
ALTER TABLE org_units
ADD COLUMN IF NOT EXISTS unit_category VARCHAR(20) DEFAULT 'academic' COMMENT '组织类别: academic-教学单位, functional-职能部门';

-- 修改 unit_type 为更灵活的 VARCHAR 类型，支持更多类型
ALTER TABLE org_units
MODIFY COLUMN unit_type VARCHAR(30) NOT NULL DEFAULT 'department' COMMENT '组织类型';

-- 添加索引
ALTER TABLE org_units ADD INDEX idx_unit_category (unit_category);

-- ================================================================
-- 2. 创建系统模块表
-- ================================================================
CREATE TABLE IF NOT EXISTS system_modules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module_code VARCHAR(50) NOT NULL UNIQUE COMMENT '模块编码',
    module_name VARCHAR(100) NOT NULL COMMENT '模块名称',
    module_desc VARCHAR(255) COMMENT '模块描述',
    parent_code VARCHAR(50) COMMENT '父模块编码',
    icon VARCHAR(50) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '系统业务模块' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ================================================================
-- 3. 创建职能部门模块管理表
-- ================================================================
CREATE TABLE IF NOT EXISTS functional_dept_modules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    org_unit_id BIGINT NOT NULL COMMENT '职能部门ID',
    module_code VARCHAR(50) NOT NULL COMMENT '管理的模块编码',
    scope_type VARCHAR(20) DEFAULT 'all' COMMENT '管理范围: all-全校, custom-自定义',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dept_module (org_unit_id, module_code),
    FOREIGN KEY (org_unit_id) REFERENCES org_units(id) ON DELETE CASCADE
) COMMENT '职能部门管理模块配置' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ================================================================
-- 4. 创建自定义范围表
-- ================================================================
CREATE TABLE IF NOT EXISTS functional_dept_scope (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    functional_module_id BIGINT NOT NULL COMMENT 'functional_dept_modules.id',
    target_org_unit_id BIGINT NOT NULL COMMENT '目标教学单位ID',
    include_children TINYINT DEFAULT 1 COMMENT '是否包含子单位: 1-是, 0-否',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_module_target (functional_module_id, target_org_unit_id),
    FOREIGN KEY (functional_module_id) REFERENCES functional_dept_modules(id) ON DELETE CASCADE,
    FOREIGN KEY (target_org_unit_id) REFERENCES org_units(id) ON DELETE CASCADE
) COMMENT '职能部门自定义管理范围' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ================================================================
-- 5. 插入系统模块初始数据（忽略重复）
-- ================================================================
INSERT IGNORE INTO system_modules (module_code, module_name, module_desc, parent_code, sort_order) VALUES
-- 一级模块
('organization', '组织管理', '组织架构与人员管理', NULL, 1),
('dormitory', '宿舍管理', '宿舍楼栋与住宿管理', NULL, 2),
('inspection', '量化检查', '日常行为量化考核', NULL, 3),
('evaluation', '综合测评', '学生综合素质测评', NULL, 4),
('teaching', '教学管理', '课程与成绩管理', NULL, 5),
('access', '权限管理', '系统权限配置', NULL, 6),

-- organization 子模块
('organization:unit', '组织架构', '组织单元管理', 'organization', 1),
('organization:student', '学生管理', '学生信息管理', 'organization', 2),
('organization:class', '班级管理', '班级信息管理', 'organization', 3),

-- dormitory 子模块
('dormitory:building', '楼栋管理', '宿舍楼栋管理', 'dormitory', 1),
('dormitory:room', '房间管理', '宿舍房间管理', 'dormitory', 2),
('dormitory:assign', '住宿分配', '学生住宿分配', 'dormitory', 3),

-- inspection 子模块
('inspection:template', '检查模板', '量化检查模板配置', 'inspection', 1),
('inspection:record', '检查记录', '检查记录管理', 'inspection', 2),
('inspection:appeal', '申诉管理', '检查申诉处理', 'inspection', 3),
('inspection:statistics', '统计分析', '量化数据统计', 'inspection', 4),

-- evaluation 子模块
('evaluation:config', '测评配置', '测评规则配置', 'evaluation', 1),
('evaluation:score', '成绩录入', '测评成绩录入', 'evaluation', 2),
('evaluation:result', '测评结果', '测评结果查询', 'evaluation', 3),

-- teaching 子模块
('teaching:course', '课程管理', '课程信息管理', 'teaching', 1),
('teaching:schedule', '课表安排', '课程表管理', 'teaching', 2),
('teaching:grade', '成绩管理', '学生成绩管理', 'teaching', 3),

-- access 子模块
('access:user', '用户管理', '系统用户管理', 'access', 1),
('access:role', '角色管理', '系统角色管理', 'access', 2),
('access:permission', '权限管理', '系统权限管理', 'access', 3);

-- ================================================================
-- 6. 迁移现有数据
-- ================================================================
-- 将所有现有数据默认标记为教学单位
UPDATE org_units SET unit_category = 'academic' WHERE unit_category IS NULL OR unit_category = '';

-- 转换现有 unit_type 值为新格式
UPDATE org_units SET unit_type = 'school' WHERE unit_type = 'SCHOOL';
UPDATE org_units SET unit_type = 'department' WHERE unit_type IN ('COLLEGE', 'DEPARTMENT');
UPDATE org_units SET unit_type = 'class' WHERE unit_type = 'TEACHING_GROUP';

-- ================================================================
-- 7. 验证迁移结果
-- ================================================================
SELECT unit_category, unit_type, COUNT(*) as count
FROM org_units
WHERE deleted = 0
GROUP BY unit_category, unit_type
ORDER BY unit_category, unit_type;
