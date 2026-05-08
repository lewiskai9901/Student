-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- V20260508_2: 给 admin 用户补 primary_org_unit_id (避免 InspectionDataPermissionFiller
-- 兜底走 SecurityContext 时拿到 NULL → strategy=missed 误报漏率信号).
--
-- 现状: admin (id=1) 的 primary_org_unit_id 为 NULL. 创建 inspection 项目
-- 不显式传 orgUnitId 时, MetaObjectHandler 走 SecurityContext.getOrgUnitId()
-- 兜底也是 NULL → 计入 missed counter, 让生产监控信号失真.
--
-- 兜底逻辑: 把 admin 关联到根组织 (parent_id IS NULL 的第一个 org_unit), 让
-- 创建路径走 strategy=context 分支, missed 信号回归真实漏率监控.
--
-- 幂等条件化: 仅当 admin.primary_org_unit_id IS NULL 时才 update.
-- ============================================================

UPDATE users
SET primary_org_unit_id = (
    SELECT id FROM (SELECT id FROM org_units WHERE deleted = 0 AND parent_id IS NULL ORDER BY id LIMIT 1) AS root
)
WHERE username = 'admin'
  AND primary_org_unit_id IS NULL
  AND EXISTS (SELECT 1 FROM org_units WHERE deleted = 0 AND parent_id IS NULL);

-- 验证 (跑后手动执行):
-- SELECT id, username, primary_org_unit_id FROM users WHERE username = 'admin';
