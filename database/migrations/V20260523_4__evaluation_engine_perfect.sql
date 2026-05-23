-- ============================================================
-- V20260523_4: 评级引擎完美架构 (Batch 1 + Batch 2 全套)
--
-- 1) Indicator 升级: 单 sectionId → sourceSectionIds 数组, 加 triggerMode
--    (TIME/COUNT/MANUAL), policies, weights, rank direction
-- 2) 新表 indicator_results: 评级结果实体化 + 版本化快照
-- 3) 撤销 inspection_plans.scoring_profile_id
-- 4) 撤销 insp_projects.default_scoring_profile_id
--
-- 全部 information_schema 条件化, 可重复执行.
-- ============================================================

-- ============================================================
-- 1. Indicator 表升级
-- ============================================================

-- 1.1 source_section_ids JSON (新, 取代单一 source_section_id)
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema=DATABASE() AND table_name='insp_indicators'
             AND column_name='source_section_ids');
SET @sql := IF(@c=0,
  'ALTER TABLE insp_indicators ADD COLUMN source_section_ids JSON NULL COMMENT ''跨分区组合: 单分区=[id], 多分区=[id,id,...]'' AFTER source_section_id',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.2 trigger_mode (TIME_WINDOW / COUNT / MANUAL)
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema=DATABASE() AND table_name='insp_indicators'
             AND column_name='trigger_mode');
SET @sql := IF(@c=0,
  'ALTER TABLE insp_indicators ADD COLUMN trigger_mode VARCHAR(20) NOT NULL DEFAULT ''TIME_WINDOW'' COMMENT ''触发模式: TIME_WINDOW/COUNT/MANUAL''',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.3 count_threshold (COUNT 模式用)
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema=DATABASE() AND table_name='insp_indicators'
             AND column_name='count_threshold');
SET @sql := IF(@c=0,
  'ALTER TABLE insp_indicators ADD COLUMN count_threshold INT NULL COMMENT ''COUNT 模式: 累计 N 次触发一次评级''',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.4 weights_by_section (跨分区加权)
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema=DATABASE() AND table_name='insp_indicators'
             AND column_name='weights_by_section');
SET @sql := IF(@c=0,
  'ALTER TABLE insp_indicators ADD COLUMN weights_by_section JSON NULL COMMENT ''跨分区加权: {section_id: weight}, 不填等权''',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.5 rank_direction (ASC=越小越好 / DESC=越大越好)
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema=DATABASE() AND table_name='insp_indicators'
             AND column_name='rank_direction');
SET @sql := IF(@c=0,
  'ALTER TABLE insp_indicators ADD COLUMN rank_direction VARCHAR(4) NULL COMMENT ''排名方向: ASC/DESC, NULL=不排名(值映射等级)''',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.6 late_policy (补做归属策略)
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema=DATABASE() AND table_name='insp_indicators'
             AND column_name='late_policy');
SET @sql := IF(@c=0,
  'ALTER TABLE insp_indicators ADD COLUMN late_policy VARCHAR(20) NOT NULL DEFAULT ''REVISE_ORIGINAL'' COMMENT ''逾期补做策略: REVISE_ORIGINAL/CARRY_FORWARD/EXCLUDE''',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.7 submission_date_field (按 taskDate 还是 completedAt 归属周期)
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema=DATABASE() AND table_name='insp_indicators'
             AND column_name='submission_date_field');
SET @sql := IF(@c=0,
  'ALTER TABLE insp_indicators ADD COLUMN submission_date_field VARCHAR(20) NOT NULL DEFAULT ''taskDate'' COMMENT ''归属周期依据: taskDate (计划日, 默认) / completedAt''',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 1.8 数据迁移: 既有 source_section_id → source_section_ids JSON 数组
UPDATE insp_indicators
SET source_section_ids = JSON_ARRAY(source_section_id)
WHERE source_section_ids IS NULL AND source_section_id IS NOT NULL;

-- 1.9 missing_policy 默认值统一从 'SKIP' 同义到 'IGNORE' (代码层兼容)
-- 不强制 UPDATE, Java 端读取时 SKIP/IGNORE 都接受, 保持向后兼容

-- ============================================================
-- 2. indicator_results 评级结果实体化
-- ============================================================
SET @t := (SELECT COUNT(*) FROM information_schema.tables
           WHERE table_schema=DATABASE() AND table_name='indicator_results');
SET @sql := IF(@t=0,
  'CREATE TABLE indicator_results (
     id BIGINT PRIMARY KEY AUTO_INCREMENT,
     tenant_id BIGINT NOT NULL DEFAULT 0,
     org_unit_id BIGINT NULL,
     indicator_id BIGINT NOT NULL,
     target_id BIGINT NOT NULL,
     target_name VARCHAR(200) NULL,
     period_key VARCHAR(50) NOT NULL COMMENT ''周期键: 2026-W22 / COUNT#7 / MANUAL:2026-05-01_2026-05-31'',
     value DECIMAL(12,4) NULL,
     rank_position INT NULL,
     grade VARCHAR(50) NULL,
     status VARCHAR(20) NOT NULL DEFAULT ''DRAFT'' COMMENT ''DRAFT / PUBLISHED / SUPERSEDED'',
     computed_at DATETIME NOT NULL,
     published_at DATETIME NULL,
     revision_of BIGINT NULL COMMENT ''前一版本 id, 修订链'',
     source_submission_ids JSON NULL COMMENT ''溯源: 这个评级基于哪些 submissions'',
     source_section_ids JSON NULL,
     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
     updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
     deleted TINYINT NOT NULL DEFAULT 0,
     INDEX idx_indicator_target_period (indicator_id, target_id, period_key),
     INDEX idx_indicator_status (indicator_id, status, deleted),
     INDEX idx_revision_of (revision_of)
   ) COMMENT=''评级结果实体: 版本化快照 + 修订链''',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================
-- 3. 撤销 inspection_plans.scoring_profile_id (上轮"评分下沉调度组")
-- ============================================================
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema=DATABASE() AND table_name='insp_inspection_plans'
             AND column_name='scoring_profile_id');
SET @sql := IF(@c>0,
  'ALTER TABLE insp_inspection_plans DROP COLUMN scoring_profile_id',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================
-- 4. 撤销 insp_projects.default_scoring_profile_id
-- ============================================================
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema=DATABASE() AND table_name='insp_projects'
             AND column_name='default_scoring_profile_id');
SET @sql := IF(@c>0,
  'ALTER TABLE insp_projects DROP COLUMN default_scoring_profile_id',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================
-- 注: insp_grade_bands 表不在此 phase 删除. Phase 2 Java 端会做
-- 数据迁移 (GradeBand → Indicator(PER_TASK) + GradeScheme), 然后
-- 在 Phase 6 守护落定后再清理 GradeBand 表 (单独的 V20260523_5
-- 迁移).
-- ============================================================
