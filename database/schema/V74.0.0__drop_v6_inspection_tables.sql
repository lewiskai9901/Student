-- =============================================
-- V74.0.0: 删除旧版量化检查系统所有表（V3/V4/V5/V6）
-- V7检查平台已完全取代旧版，旧表不再需要
-- =============================================

-- 先删除视图
DROP VIEW IF EXISTS v_org_inspection_summary;
DROP VIEW IF EXISTS v_class_inspection_summary;

-- V6 表 (V8.4.0 ~ V9.0.0 创建)
DROP TABLE IF EXISTS template_score_items;
DROP TABLE IF EXISTS template_categories;
DROP TABLE IF EXISTS v6_corrective_actions;
DROP TABLE IF EXISTS inspection_evidences;
DROP TABLE IF EXISTS inspection_details;
DROP TABLE IF EXISTS scoring_strategies;
DROP TABLE IF EXISTS inspection_monthly_summaries;
DROP TABLE IF EXISTS inspection_weekly_summaries;
DROP TABLE IF EXISTS inspection_daily_summaries;
DROP TABLE IF EXISTS task_inspector_assignments;
DROP TABLE IF EXISTS project_inspector_configs;
DROP TABLE IF EXISTS inspection_targets;
DROP TABLE IF EXISTS inspection_tasks;
DROP TABLE IF EXISTS inspection_projects;
DROP TABLE IF EXISTS inspection_deductions;
DROP TABLE IF EXISTS inspection_bonuses;
DROP TABLE IF EXISTS entity_types;
DROP TABLE IF EXISTS entity_groups;

-- V5 表 (V5.0.2 创建)
DROP TABLE IF EXISTS appeal_approvals;
DROP TABLE IF EXISTS appeals_v2;
DROP TABLE IF EXISTS weight_schemes;
DROP TABLE IF EXISTS deduction_items_v2;
DROP TABLE IF EXISTS inspection_categories;
DROP TABLE IF EXISTS inspection_rounds;
DROP TABLE IF EXISTS inspection_records;
DROP TABLE IF EXISTS template_versions;
DROP TABLE IF EXISTS inspection_templates;

-- V4 残留表（V38 可能已清理，用 IF EXISTS 安全删除）
DROP TABLE IF EXISTS inspection_sessions;
DROP TABLE IF EXISTS class_inspection_records;
DROP TABLE IF EXISTS checklist_responses;
DROP TABLE IF EXISTS bonus_items;
DROP TABLE IF EXISTS corrective_actions;
DROP TABLE IF EXISTS auto_action_rules;
DROP TABLE IF EXISTS student_behavior_records;
DROP TABLE IF EXISTS student_behavior_alerts;
DROP TABLE IF EXISTS schedule_policies;
DROP TABLE IF EXISTS schedule_executions;
DROP TABLE IF EXISTS analytics_snapshots;
DROP TABLE IF EXISTS inspection_deduction_items;
DROP TABLE IF EXISTS inspection_session_sequence;
DROP TABLE IF EXISTS inspection_targets_v4_backup;
DROP TABLE IF EXISTS inspection_deductions_v4_backup;
DROP TABLE IF EXISTS inspection_bonuses_v4_backup;

-- V3 残留表
DROP TABLE IF EXISTS check_record_rating_results;
DROP TABLE IF EXISTS check_record_items;
DROP TABLE IF EXISTS check_record_class_stats;
DROP TABLE IF EXISTS check_record_weight_configs;
DROP TABLE IF EXISTS check_records;
DROP TABLE IF EXISTS check_item_appeals;
DROP TABLE IF EXISTS check_audit_logs;
DROP TABLE IF EXISTS template_items;
DROP TABLE IF EXISTS template_categories;
DROP TABLE IF EXISTS check_items;
DROP TABLE IF EXISTS check_categories;
DROP TABLE IF EXISTS check_templates;
DROP TABLE IF EXISTS rating_levels;
DROP TABLE IF EXISTS rating_rules;
DROP TABLE IF EXISTS rating_templates;
DROP TABLE IF EXISTS check_plans;
DROP TABLE IF EXISTS class_scores;
DROP TABLE IF EXISTS deduction_items;
DROP TABLE IF EXISTS deduction_details;
DROP TABLE IF EXISTS appeal_approval_records;
DROP TABLE IF EXISTS check_plan_rating_rules;
DROP TABLE IF EXISTS check_plan_rating_levels;
DROP TABLE IF EXISTS check_plan_rating_results;
DROP TABLE IF EXISTS check_plan_rating_frequency;
DROP TABLE IF EXISTS appeal_audit_logs;
DROP TABLE IF EXISTS rating_change_logs;
