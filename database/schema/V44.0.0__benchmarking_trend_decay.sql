-- ============================================================
-- Migration: V44.0.0__benchmarking_trend_decay.sql
-- Description: 1.8 基准对标 + 1.9 趋势因子 + 1.10 分数衰减
-- ============================================================

-- 1.8 基准对标/百分位 (columns on insp_daily_summaries)
ALTER TABLE insp_daily_summaries ADD COLUMN percentile DECIMAL(5,2) DEFAULT NULL COMMENT '百分位(0-100)';
ALTER TABLE insp_daily_summaries ADD COLUMN rank_in_group INT DEFAULT NULL COMMENT '组内排名';
ALTER TABLE insp_daily_summaries ADD COLUMN group_total INT DEFAULT NULL COMMENT '组内总数';
ALTER TABLE insp_daily_summaries ADD COLUMN group_avg DECIMAL(8,2) DEFAULT NULL COMMENT '组平均分';
ALTER TABLE insp_daily_summaries ADD COLUMN group_median DECIMAL(8,2) DEFAULT NULL COMMENT '组中位数';

-- 1.9 趋势因子
ALTER TABLE insp_scoring_profiles ADD COLUMN trend_factor_enabled TINYINT DEFAULT 0;
ALTER TABLE insp_scoring_profiles ADD COLUMN trend_lookback_days INT DEFAULT 7;
ALTER TABLE insp_scoring_profiles ADD COLUMN trend_bonus_per_percent DECIMAL(4,2) DEFAULT 0.5 COMMENT '每1%进步奖励分';
ALTER TABLE insp_scoring_profiles ADD COLUMN trend_penalty_per_percent DECIMAL(4,2) DEFAULT 0.3 COMMENT '每1%退步惩罚分';
ALTER TABLE insp_scoring_profiles ADD COLUMN trend_max_adjustment DECIMAL(6,2) DEFAULT 10.0 COMMENT '趋势最大调整值';

-- 1.10 分数衰减
ALTER TABLE insp_scoring_profiles ADD COLUMN decay_enabled TINYINT DEFAULT 0;
ALTER TABLE insp_scoring_profiles ADD COLUMN decay_mode VARCHAR(20) DEFAULT 'LINEAR' COMMENT 'LINEAR|EXPONENTIAL';
ALTER TABLE insp_scoring_profiles ADD COLUMN decay_rate_per_day DECIMAL(6,4) DEFAULT 0.5 COMMENT '每天衰减值/率';
ALTER TABLE insp_scoring_profiles ADD COLUMN decay_floor DECIMAL(6,2) DEFAULT 60.0 COMMENT '衰减下限';
