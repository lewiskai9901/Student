-- ============================================================
-- Migration: V41.0.0__item_weight_and_cross_dim.sql
-- Description: 1.1 检查项权重 + 1.2 跨维度规则
-- ============================================================

-- 1.1 检查项权重
ALTER TABLE insp_template_items ADD COLUMN item_weight DECIMAL(5,2) DEFAULT 1.00 COMMENT '项目权重(维度内)';
ALTER TABLE insp_submission_details ADD COLUMN item_weight DECIMAL(5,2) DEFAULT 1.00 COMMENT '快照:提交时的项目权重';

-- 1.2 跨维度规则
ALTER TABLE insp_calculation_rules ADD COLUMN scope_type VARCHAR(20) DEFAULT 'GLOBAL' COMMENT 'GLOBAL|DIMENSION|CROSS_DIMENSION';
ALTER TABLE insp_calculation_rules ADD COLUMN target_dimension_ids JSON COMMENT '跨维度规则关联的维度ID列表';
