-- ============================================================
-- V20260529_2: 组织树得分 roll-up 表 (规模公平性 Stage 5)
--
-- 当前最高汇总是 insp_project_scores (项目×日期一条, 简单平均). 系统没有
-- "组织级得分" — 部门/年级/学院排名无合法聚合层, 这是"5 个班 vs 20 个班"
-- 不公平的根因.
--
-- org_unit_scores: 对一个 (project, cycle_date) 的所有已完成 submission, 按其
-- target 所属 orgUnit 分组, 沿组织树自底向上用 MEAN (均值, 不是 SUM) 逐层滚动.
--   - 叶子组织分 = 该组织下 submission 的均分 (source_count = submission 数)
--   - 父组织分   = 直接子组织分的均值       (child_count  = 子组织数)
-- 用 MEAN 是关键: 求和会让子组织多的部门吃亏, 均值让数量自动抵消.
--
-- org_unit_id 是业务主键 (按 orgUnit 存的, 本身就是主体), 由 RepositoryImpl
-- 显式 set, 不走 InspectionDataPermissionFiller 横切填充链路 — 故 PO 上的
-- orgUnitId 字段不标 @TableField(fill=INSERT).
--
-- 全部用 information_schema 条件化, 可重复执行 (与 V97 / V104 / V20260522_1 风格一致).
-- ============================================================

CREATE TABLE IF NOT EXISTS org_unit_scores (
  id            BIGINT       PRIMARY KEY,
  tenant_id     BIGINT       NOT NULL DEFAULT 1,
  project_id    BIGINT       NOT NULL,
  org_unit_id   BIGINT       NOT NULL,
  cycle_date    DATE         NOT NULL,
  score         DECIMAL(8,2) NOT NULL,
  grade         VARCHAR(32)  NULL,
  child_count   INT          NOT NULL DEFAULT 0,
  source_count  INT          NOT NULL DEFAULT 0,
  deleted       TINYINT      NOT NULL DEFAULT 0,
  created_at    DATETIME     NOT NULL,
  updated_at    DATETIME     NULL,
  UNIQUE KEY uk_org_score (tenant_id, project_id, org_unit_id, cycle_date),
  KEY idx_org_score_project_date (project_id, cycle_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
