-- =============================================
-- V8.3.0: 用户模块改造 - 集成类型系统和多归属关系
-- =============================================

-- 1. 添加用户类型编码字段（引用 user_types 表）
ALTER TABLE users
ADD COLUMN user_type_code VARCHAR(50) COMMENT '用户类型编码（引用user_types）' AFTER user_type;

-- 2. 添加主组织关系ID字段（引用 user_org_relations 表）
ALTER TABLE users
ADD COLUMN primary_org_relation_id BIGINT COMMENT '主组织关系ID（引用user_org_relations）' AFTER org_unit_id;

-- 3. 添加索引
ALTER TABLE users
ADD INDEX idx_user_type_code (user_type_code);

-- 4. 数据迁移：将旧的 user_type 整数值转换为 user_type_code
UPDATE users SET user_type_code = 'ADMIN' WHERE user_type = 1;
UPDATE users SET user_type_code = 'TEACHER' WHERE user_type = 2;
UPDATE users SET user_type_code = 'STUDENT' WHERE user_type = 3;
-- 其他类型默认为 TEACHER
UPDATE users SET user_type_code = 'TEACHER' WHERE user_type_code IS NULL OR user_type_code = '';

-- 5. 将现有的 org_unit_id 迁移到 user_org_relations 表
-- 为每个有 org_unit_id 的用户创建主归属关系
INSERT INTO user_org_relations (user_id, org_unit_id, relation_type, is_primary, can_manage, can_approve, weight_ratio, created_by)
SELECT
    id,
    org_unit_id,
    'PRIMARY',
    TRUE,
    CASE WHEN user_type = 1 THEN TRUE ELSE FALSE END,  -- 管理员有管理权限
    CASE WHEN user_type = 1 THEN TRUE ELSE FALSE END,  -- 管理员有审批权限
    100.00,
    1
FROM users
WHERE org_unit_id IS NOT NULL AND deleted = 0
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- 6. 更新 primary_org_relation_id
UPDATE users u
INNER JOIN user_org_relations r ON u.id = r.user_id AND r.is_primary = 1 AND r.deleted = 0
SET u.primary_org_relation_id = r.id
WHERE u.deleted = 0;

-- 7. 如果有班级信息，也创建对应的用户-场所关系
-- 注意：这里假设 class_id 对应的是 spaces 表中的记录
-- 需要根据实际情况调整
-- INSERT INTO user_space_relations (user_id, space_id, relation_type, is_primary, ...)
-- SELECT u.id, u.class_id, 'ASSIGNED', TRUE, ...
-- FROM users u WHERE u.class_id IS NOT NULL AND u.deleted = 0;
