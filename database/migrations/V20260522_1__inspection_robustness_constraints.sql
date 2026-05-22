-- ============================================================
-- V20260522_1: 检查平台健壮性约束
--
-- 本轮"检查平台应用层健壮性整改"配套的 DB 约束. 全部用 information_schema
-- 条件化, 可重复执行 (与 V97 / V104 / V108 风格一致).
--
-- 加的约束:
--   1. insp_tasks (source_ref_type, source_ref_id) 唯一索引 — 触发任务去重
--      (createTriggeredTask 去重查询与插入间存在竞态, 靠 DB 唯一索引兜底,
--       应用层 catch DuplicateKeyException 优雅跳过).
--      注: MySQL 唯一索引允许多个 NULL, 因此 SCHEDULED 等无 source_ref 的任务不受影响.
--   2. insp_project_scores (tenant_id, project_id, cycle_date) 唯一索引 —
--      ScoreAggregationService.recomputeProjectScore 并发下可能为同一
--      (project, cycle_date) 插入两行, 唯一索引保证 upsert 语义.
--
-- 业务编号 (task_code / project_code / appeal_code / case_code) 的唯一索引
-- 已分别在 V32 (uk_task_code / uk_project_code) / V33 (uk_case_code) /
-- V20260427_3 (uk_appeal_code) 建立, 本迁移不再重复.
--
-- 存量重复数据说明: 本项目开发阶段不考虑旧数据兼容 (见 MEMORY.md "开发原则").
-- 若已有库 insp_tasks / insp_project_scores 存在违反唯一性的重复行, 下面的
-- CREATE UNIQUE INDEX 会失败 — 此时需先人工清理重复行 (开发库可直接 TRUNCATE
-- 相关表或删重复行) 再重跑本迁移.
-- ============================================================

-- ------------------------------------------------------------
-- 1) insp_tasks (source_ref_type, source_ref_id) 唯一索引
--    先删旧的非唯一 idx_source_ref (V108 建), 再建唯一索引.
-- ------------------------------------------------------------
SET @idx := (SELECT COUNT(*) FROM information_schema.statistics
             WHERE table_schema = DATABASE()
               AND table_name = 'insp_tasks'
               AND index_name = 'idx_source_ref');
SET @sql := IF(@idx > 0,
  'ALTER TABLE insp_tasks DROP INDEX idx_source_ref',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @uk := (SELECT COUNT(*) FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = 'insp_tasks'
              AND index_name = 'uk_insp_task_source_ref');
SET @sql := IF(@uk = 0,
  'CREATE UNIQUE INDEX uk_insp_task_source_ref ON insp_tasks (source_ref_type, source_ref_id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2) insp_project_scores (tenant_id, project_id, cycle_date) 唯一索引
--    保留原 idx_project_date (查询用), 额外加唯一索引保证一日一行.
-- ------------------------------------------------------------
SET @uk2 := (SELECT COUNT(*) FROM information_schema.statistics
             WHERE table_schema = DATABASE()
               AND table_name = 'insp_project_scores'
               AND index_name = 'uk_insp_project_score_cycle');
SET @sql := IF(@uk2 = 0,
  'CREATE UNIQUE INDEX uk_insp_project_score_cycle ON insp_project_scores (tenant_id, project_id, cycle_date)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 3) insp_submission_details 加 appeal_adjusted_at 列
--    SubmissionDetail.applyAppealAdjustment 用此列做幂等标记 —
--    非空表示该 detail 已应用过申诉调整, 防 AppealApprovedEvent
--    重复投递把同一笔退分叠加多次.
-- ------------------------------------------------------------
SET @col := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema = DATABASE()
               AND table_name = 'insp_submission_details'
               AND column_name = 'appeal_adjusted_at');
SET @sql := IF(@col = 0,
  'ALTER TABLE insp_submission_details ADD COLUMN appeal_adjusted_at DATETIME NULL COMMENT ''申诉调整幂等标记: 非空表示已应用过申诉调整'' AFTER updated_at',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
