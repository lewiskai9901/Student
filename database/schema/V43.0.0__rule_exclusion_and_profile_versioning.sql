-- ============================================================
-- Migration: V43.0.0__rule_exclusion_and_profile_versioning.sql
-- Description: 1.6 规则互斥组 + 1.7 评分配置版本化
-- ============================================================

-- 1.6 规则互斥组
ALTER TABLE insp_calculation_rules ADD COLUMN exclusion_group VARCHAR(50) DEFAULT NULL COMMENT '互斥组名';

-- 1.7 评分配置版本化
ALTER TABLE insp_scoring_profiles ADD COLUMN current_version INT DEFAULT 0;

CREATE TABLE IF NOT EXISTS insp_scoring_profile_versions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  profile_id BIGINT NOT NULL,
  version INT NOT NULL,
  snapshot JSON NOT NULL COMMENT '完整配置快照(dimensions+rules+bands+escalation)',
  published_at DATETIME NOT NULL,
  published_by BIGINT,
  change_summary VARCHAR(500),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  UNIQUE KEY uk_profile_version (profile_id, version)
) COMMENT='评分配置版本快照';

ALTER TABLE insp_submissions ADD COLUMN scoring_profile_version INT DEFAULT NULL COMMENT '使用的评分配置版本';
