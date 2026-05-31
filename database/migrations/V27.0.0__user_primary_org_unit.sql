-- V27.0.0 (OBSOLETE / NO-OP)
--
-- 历史意图: 把 users.primary_org_relation_id 替换为直连列 users.primary_org_unit_id,
--   并从 access_relations 回填、建 idx_users_primary_org_unit 索引。
--
-- 现已作废: 组织归属唯一真相源统一为 access_relations 的 member 关系
--   (V20260531_3 唯一约束 + V20260531_4 删 primary_org_unit_id 列)。
--   该迁移引用的 primary_org_unit_id / primary_org_relation_id 列在当前 baseline 中均不存在,
--   原 ALTER 语句若执行会报列不存在。故整体改为 no-op。
SELECT 'V27.0.0 obsolete: user membership unified to access_relations member' AS msg;
