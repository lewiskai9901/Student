-- ============================================================
-- Basic Modules Refactor v3 — Phase 2: 破坏性变更
--   1) DROP 已确认无代码引用的 zombie 表 (18 张)
--   2) DROP 已确认无代码引用的字段:
--      - org_units.leader_id / deputy_leader_ids (grep 全 0 引用)
--   3) 规范化剩余结构
--
-- 保留决策 (v3 修正):
--   - users.primary_org_unit_id   保留作为主归属快捷字段 (45 处代码引用,性能缓存)
--   - places.org_unit_id          保留,场所主组织快捷字段
--   - places.responsible_user_id  保留,场所主责人快捷字段
--   - casbin_rule                 保留空表 (jCasbin 库启动依赖)
--   - domain_events/activity_events 保留 (DDD store + 审计日志,各有用途)
--   - classrooms/grades/majors/major_directions 保留 (教务业务表)
--
-- 执行前确保代码已验证:
--   - DashboardReadModel.java:157 已改走 access_relations (Day 3 完成)
--   - RoleDataPermissionPO/Mapper 已删除 (Day 3 完成)
-- ============================================================

-- ------------------------------------------------------------
-- Step 1: DROP org_units 无引用字段
-- ------------------------------------------------------------
ALTER TABLE org_units DROP COLUMN leader_id;
ALTER TABLE org_units DROP COLUMN deputy_leader_ids;

-- ------------------------------------------------------------
-- Step 2: DROP 18 张 zombie 表
-- ------------------------------------------------------------
-- 老关系表 (access_relations 已取代, 数据为空)
DROP TABLE IF EXISTS user_org_relations;
DROP TABLE IF EXISTS user_org_relation_history;
DROP TABLE IF EXISTS user_place_relations;
DROP TABLE IF EXISTS user_place_relation_history;
DROP TABLE IF EXISTS user_departments;
DROP TABLE IF EXISTS user_positions;
DROP TABLE IF EXISTS place_org_relations;
DROP TABLE IF EXISTS place_org_relation_history;

-- 老权限 scope 表 (0 引用)
DROP TABLE IF EXISTS user_data_scopes;
DROP TABLE IF EXISTS user_scope_assignments;
DROP TABLE IF EXISTS role_custom_scope;
DROP TABLE IF EXISTS data_permissions;
DROP TABLE IF EXISTS scope_metadata;
DROP TABLE IF EXISTS functional_dept_scope;

-- 老 place 扩展 (0 引用)
DROP TABLE IF EXISTS place_classroom_ext;
DROP TABLE IF EXISTS place_dormitory_ext;
DROP TABLE IF EXISTS place_lab_ext;
DROP TABLE IF EXISTS place_office_ext;
DROP TABLE IF EXISTS place_occupant;  -- 单数 place_occupant,不是活跃的 place_occupants

-- 其他 zombie
DROP TABLE IF EXISTS notification_records;

-- ------------------------------------------------------------
-- Step 3: 检查 RoleDataPermission 旧数据
--   role_data_permissions (58 行) 的 Java PO/Mapper 已删 (Day 3),
--   但表里数据还在。可以直接 DROP 表,或保留作为只读归档。
--   DECISION: DROP (因为 code 读写路径已全部走 _v5)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS role_data_permissions;

-- ============================================================
-- Phase 2 结束。access_relations 补录 relation 数据、
-- role_data_permissions_v5 -> role_data_scopes 迁移、
-- 等相关动作留给 Phase 3 完成。
-- ============================================================
