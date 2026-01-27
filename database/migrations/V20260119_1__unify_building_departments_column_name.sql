-- 统一列名: 将 department_id 改为 org_unit_id
-- 保持命名规范一致性

-- 1. building_departments 表
ALTER TABLE building_departments
CHANGE COLUMN department_id org_unit_id BIGINT NOT NULL COMMENT '组织单元ID';

-- 2. building_department_assignments 表
ALTER TABLE building_department_assignments
CHANGE COLUMN department_id org_unit_id BIGINT NOT NULL COMMENT '组织单元ID';
