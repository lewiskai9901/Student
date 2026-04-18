-- ============================================================
-- 权限体系单轨化: DROP 老权限表
--
-- 前置条件:
--   - V20260419_4 已把 role_data_permissions_v5 的 34 行迁移到 role_data_scopes
--   - V20260419_4 已把 data_modules 的 12 条补齐到 data_resources
--   - 代码已全部改为指向 role_data_scopes / data_resources (W2-3 / W2-4)
--
-- DROP 的表:
--   role_data_permissions_v5  (v5 角色数据权限, 34 行已迁走)
--   role_data_scope_items     (v5 CUSTOM 明细, 0 行, v3 用 JSON 列代替)
--   data_modules              (v2 模块配置, 12 行已迁走)
--   scope_item_types          (v2 范围项类型, 代码不再读)
--   module_scope_item_types   (v2 模块-范围映射, 代码不再读)
--   data_scope_types          (v2 范围类型字典, 代码不再读)
-- ============================================================

-- role_data_scope_items 有 FK 指向 role_data_permissions_v5, 关掉 FK check 后顺序 DROP
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS role_data_scope_items;
DROP TABLE IF EXISTS role_data_permissions_v5;
DROP TABLE IF EXISTS data_modules;
DROP TABLE IF EXISTS scope_item_types;
DROP TABLE IF EXISTS module_scope_item_types;
DROP TABLE IF EXISTS data_scope_types;

SET FOREIGN_KEY_CHECKS = 1;
