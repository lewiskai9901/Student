-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- V20260524_1: 检查项目 LEAD 语义化 — 把存量项目的 createdBy 回填为 LEAD
--
-- 背景: 2026-05-23 重构把装饰性 InspectorRole.LEAD 改造成真"项目负责人":
--   * 新建项目: AutoEnrollCreatorAsLeadHandler 自动写入 createdBy 为 LEAD
--   * 存量项目: 本迁移回填 — 每个项目至少 1 LEAD, 解决"创建者隐身"
--
-- 安全设计:
--   * 仅在 createdBy 用户仍存在 (users.id 有效且未删除) 时回填
--   * NOT EXISTS 守护幂等可重复执行
--   * 不覆盖已有 LEAD 关系
--   * org_unit_id 从项目继承 (与 InspectionDataPermissionFiller MetaObjectHandler 一致)
--
-- 回填后会输出告警: 哪些项目 createdBy 已不存在 (无法补 LEAD), 需 ops 后续手动指派.
-- ============================================================

-- Step 1: 回填 LEAD - createdBy 仍是有效用户的项目
INSERT INTO insp_project_inspectors (
    tenant_id, org_unit_id, project_id, user_id, user_name, role, is_active, created_at, updated_at, deleted
)
SELECT
    COALESCE(p.tenant_id, 0)    AS tenant_id,
    p.org_unit_id               AS org_unit_id,
    p.id                        AS project_id,
    p.created_by                AS user_id,
    COALESCE(u.real_name, u.username, '') AS user_name,
    'LEAD'                      AS role,
    1                           AS is_active,
    NOW()                       AS created_at,
    NOW()                       AS updated_at,
    0                           AS deleted
FROM insp_projects p
JOIN users u ON u.id = p.created_by AND u.deleted = 0
WHERE p.created_by IS NOT NULL
  AND p.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM insp_project_inspectors pi
      WHERE pi.project_id = p.id
        AND pi.user_id = p.created_by
        AND pi.role = 'LEAD'
        AND pi.deleted = 0
  );

-- Step 2: 告警 - createdBy 已离职/删除, 项目无 LEAD, 需手动处理
--   (本迁移不强行升级 INSPECTOR 为 LEAD, 让 ops 决策最合适的人选;
--    应用层 InspProjectAuthorizationGuard.ensureProjectHasLead 也会做最后兜底)
SELECT
    p.id           AS project_id,
    p.project_code AS project_code,
    p.project_name AS project_name,
    p.created_by   AS createdBy_missing,
    '[WARN] 项目 createdBy 用户不存在, 未回填 LEAD - 请管理员手动指派' AS msg
FROM insp_projects p
LEFT JOIN users u ON u.id = p.created_by AND u.deleted = 0
WHERE p.created_by IS NOT NULL
  AND p.deleted = 0
  AND u.id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM insp_project_inspectors pi
      WHERE pi.project_id = p.id
        AND pi.role = 'LEAD'
        AND pi.is_active = 1
        AND pi.deleted = 0
  );

-- Step 3: 验证 - 每个 active 项目应至少 1 LEAD (允许 createdBy 缺失的告警项目暂时无 LEAD)
SELECT
    p.id           AS project_id,
    p.project_code AS project_code,
    COUNT(pi.id)   AS lead_count,
    CASE WHEN COUNT(pi.id) = 0 THEN '[CHECK] 项目无 LEAD' ELSE '[OK]' END AS status
FROM insp_projects p
LEFT JOIN insp_project_inspectors pi
       ON pi.project_id = p.id
      AND pi.role = 'LEAD'
      AND pi.is_active = 1
      AND pi.deleted = 0
WHERE p.deleted = 0
GROUP BY p.id, p.project_code
HAVING COUNT(pi.id) = 0;
