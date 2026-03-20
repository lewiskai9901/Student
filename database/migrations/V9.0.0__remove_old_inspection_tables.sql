-- =====================================================
-- V9.0.0: [已废弃 - 请勿执行]
-- =====================================================
-- !! 此脚本存在BUG：会误删V6活跃表 inspection_templates !!
-- !! 注释中声称保留 v6_ 前缀表，但V6实际使用无前缀表名 !!
-- !! 正确的清理迁移见 V38.0.0__cleanup_orphaned_inspection_tables.sql !!
-- =====================================================

-- 设置安全模式
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 删除旧版量化检查相关表
-- =====================================================

-- 检查会话相关
DROP TABLE IF EXISTS inspection_sessions;
DROP TABLE IF EXISTS class_inspection_records;
DROP TABLE IF EXISTS inspection_deductions;
DROP TABLE IF EXISTS inspection_bonuses;
DROP TABLE IF EXISTS checklist_responses;

-- 检查记录相关
DROP TABLE IF EXISTS inspection_records;
DROP TABLE IF EXISTS class_scores;
DROP TABLE IF EXISTS deduction_details;

-- 检查模板相关 (V4版本)
DROP TABLE IF EXISTS inspection_templates;
DROP TABLE IF EXISTS inspection_categories;
DROP TABLE IF EXISTS deduction_items;
DROP TABLE IF EXISTS bonus_items;

-- 申诉相关 (V4版本)
DROP TABLE IF EXISTS appeals;
DROP TABLE IF EXISTS appeal_approvals;

-- 行为记录相关
DROP TABLE IF EXISTS behavior_records;
DROP TABLE IF EXISTS behavior_alerts;

-- 整改工单相关 (V4版本)
DROP TABLE IF EXISTS corrective_actions;
DROP TABLE IF EXISTS auto_action_rules;

-- 分析快照
DROP TABLE IF EXISTS analytics_snapshots;

-- =====================================================
-- 保留的V6检查系统表（不删除）
-- =====================================================
-- v6_inspection_projects
-- v6_inspection_templates
-- v6_template_items
-- v6_inspection_tasks
-- v6_task_records
-- v6_task_record_details
-- v6_scoring_strategies
-- v6_input_types
-- v6_calculation_rules
-- v6_formula_functions
-- v6_formula_variables
-- v6_corrective_actions

-- 恢复外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- 清理完成提示
-- =====================================================
SELECT 'V4旧版量化检查系统表已清理，V6新版表保留' AS migration_result;
