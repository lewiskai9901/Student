-- =============================================
-- V75.0.0: 删除综合测评模块所有表
-- 综合测评功能已彻底移除
-- =============================================

DROP TABLE IF EXISTS eval_results;
DROP TABLE IF EXISTS eval_batches;
DROP TABLE IF EXISTS eval_conditions;
DROP TABLE IF EXISTS eval_levels;
DROP TABLE IF EXISTS eval_campaigns;

-- 旧版综合测评表（evaluation_schema.sql 创建的）
DROP TABLE IF EXISTS evaluation_results;
DROP TABLE IF EXISTS evaluation_dimensions;
DROP TABLE IF EXISTS evaluation_periods;
DROP TABLE IF EXISTS evaluation_scores;
DROP TABLE IF EXISTS evaluation_courses;
DROP TABLE IF EXISTS evaluation_semesters;
DROP TABLE IF EXISTS evaluation_honors;
DROP TABLE IF EXISTS evaluation_honor_types;
DROP TABLE IF EXISTS evaluation_behavior_types;
DROP TABLE IF EXISTS evaluation_behaviors;
