-- ============================================================
-- Migration: V45.0.0__multi_rater_calibration.sql
-- Description: 1.11 多评审员聚合 + 1.12 分布校准
-- ============================================================

-- 1.11 多评审员聚合
ALTER TABLE insp_scoring_profiles ADD COLUMN multi_rater_mode VARCHAR(30) DEFAULT 'LATEST'
  COMMENT 'LATEST|AVERAGE|WEIGHTED_AVERAGE|MEDIAN|MAX|MIN|CONSENSUS';
ALTER TABLE insp_scoring_profiles ADD COLUMN rater_weight_by VARCHAR(30) DEFAULT NULL
  COMMENT 'EQUAL|BY_ROLE|BY_EXPERIENCE|CUSTOM';
ALTER TABLE insp_scoring_profiles ADD COLUMN consensus_threshold DECIMAL(4,2) DEFAULT 0.8
  COMMENT '共识阈值(0-1)，偏差超过时标记需复核';

-- 1.12 分布校准
ALTER TABLE insp_scoring_profiles ADD COLUMN calibration_enabled TINYINT DEFAULT 0;
ALTER TABLE insp_scoring_profiles ADD COLUMN calibration_method VARCHAR(30) DEFAULT 'Z_SCORE'
  COMMENT 'Z_SCORE|MIN_MAX|PERCENTILE_RANK|IRT';
ALTER TABLE insp_scoring_profiles ADD COLUMN calibration_period_days INT DEFAULT 30
  COMMENT '校准样本回溯天数';
ALTER TABLE insp_scoring_profiles ADD COLUMN calibration_min_samples INT DEFAULT 10
  COMMENT '最少样本数才启用校准';

CREATE TABLE IF NOT EXISTS insp_rater_calibration_stats (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  profile_id BIGINT NOT NULL,
  inspector_id BIGINT NOT NULL,
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  sample_count INT NOT NULL,
  mean_score DECIMAL(8,4) NOT NULL,
  std_dev DECIMAL(8,4) NOT NULL,
  skewness DECIMAL(8,4) DEFAULT NULL,
  calibration_offset DECIMAL(8,4) DEFAULT NULL COMMENT '校准偏移量',
  calibration_scale DECIMAL(8,4) DEFAULT NULL COMMENT '校准缩放因子',
  calculated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_inspector_period (profile_id, inspector_id, period_start, period_end)
) COMMENT='评审员校准统计';
