-- =============================================
-- V8.3.2: 组织模块改造 - 集成类型系统
-- =============================================

-- 1. 添加组织类型编码字段（引用 org_types 表）
ALTER TABLE org_units
ADD COLUMN type_code VARCHAR(50) COMMENT '组织类型编码（引用org_types）' AFTER unit_type;

-- 2. 添加索引
ALTER TABLE org_units
ADD INDEX idx_type_code (type_code);

-- 3. 数据迁移：将旧的 unit_type 转换为 type_code
UPDATE org_units SET type_code = 'SCHOOL' WHERE unit_type = 'SCHOOL';
UPDATE org_units SET type_code = 'COLLEGE' WHERE unit_type = 'COLLEGE';
UPDATE org_units SET type_code = 'DEPARTMENT' WHERE unit_type = 'DEPARTMENT';
UPDATE org_units SET type_code = 'TEACHING_GROUP' WHERE unit_type = 'TEACHING_GROUP';
UPDATE org_units SET type_code = 'GRADE' WHERE unit_type = 'GRADE';
UPDATE org_units SET type_code = 'CLASS' WHERE unit_type = 'CLASS';
-- 其他类型默认为 DEPARTMENT
UPDATE org_units SET type_code = 'DEPARTMENT' WHERE type_code IS NULL OR type_code = '';
