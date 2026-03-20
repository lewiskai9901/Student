-- ============================================================
-- Migration: V42.0.0__repeat_violation_escalation.sql
-- Description: 1.3 重复违规递增扣分策略 + 1.4 条件触发规则 + 1.5 规则有效期
-- ============================================================

-- 1.3 重复违规递增策略表
CREATE TABLE IF NOT EXISTS insp_escalation_policies (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  tenant_id BIGINT NOT NULL DEFAULT 0,
  profile_id BIGINT NOT NULL COMMENT '关联评分配置',
  policy_name VARCHAR(100) NOT NULL,
  lookup_period_days INT NOT NULL DEFAULT 30 COMMENT '回溯天数',
  escalation_mode VARCHAR(20) NOT NULL DEFAULT 'MULTIPLY' COMMENT 'MULTIPLY|ADD|FIXED_TABLE',
  multiplier DECIMAL(4,2) DEFAULT 2.0 COMMENT '乘数(MULTIPLY模式)',
  adder DECIMAL(6,2) DEFAULT NULL COMMENT '增加值(ADD模式)',
  fixed_table JSON DEFAULT NULL COMMENT '固定表: [{occurrence:1,factor:1.0},...]',
  max_escalation_factor DECIMAL(4,2) DEFAULT 5.0 COMMENT '最大放大倍数',
  match_by VARCHAR(50) NOT NULL DEFAULT 'ITEM_CODE' COMMENT 'ITEM_CODE|DIMENSION|SECTION|CATEGORY',
  is_enabled TINYINT DEFAULT 1,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  INDEX idx_profile (profile_id)
) COMMENT='重复违规递增策略';

-- 1.4 条件触发规则
ALTER TABLE insp_calculation_rules ADD COLUMN activation_condition JSON COMMENT '激活条件(条件逻辑V2格式)';
ALTER TABLE insp_calculation_rules ADD COLUMN applies_to JSON COMMENT '适用范围: {targetTypes:[], orgUnitIds:[], userTypes:[]}';

-- 1.5 规则有效期
ALTER TABLE insp_calculation_rules ADD COLUMN effective_from DATE DEFAULT NULL COMMENT '生效起始日';
ALTER TABLE insp_calculation_rules ADD COLUMN effective_until DATE DEFAULT NULL COMMENT '生效截止日';
