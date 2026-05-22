-- ============================================================
-- V20260523_1: 评分配置下沉调度组
--
-- 把评分配置从"项目全局 + 散落 3 处"收敛为两层模型:
--   ScoringProfile(可复用规则) ← 引用 ← InspectionPlan 调度组(执行单元)
--
-- 本迁移:
--   1. insp_inspection_plans 加 scoring_profile_id / raters_per_target
--   2. insp_projects.scoring_profile_id 改名 default_scoring_profile_id
--      (语义变为: 非计划任务兜底 + 新调度组预填)
--   3. 存量迁移: 各调度组从所属项目继承 scoring_profile + 评分人数
--      (evaluation_mode=MULTI → raters_per_target=2)
--   4. insp_projects 删除 11 个评分列 (评分规则只属于 ScoringProfile)
--
-- 全部 information_schema 条件化, 可重复执行 (与 V97/V104/V108 风格一致).
-- ============================================================

-- ------------------------------------------------------------
-- 1) insp_inspection_plans + scoring_profile_id
-- ------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema=DATABASE() AND table_name='insp_inspection_plans'
             AND column_name='scoring_profile_id');
SET @sql := IF(@c=0,
  'ALTER TABLE insp_inspection_plans ADD COLUMN scoring_profile_id BIGINT NULL COMMENT ''调度组引用的评分方案'' AFTER root_section_id',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 2) insp_inspection_plans + raters_per_target
-- ------------------------------------------------------------
SET @c := (SELECT COUNT(*) FROM information_schema.columns
           WHERE table_schema=DATABASE() AND table_name='insp_inspection_plans'
             AND column_name='raters_per_target');
SET @sql := IF(@c=0,
  'ALTER TABLE insp_inspection_plans ADD COLUMN raters_per_target INT NOT NULL DEFAULT 1 COMMENT ''每目标评分人数 1=单人 >1=多人评分''',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 3) insp_projects.scoring_profile_id → default_scoring_profile_id
-- ------------------------------------------------------------
SET @old := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema=DATABASE() AND table_name='insp_projects'
               AND column_name='scoring_profile_id');
SET @new := (SELECT COUNT(*) FROM information_schema.columns
             WHERE table_schema=DATABASE() AND table_name='insp_projects'
               AND column_name='default_scoring_profile_id');
SET @sql := IF(@old>0 AND @new=0,
  'ALTER TABLE insp_projects CHANGE COLUMN scoring_profile_id default_scoring_profile_id BIGINT NULL COMMENT ''默认评分方案: 非计划任务兜底 + 新调度组预填''',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 4) 存量迁移: 调度组从所属项目继承评分配置
--    仅当 evaluation_mode 列还在 (即首次执行) 时运行.
-- ------------------------------------------------------------
SET @em := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema=DATABASE() AND table_name='insp_projects'
              AND column_name='evaluation_mode');
SET @sql := IF(@em>0,
  'UPDATE insp_inspection_plans p JOIN insp_projects pr ON p.project_id=pr.id
     SET p.scoring_profile_id = pr.default_scoring_profile_id,
         p.raters_per_target  = IF(pr.evaluation_mode=''MULTI'', 2, 1)
   WHERE p.scoring_profile_id IS NULL',
  'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ------------------------------------------------------------
-- 5) insp_projects 删除 11 个评分列 — 评分规则只属于 ScoringProfile
-- ------------------------------------------------------------
-- 逐列条件化删除 (某列已删则跳过).
SET @cols := 'evaluation_mode,multi_rater_mode,rater_weight_by,consensus_threshold,trend_enabled,trend_lookback_days,decay_enabled,decay_mode,calibration_enabled,calibration_method,split_strategy';

SET @col:='evaluation_mode';        SET @e:=(SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name=@col); SET @sql:=IF(@e>0,CONCAT('ALTER TABLE insp_projects DROP COLUMN ',@col),'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col:='multi_rater_mode';       SET @e:=(SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name=@col); SET @sql:=IF(@e>0,CONCAT('ALTER TABLE insp_projects DROP COLUMN ',@col),'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col:='rater_weight_by';        SET @e:=(SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name=@col); SET @sql:=IF(@e>0,CONCAT('ALTER TABLE insp_projects DROP COLUMN ',@col),'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col:='consensus_threshold';    SET @e:=(SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name=@col); SET @sql:=IF(@e>0,CONCAT('ALTER TABLE insp_projects DROP COLUMN ',@col),'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col:='trend_enabled';          SET @e:=(SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name=@col); SET @sql:=IF(@e>0,CONCAT('ALTER TABLE insp_projects DROP COLUMN ',@col),'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col:='trend_lookback_days';    SET @e:=(SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name=@col); SET @sql:=IF(@e>0,CONCAT('ALTER TABLE insp_projects DROP COLUMN ',@col),'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col:='decay_enabled';          SET @e:=(SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name=@col); SET @sql:=IF(@e>0,CONCAT('ALTER TABLE insp_projects DROP COLUMN ',@col),'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col:='decay_mode';             SET @e:=(SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name=@col); SET @sql:=IF(@e>0,CONCAT('ALTER TABLE insp_projects DROP COLUMN ',@col),'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col:='calibration_enabled';    SET @e:=(SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name=@col); SET @sql:=IF(@e>0,CONCAT('ALTER TABLE insp_projects DROP COLUMN ',@col),'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col:='calibration_method';     SET @e:=(SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name=@col); SET @sql:=IF(@e>0,CONCAT('ALTER TABLE insp_projects DROP COLUMN ',@col),'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
SET @col:='split_strategy';         SET @e:=(SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_projects' AND column_name=@col); SET @sql:=IF(@e>0,CONCAT('ALTER TABLE insp_projects DROP COLUMN ',@col),'SELECT 1'); PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
