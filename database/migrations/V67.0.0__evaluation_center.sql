-- 评级中心 (Evaluation Center)
-- V67.0.0: 评选活动、级别、条件、执行批次、批次结果

-- 评选活动
CREATE TABLE eval_campaigns (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  campaign_name VARCHAR(100) NOT NULL,
  campaign_description TEXT NULL,
  target_type VARCHAR(20) NOT NULL COMMENT 'ORG/PLACE/USER',
  scope_org_ids TEXT NULL COMMENT 'JSON: 范围组织ID列表',
  evaluation_period VARCHAR(20) DEFAULT 'MONTHLY',
  status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT/ACTIVE/PAUSED/ARCHIVED',
  is_auto_execute TINYINT DEFAULT 0 COMMENT '是否按周期自动执行',
  last_executed_at DATETIME NULL,
  next_execute_at DATETIME NULL,
  sort_order INT DEFAULT 0,
  created_by BIGINT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_by BIGINT NULL,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_tenant (tenant_id),
  INDEX idx_status (status)
);

-- 评选级别
CREATE TABLE eval_levels (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  campaign_id BIGINT NOT NULL,
  level_num INT NOT NULL COMMENT '1=最高',
  level_name VARCHAR(50) NOT NULL,
  condition_logic VARCHAR(10) DEFAULT 'AND',
  sort_order INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_campaign (campaign_id),
  UNIQUE KEY uk_campaign_level (campaign_id, level_num)
);

-- 评选条件
CREATE TABLE eval_conditions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  level_id BIGINT NOT NULL,
  source_type VARCHAR(20) NOT NULL COMMENT 'INSPECTION/EVENT/HISTORY',
  source_config TEXT NOT NULL COMMENT 'JSON: 数据源参数',
  metric VARCHAR(30) NOT NULL COMMENT '指标类型',
  operator VARCHAR(10) NOT NULL COMMENT '>=/<=/=/!=/IN',
  threshold TEXT NOT NULL COMMENT '阈值（数字或JSON数组）',
  scope VARCHAR(20) DEFAULT 'SELF' COMMENT 'SELF/MEMBERS/SPECIFIC_ROLE',
  scope_role VARCHAR(50) NULL COMMENT 'SPECIFIC_ROLE时的角色编码',
  time_range VARCHAR(20) DEFAULT 'CYCLE' COMMENT 'CYCLE/CUSTOM',
  time_range_days INT NULL COMMENT 'CUSTOM时的天数',
  description VARCHAR(200) NULL COMMENT '自然语言描述（自动生成）',
  sort_order INT DEFAULT 0,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_level (level_id)
);

-- 执行批次
CREATE TABLE eval_batches (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  campaign_id BIGINT NOT NULL,
  cycle_start DATE NOT NULL,
  cycle_end DATE NOT NULL,
  total_targets INT DEFAULT 0,
  executed_at DATETIME NOT NULL,
  executed_by BIGINT NULL,
  status VARCHAR(20) DEFAULT 'COMPLETED',
  summary TEXT NULL COMMENT 'JSON: {level1Count:3, level2Count:12, ...}',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_campaign (campaign_id, cycle_start)
);

-- 批次结果
CREATE TABLE eval_results (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_id BIGINT NOT NULL,
  campaign_id BIGINT NOT NULL,
  target_type VARCHAR(20) NOT NULL,
  target_id BIGINT NOT NULL,
  target_name VARCHAR(100) NULL,
  level_num INT NULL COMMENT 'NULL=未达任何级别',
  level_name VARCHAR(50) NULL,
  rank_no INT NULL,
  score DECIMAL(10,2) NULL COMMENT '综合分（用于排名）',
  condition_details TEXT NULL COMMENT 'JSON: [{conditionId, passed, actual, threshold, description}]',
  upgrade_hint TEXT NULL COMMENT '升级提示文本',
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_batch (batch_id),
  INDEX idx_campaign_target (campaign_id, target_type, target_id),
  INDEX idx_rank (batch_id, rank_no)
);
