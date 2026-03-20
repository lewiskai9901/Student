-- V35.0.0 - 组织类型添加岗位模板字段
-- 在 org_unit_types 表添加 default_positions JSON 列，存储该类型的默认岗位模板

ALTER TABLE org_unit_types
    ADD COLUMN default_positions JSON DEFAULT NULL
    COMMENT '默认岗位模板 JSON数组' AFTER default_place_type_codes;
