-- V15.0.0: 删除 users 表历史遗留字段
-- org_unit_id → 已由 user_org_relations 表替代
-- class_id → 已由 Student 领域 / UserOrgRelation 替代
-- user_type → 已由 user_type_code (引用 user_types 表) 替代

ALTER TABLE users DROP COLUMN org_unit_id, DROP COLUMN class_id, DROP COLUMN user_type;
