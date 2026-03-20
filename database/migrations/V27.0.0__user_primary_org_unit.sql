-- V27.0.0: 用户主组织归属字段重构
-- 将 users.primary_org_relation_id (指向 access_relations 行) 替换为 primary_org_unit_id (直接指向 org_units)

-- 1. 添加新列
ALTER TABLE users ADD COLUMN primary_org_unit_id BIGINT NULL COMMENT '主归属组织ID' AFTER identity_card;

-- 2. 从 access_relations 迁移数据
UPDATE users u
  JOIN access_relations ar ON ar.subject_id = u.id
    AND ar.subject_type = 'user'
    AND ar.resource_type = 'org_unit'
    AND ar.relation = 'member'
    AND JSON_EXTRACT(ar.metadata, '$.isPrimary') IN (TRUE, 1)
    AND ar.deleted = 0
SET u.primary_org_unit_id = ar.resource_id
WHERE u.deleted = 0 AND u.primary_org_unit_id IS NULL;

-- 3. 删除旧列
ALTER TABLE users DROP COLUMN primary_org_relation_id;

-- 4. 添加索引
CREATE INDEX idx_users_primary_org_unit ON users(primary_org_unit_id);
