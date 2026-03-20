-- V19.0.0 用户类型数据约束字段重命名
-- requires_class -> requires_org (关联组织)
-- requires_dormitory -> requires_place (关联场所)

ALTER TABLE user_types CHANGE COLUMN requires_class requires_org TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要组织归属';
ALTER TABLE user_types CHANGE COLUMN requires_dormitory requires_place TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否需要场所归属';
