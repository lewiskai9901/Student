-- ============================================================================
-- V20260622_2: resource_relations 加 enforce_insert_scope 列 (R8 P3-INSERT opt-in)
--
-- INSERT 授权 (新行 owner_org ∈ 用户可写组织) 改为显式 opt-in: 仅 owner_org 标注本列=1 的
-- ownership 语义资源参与 (教务记录/场所/组织), 排除 target 语义 (检查 submission 的 org_unit_id
-- =受检组织, 非创建者归属 → 不该 org-check, 否则误拦)。启动期 ResourceRelationUpserter 按
-- ResourceRelationDef.enforceInsertScope() 写回。
-- ============================================================================

ALTER TABLE resource_relations
  ADD COLUMN enforce_insert_scope TINYINT(1) NOT NULL DEFAULT 0
  COMMENT 'R8: owner_org 参与 INSERT 授权 (新行 owner_org∈可写组织, 仅 ownership 语义资源)'
  AFTER resolver_bean;
