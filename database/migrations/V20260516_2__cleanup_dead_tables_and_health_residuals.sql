-- ============================================================================
-- V20260516_2: 清死表 + HEALTH 插件残留
-- ============================================================================
-- 真完美架构里, 设计但 0 使用 = 噪音. 这次集中清:
--
-- 1) 用户类型扩展表 (3 张, 0 行, 已被 entity_type_configs + entity_attribute_values 替代):
--    user_parent / user_counselor / user_staff
--    保留: user_student (583 行) / user_teacher (3 行) — 有真实数据
--
-- 2) 场所子类型扩展表 (4 张, 0 行, 已被 places.attributes JSON 替代):
--    place_classroom / place_office / place_dorm_room / place_lab
--
-- 3) 场所组织 N:N 关联表 (1 张, 0 行, 已被 places.org_unit_id 直接关联替代):
--    place_org_assignment
--
-- 4) 通用审计表 (1 张, 0 行, 已被 activity_events + permission_audit_log + 业务专用审计表替代):
--    audit_logs (V36.0.0 也曾试图删, 但显然未生效)
--
-- 5) HEALTH 插件残留 (V20260509_7 删了插件代码但数据残留):
--    - 5 个 roles (DOCTOR / NURSE / WARD_HEAD 等)
--    - 10 个 permissions
--    - 2 个 data_scope_dims (BY_ATTENDING_DOCTOR / BY_WARD) — 而且 Resolver class 都不存在
--    - 4 个 entity_type_configs (PATIENT / DOCTOR / NURSE / WARD_HEAD 等)
--
-- 所有 DROP 都已 grep 验证零 Java 引用. 幂等 DROP IF EXISTS.
-- ============================================================================

-- 1) 用户类型扩展死表
DROP TABLE IF EXISTS user_parent;
DROP TABLE IF EXISTS user_counselor;
DROP TABLE IF EXISTS user_staff;

-- 2) 场所子类型死表
DROP TABLE IF EXISTS place_classroom;
DROP TABLE IF EXISTS place_office;
DROP TABLE IF EXISTS place_dorm_room;
DROP TABLE IF EXISTS place_lab;

-- 3) 场所组织 N:N 死表
DROP TABLE IF EXISTS place_org_assignment;

-- 4) 通用审计死表
DROP TABLE IF EXISTS audit_logs;

-- 5) HEALTH 插件残留清理

-- 5a) 删 HEALTH industry roles (DOCTOR / NURSE / WARD_HEAD)
DELETE rds FROM role_data_scopes rds
JOIN roles r ON r.id = rds.role_id
WHERE r.industry = 'HEALTH';

DELETE rp FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
WHERE r.industry = 'HEALTH';

DELETE ur FROM user_roles ur
JOIN roles r ON r.id = ur.role_id
WHERE r.industry = 'HEALTH';

DELETE FROM roles WHERE industry = 'HEALTH';

-- 5b) 删 HEALTH industry permissions
DELETE FROM permissions WHERE industry = 'HEALTH';

-- 5c) 删 HEALTH industry data_scope_dims
DELETE FROM data_scope_dims WHERE industry = 'HEALTH';

-- 5d) 删 HEALTH industry entity_type_configs
DELETE FROM entity_type_configs WHERE industry = 'HEALTH';

-- 5e) 删 HEALTH industry data_resources
DELETE FROM data_resources WHERE industry = 'HEALTH';

-- 5f) 删 HEALTH industry relation_types
DELETE FROM relation_types WHERE COALESCE(industry, '') = 'HEALTH';
