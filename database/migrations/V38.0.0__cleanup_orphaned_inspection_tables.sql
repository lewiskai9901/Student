-- =====================================================
-- V38.0.0: 清理量化检查系统孤儿表
-- =====================================================
-- 背景：
--   V4检查系统的Java代码已全部删除，但数据库表仍存在
--   V9.0.0迁移脚本存在BUG（会误删V6同名表），从未执行
--   V8.5.0创建的entity_types/entity_groups后续弃用
--   本迁移仅删除确认无Java代码引用的孤儿表
--
-- 保留的V6活跃表（有@TableName或Raw SQL引用）：
--   inspection_projects, inspection_tasks, inspection_templates,
--   inspection_targets, inspection_details, inspection_evidences,
--   template_categories, template_score_items,
--   project_inspector_configs, task_inspector_assignments,
--   v6_corrective_actions,
--   inspection_daily_summaries, inspection_weekly_summaries,
--   inspection_monthly_summaries
-- =====================================================

SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 1. V4 会话/记录相关（零Java代码引用）
-- =====================================================
DROP TABLE IF EXISTS inspection_sessions;
DROP TABLE IF EXISTS class_inspection_records;
DROP TABLE IF EXISTS checklist_responses;
DROP TABLE IF EXISTS inspection_deduction_items;

-- =====================================================
-- 2. V4 扣分/加分表（与V6 inspection_details功能重叠，零引用）
-- =====================================================
DROP TABLE IF EXISTS inspection_deductions;
DROP TABLE IF EXISTS inspection_bonuses;

-- =====================================================
-- 3. V4 旧版模板/记录（名称与V6不冲突的）
-- =====================================================
DROP TABLE IF EXISTS inspection_records;
DROP TABLE IF EXISTS class_scores;
DROP TABLE IF EXISTS deduction_details;
DROP TABLE IF EXISTS inspection_categories;
DROP TABLE IF EXISTS deduction_items;
DROP TABLE IF EXISTS bonus_items;

-- =====================================================
-- 4. V4 申诉相关（V6无申诉功能）
-- =====================================================
DROP TABLE IF EXISTS appeals;
DROP TABLE IF EXISTS appeal_approvals;

-- =====================================================
-- 5. V4 行为记录（Java代码已删除）
-- =====================================================
DROP TABLE IF EXISTS behavior_records;
DROP TABLE IF EXISTS behavior_alerts;
DROP TABLE IF EXISTS student_behavior_records;
DROP TABLE IF EXISTS student_behaviors;

-- =====================================================
-- 6. V4 整改/调度/分析（已迁移到v6_corrective_actions）
-- =====================================================
DROP TABLE IF EXISTS corrective_actions;
DROP TABLE IF EXISTS auto_action_rules;
DROP TABLE IF EXISTS analytics_snapshots;
DROP TABLE IF EXISTS inspection_schedules;

-- =====================================================
-- 7. 弃用的实体配置表（V8.5.0创建，后来弃用）
-- =====================================================
DROP TABLE IF EXISTS entity_types;
DROP TABLE IF EXISTS entity_groups;

-- =====================================================
-- 8. V4 备份表（FK修复时发现的残留）
-- =====================================================
DROP TABLE IF EXISTS inspection_targets_v4_backup;
DROP TABLE IF EXISTS inspection_deductions_v4_backup;
DROP TABLE IF EXISTS inspection_bonuses_v4_backup;

-- =====================================================
-- 9. 其他无代码引用的检查相关表
-- =====================================================
DROP TABLE IF EXISTS inspection_rounds;
DROP TABLE IF EXISTS inspection_session_sequence;
DROP TABLE IF EXISTS template_items;
DROP TABLE IF EXISTS template_versions;

SET FOREIGN_KEY_CHECKS = 1;

SELECT 'V38: 已清理所有量化检查孤儿表，V6活跃表全部保留' AS migration_result;
