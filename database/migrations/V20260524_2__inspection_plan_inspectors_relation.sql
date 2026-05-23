-- ============================================================
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
-- V20260524_2: 调度组检查员 — JSON 列拆为关系表
--
-- 背景: insp_inspection_plans.inspector_ids 旧为 TEXT 存 JSON array [uid1,uid2,...]
--   * 不能 FK / JOIN / INDEX, 移除 project_inspector 无法级联
--   * "查哪些调度组用了张三" 必须全表扫
--   * 数据完整性弱
--
-- 新模型: insp_plan_inspectors (plan_id, user_id) 复合主键
-- 兼容: 旧列 inspector_ids 暂保留 (NULLable), 由迁移回填新表后**改成纯输出兜底**;
--   Repository 写入路径完全走新表, 旧列仅用于第三方读取场景 + 灰度期回退;
--   2 周后用 V20260524_3 删除旧列.
-- 幂等: information_schema 守护; 多次执行无副作用.
-- ============================================================

-- Step 1: 建关系表
CREATE TABLE IF NOT EXISTS `insp_plan_inspectors` (
    `plan_id`     BIGINT       NOT NULL COMMENT '调度组 ID',
    `user_id`     BIGINT       NOT NULL COMMENT '检查员 user_id (必须属于 project_inspector)',
    `tenant_id`   BIGINT       NOT NULL DEFAULT 0,
    `org_unit_id` BIGINT       NULL COMMENT '数据权限边界, 从 plan 继承',
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`plan_id`, `user_id`, `deleted`),
    INDEX `idx_plan`  (`plan_id`),
    INDEX `idx_user`  (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='调度组检查员关系表 (替代 inspection_plans.inspector_ids JSON)';

-- Step 2: 回填旧 JSON → 新表
--   解析 inspector_ids JSON 数组, 逐 uid 写入新表; 跳过 NULL / 空数组.
--   用 JSON_TABLE (MySQL 8.0+) 把 JSON array 展开为行.
INSERT IGNORE INTO insp_plan_inspectors (plan_id, user_id, tenant_id, org_unit_id, created_at, deleted)
SELECT
    p.id        AS plan_id,
    jt.uid      AS user_id,
    COALESCE(p.tenant_id, 0),
    NULL        AS org_unit_id,
    NOW()       AS created_at,
    0           AS deleted
FROM insp_inspection_plans p
CROSS JOIN JSON_TABLE(
    COALESCE(NULLIF(p.inspector_ids, ''), '[]'),
    '$[*]' COLUMNS (uid BIGINT PATH '$')
) AS jt
WHERE p.deleted = 0
  AND p.inspector_ids IS NOT NULL
  AND p.inspector_ids <> ''
  AND p.inspector_ids <> '[]';

-- Step 3: 验证 - 输出回填行数
SELECT COUNT(*) AS plan_inspector_relations FROM insp_plan_inspectors WHERE deleted = 0;

-- Step 4: 警告 - 任何调度组的 inspector 不在 project_inspector 池子里
SELECT
    pi.plan_id,
    pi.user_id,
    p.project_id,
    '[WARN] 调度组检查员不在项目检查员池, 业务校验启用后会报错' AS msg
FROM insp_plan_inspectors pi
JOIN insp_inspection_plans p ON p.id = pi.plan_id AND p.deleted = 0
LEFT JOIN insp_project_inspectors ppi
       ON ppi.project_id = p.project_id
      AND ppi.user_id = pi.user_id
      AND ppi.is_active = 1
      AND ppi.deleted = 0
WHERE pi.deleted = 0
  AND ppi.id IS NULL;
