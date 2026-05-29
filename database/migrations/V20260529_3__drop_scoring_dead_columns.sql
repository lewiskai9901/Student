-- ============================================================
-- Migration: V20260529_3__drop_scoring_dead_columns.sql
-- Description: 规模公平性重构 Phase 4 — 彻底删除 insp_scoring_profiles 上
--              4 组死的"高级算法"列 (trend/decay/multiRater/calibration).
--
-- 背景: 评分引擎 (ScoreCalculationDomainService / ScoreAggregationService)
--       对这 16 列 0 引用, 且概念错位 (趋势属分析/衰减属整改时效/校准/多评用不上).
--       按"删字段同步删 DB 列, 不留旧列"原则一并 DROP.
--
-- 全新建库一致性: V44 (trend/decay) + V45 (multiRater/calibration) 在历史顺序里
--       ADD 这些列, 本迁移 (序号 V20260529_3) 排在它们之后, 叠加执行末尾把列删掉,
--       最终一致 — 全新库与升级库都不含这 16 列. (V45 建的 insp_rater_calibration_stats
--       表不在本次删除范围, 保留.)
--
-- 幂等: 每列单独 information_schema 条件化 (列存在才 DROP), 可重复执行,
--       缺列也不报错 (与 V97/V104/V20260529_1 风格一致).
-- ============================================================

-- ---------- helper: 逐列条件 DROP ----------
-- 1.9 趋势因子
SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='trend_factor_enabled');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN trend_factor_enabled','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='trend_lookback_days');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN trend_lookback_days','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='trend_bonus_per_percent');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN trend_bonus_per_percent','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='trend_penalty_per_percent');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN trend_penalty_per_percent','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='trend_max_adjustment');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN trend_max_adjustment','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 1.10 分数衰减
SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='decay_enabled');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN decay_enabled','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='decay_mode');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN decay_mode','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='decay_rate_per_day');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN decay_rate_per_day','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='decay_floor');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN decay_floor','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 1.11 多评审员聚合
SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='multi_rater_mode');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN multi_rater_mode','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='rater_weight_by');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN rater_weight_by','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='consensus_threshold');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN consensus_threshold','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 1.12 分布校准
SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='calibration_enabled');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN calibration_enabled','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='calibration_method');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN calibration_method','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='calibration_period_days');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN calibration_period_days','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c = (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='insp_scoring_profiles' AND column_name='calibration_min_samples');
SET @s = IF(@c>0,'ALTER TABLE insp_scoring_profiles DROP COLUMN calibration_min_samples','SELECT 1'); PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
