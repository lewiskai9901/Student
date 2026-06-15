-- T9: 删除 role_data_scopes 的 legacy 扁平范围列。
-- 真相已全部迁到可组合三轴列 (V20260615_2):
--   scope_type          → org_anchor / anchor_param / include_subtree
--   custom_org_unit_ids → custom_org_ids
-- 代码 (DataPermissionPolicyService) 已在 T9 停止读写这两列, scopeCode 改由轴① 反推。
-- 让已升级到 baseline_v3 的开发库追上 baseline。新库直接由 baseline_v3.sql 建出 (已无此两列)。

ALTER TABLE `role_data_scopes`
  DROP COLUMN `scope_type`,
  DROP COLUMN `custom_org_unit_ids`;
