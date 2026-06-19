-- ============================================================================
-- V20260620_2: 统一锚定 R3a-2b Step B — 删 role_data_scopes 死的 M1 轴①列
--
-- 轴① 真相源已是 relation_grants (V20260620_1 回填 + R3a-2b 读写改用):
--   - 读: getScopeSpec/mapToScopeSpec/mapToPermission 由 relation_grants 反推轴① (RelationGrant.anchorOf)
--   - 写: saveRolePermission / RoleScopeBindingRegistrar 只写 relation_grants
-- 故 org_anchor/anchor_param/include_subtree/custom_org_ids 4 列已无读无写 (Step A 双写不读 → Step B 单写), 删之。
--
-- 轴② (subject_rel_include/exclude) / 轴③ (type_filter) 仍是独立列, 保留。
-- ============================================================================

ALTER TABLE `role_data_scopes`
  DROP COLUMN `org_anchor`,
  DROP COLUMN `anchor_param`,
  DROP COLUMN `include_subtree`,
  DROP COLUMN `custom_org_ids`;
