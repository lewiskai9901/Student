-- =====================================================
-- 全量数据清空脚本
-- 保留: admin账号、角色权限、系统配置、场所类型、专业大类
-- =====================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ==================== 学生相关 ====================
TRUNCATE TABLE students;
TRUNCATE TABLE student_grades;
TRUNCATE TABLE student_dormitory;
TRUNCATE TABLE student_behavior_alerts;
TRUNCATE TABLE student_evaluation_results;
TRUNCATE TABLE student_honor_application;
TRUNCATE TABLE student_scores;
TRUNCATE TABLE student_status_changes;
TRUNCATE TABLE attendance_records;
TRUNCATE TABLE leave_requests;
TRUNCATE TABLE academic_warnings;
TRUNCATE TABLE academic_warning_rules;
TRUNCATE TABLE enrollment_applications;
TRUNCATE TABLE enrollment_plans;

-- ==================== 班级/年级 ====================
TRUNCATE TABLE classes;
TRUNCATE TABLE class_course_assignments;
TRUNCATE TABLE class_dormitory_bindings;
TRUNCATE TABLE class_quantification_summary;
TRUNCATE TABLE class_size_snapshots;
TRUNCATE TABLE class_size_standards;
TRUNCATE TABLE class_weight_configs;
TRUNCATE TABLE grade_major_directions;
TRUNCATE TABLE grade_directors;
TRUNCATE TABLE grades;

-- ==================== 学术(专业/课程/方案) ====================
TRUNCATE TABLE curriculum_plan_courses;
TRUNCATE TABLE curriculum_plans;
TRUNCATE TABLE courses;
TRUNCATE TABLE major_directions;
TRUNCATE TABLE majors;
-- 保留 major_categories (基础字典数据)

-- ==================== 教学 ====================
TRUNCATE TABLE teaching_tasks;
TRUNCATE TABLE teaching_task_teachers;
TRUNCATE TABLE teaching_classes;
TRUNCATE TABLE teaching_class_members;
TRUNCATE TABLE semester_course_offerings;
TRUNCATE TABLE schedule_entries;
TRUNCATE TABLE schedule_adjustments;
TRUNCATE TABLE schedule_conflict_records;
TRUNCATE TABLE scheduling_constraints;
TRUNCATE TABLE schedule_policies;
TRUNCATE TABLE schedule_executions;

-- ==================== 考试/成绩 ====================
TRUNCATE TABLE exam_batches;
TRUNCATE TABLE exam_arrangements;
TRUNCATE TABLE exam_rooms;
TRUNCATE TABLE exam_invigilators;
TRUNCATE TABLE grade_batches;

-- ==================== 校历 ====================
TRUNCATE TABLE academic_years;
TRUNCATE TABLE semesters;
TRUNCATE TABLE academic_weeks;
TRUNCATE TABLE academic_event;

-- ==================== 组织架构 ====================
TRUNCATE TABLE org_units;
TRUNCATE TABLE org_change_logs;
TRUNCATE TABLE positions;
TRUNCATE TABLE user_positions;
TRUNCATE TABLE departments;
-- 保留 org_unit_types (配置数据)

-- ==================== 用户(保留admin) ====================
DELETE FROM users WHERE id != 1;
DELETE FROM user_roles WHERE user_id != 1;
TRUNCATE TABLE user_departments;
TRUNCATE TABLE user_org_relations;
TRUNCATE TABLE user_org_relation_history;
TRUNCATE TABLE user_data_scopes;
TRUNCATE TABLE user_scope_assignments;
TRUNCATE TABLE user_place_relations;
TRUNCATE TABLE user_place_relation_history;
-- 保留 roles, permissions, role_permissions (权限配置)
-- 保留 user_types (用户类型配置)

-- ==================== 教师 ====================
TRUNCATE TABLE teacher_profiles;
TRUNCATE TABLE teacher_course_qualifications;
TRUNCATE TABLE teacher_assignments;

-- ==================== 场所/宿舍 ====================
TRUNCATE TABLE places;
TRUNCATE TABLE place_occupants;
TRUNCATE TABLE place_bookings;
TRUNCATE TABLE place_org_relations;
TRUNCATE TABLE place_org_relation_history;
TRUNCATE TABLE place_class_assignment;
TRUNCATE TABLE place_org_assignment;
TRUNCATE TABLE place_assignments;
TRUNCATE TABLE place_audit_logs;
TRUNCATE TABLE place_batch_jobs;
TRUNCATE TABLE place_batch_job_items;
TRUNCATE TABLE place_capacity_stats_mv;
TRUNCATE TABLE place_categories;
TRUNCATE TABLE booking_seat_assignments;
-- 保留 place_types, place_type_config (场所类型配置)

-- 旧宿舍表(如还在)
TRUNCATE TABLE dormitories;
TRUNCATE TABLE building_dormitories;
TRUNCATE TABLE building_departments;
TRUNCATE TABLE building_teachings;
TRUNCATE TABLE buildings;
TRUNCATE TABLE dormitory_buildings;
TRUNCATE TABLE dormitory_building_managers;
TRUNCATE TABLE classrooms;

-- ==================== 检查平台 ====================
TRUNCATE TABLE insp_projects;
TRUNCATE TABLE insp_tasks;
TRUNCATE TABLE insp_submissions;
TRUNCATE TABLE insp_submission_details;
TRUNCATE TABLE insp_evidences;
TRUNCATE TABLE insp_inspection_plans;
TRUNCATE TABLE insp_project_inspectors;
TRUNCATE TABLE insp_project_scores;
TRUNCATE TABLE insp_task_section_assignments;
TRUNCATE TABLE insp_templates;
TRUNCATE TABLE insp_template_versions;
TRUNCATE TABLE insp_template_sections;
TRUNCATE TABLE insp_template_items;
TRUNCATE TABLE insp_template_module_refs;
TRUNCATE TABLE insp_template_catalogs;
TRUNCATE TABLE insp_library_items;
TRUNCATE TABLE insp_response_sets;
TRUNCATE TABLE insp_response_set_options;
TRUNCATE TABLE insp_scoring_profiles;
TRUNCATE TABLE insp_scoring_profile_versions;
TRUNCATE TABLE insp_scoring_presets;
TRUNCATE TABLE insp_scoring_policies;
TRUNCATE TABLE insp_policy_calc_rules;
TRUNCATE TABLE insp_policy_grade_bands;
TRUNCATE TABLE insp_grade_schemes;
TRUNCATE TABLE insp_grade_definitions;
TRUNCATE TABLE insp_grade_bands;
TRUNCATE TABLE insp_indicators;
TRUNCATE TABLE insp_indicator_scores;
TRUNCATE TABLE insp_corrective_cases;
TRUNCATE TABLE insp_corrective_subtasks;
TRUNCATE TABLE insp_corrective_summaries;
TRUNCATE TABLE insp_daily_summaries;
TRUNCATE TABLE insp_period_summaries;
TRUNCATE TABLE insp_inspector_summaries;
TRUNCATE TABLE insp_item_frequency_summaries;
TRUNCATE TABLE insp_alert_rules;
TRUNCATE TABLE insp_alerts;
TRUNCATE TABLE insp_audit_trail;
TRUNCATE TABLE insp_notification_rules;
TRUNCATE TABLE insp_webhook_subscriptions;
TRUNCATE TABLE insp_nfc_tags;
TRUNCATE TABLE insp_iot_sensors;
TRUNCATE TABLE insp_sensor_readings;
TRUNCATE TABLE insp_item_sensor_bindings;
TRUNCATE TABLE insp_holiday_calendars;
TRUNCATE TABLE insp_compliance_standards;
TRUNCATE TABLE insp_compliance_clauses;
TRUNCATE TABLE insp_item_compliance_mappings;
TRUNCATE TABLE insp_knowledge_articles;
TRUNCATE TABLE insp_report_templates;
TRUNCATE TABLE insp_violation_records;
TRUNCATE TABLE insp_rater_calibration_stats;
TRUNCATE TABLE insp_rating_dimensions;
TRUNCATE TABLE insp_rating_results;
TRUNCATE TABLE insp_rating_links;
TRUNCATE TABLE insp_score_dimensions;
TRUNCATE TABLE insp_escalation_policies;
TRUNCATE TABLE insp_evaluation_levels;
TRUNCATE TABLE insp_evaluation_results;
TRUNCATE TABLE insp_evaluation_rules;
TRUNCATE TABLE insp_calculation_rules;

-- ==================== 资产 ====================
TRUNCATE TABLE asset;
TRUNCATE TABLE asset_alert;
TRUNCATE TABLE asset_approval;
TRUNCATE TABLE asset_borrow;
TRUNCATE TABLE asset_category;
TRUNCATE TABLE asset_depreciation;
TRUNCATE TABLE asset_history;
TRUNCATE TABLE asset_inventory;
TRUNCATE TABLE asset_inventory_detail;
TRUNCATE TABLE asset_maintenance;

-- ==================== 消息/通知 ====================
TRUNCATE TABLE msg_notifications;
TRUNCATE TABLE msg_subscription_rules;
TRUNCATE TABLE msg_templates;
TRUNCATE TABLE announcements;
TRUNCATE TABLE announcement_reads;
TRUNCATE TABLE notification_records;
TRUNCATE TABLE system_messages;

-- ==================== 审计/日志 ====================
TRUNCATE TABLE audit_trail;
TRUNCATE TABLE audit_logs;
TRUNCATE TABLE operation_logs;
TRUNCATE TABLE domain_events;
TRUNCATE TABLE entity_events;
TRUNCATE TABLE entity_event_relations;
TRUNCATE TABLE entity_event_types;
TRUNCATE TABLE event_publications;
TRUNCATE TABLE permission_audit_log;

-- ==================== 其他 ====================
TRUNCATE TABLE access_relations;
TRUNCATE TABLE wechat_push_record;
TRUNCATE TABLE export_tasks;
TRUNCATE TABLE revision_records;
TRUNCATE TABLE tasks;
TRUNCATE TABLE task_assignees;
TRUNCATE TABLE task_logs;
TRUNCATE TABLE task_submissions;

SET FOREIGN_KEY_CHECKS = 1;

-- 验证
SELECT 'admin用户' as item, COUNT(*) as cnt FROM users WHERE deleted=0
UNION SELECT '角色数', COUNT(*) FROM roles WHERE deleted=0
UNION SELECT '权限数', COUNT(*) FROM permissions WHERE deleted=0
UNION SELECT '场所类型', COUNT(*) FROM place_types WHERE deleted=0
UNION SELECT '专业大类', COUNT(*) FROM major_categories WHERE deleted=0;
