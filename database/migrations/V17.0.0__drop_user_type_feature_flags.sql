-- V17.0.0 移除用户类型特性标志（canXxx字段）
-- 这些功能由 RBAC 角色权限系统统一管理，不再需要在用户类型上冗余定义

ALTER TABLE user_types DROP COLUMN can_login;
ALTER TABLE user_types DROP COLUMN can_be_inspector;
ALTER TABLE user_types DROP COLUMN can_be_inspected;
ALTER TABLE user_types DROP COLUMN can_manage_org;
ALTER TABLE user_types DROP COLUMN can_view_reports;
