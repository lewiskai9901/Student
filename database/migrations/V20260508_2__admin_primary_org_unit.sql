-- V20260508_2 (OBSOLETE / NO-OP)
--
-- 历史意图: 给 admin 用户补 users.primary_org_unit_id (指向根组织), 让 inspection
--   创建路径 MetaObjectHandler 走 strategy=context 分支, missed 信号回归真实漏率监控。
--
-- 现已作废: 组织归属唯一真相源统一为 access_relations 的 member 关系
--   (V20260531_4 已删 users.primary_org_unit_id 列)。admin 的归属若需要,
--   由 seed (init_data.sql / ci_e2e_seed.sql) 写 member 关系承载。该 UPDATE 引用已删列, 改为 no-op。
SELECT 'V20260508_2 obsolete: admin membership via access_relations member' AS msg;
