-- 新增 GRADE_PUBLISHED_PERSONAL 事件: 成绩发布给每个学生的独立事件
-- 用于通知学生本人 + 其家长, 替代原粗粒度 BY_FEATURE(isLearner) 全系统扇出

INSERT INTO entity_event_types
  (tenant_id, category_code, category_name, type_code, type_name, has_score, has_severity,
   icon, color, applicable_subjects, is_system, is_enabled, sort_order, category_polarity,
   created_at, updated_at, deleted)
VALUES
  (1, 'TEACHING', '教务流程', 'GRADE_PUBLISHED_PERSONAL', '成绩发布(个人)',
   0, 0, 'user-check', '#2563eb', '["USER"]', 0, 1, 13, 'NEUTRAL',
   NOW(), NOW(), 0);

INSERT INTO trigger_points
  (module_code, module_name, point_code, point_name, description, context_schema,
   is_enabled, sort_order, tenant_id, created_at, updated_at, deleted)
VALUES
  ('teaching', '教学管理', 'GRADE_PUBLISHED_PERSONAL', '成绩发布-个人级',
   '对班级每个学生独立触发, 用于通知学生本人和家长',
   JSON_OBJECT('required', JSON_ARRAY('studentId', 'batchId', 'orgUnitId')),
   1, 0, 1, NOW(), NOW(), 0);

INSERT INTO event_triggers
  (name, trigger_point_code, condition_json, event_type_mode, event_type_code, event_type_source,
   description, subjects_json, is_enabled, sort_order, tenant_id, created_at, updated_at, deleted)
VALUES
  ('成绩个人通知', 'GRADE_PUBLISHED_PERSONAL', NULL, 'FIXED', 'GRADE_PUBLISHED_PERSONAL', NULL,
   '成绩发布 → 学生 + 家长',
   JSON_ARRAY(JSON_OBJECT('type','USER','idSource','studentId','nameSource','studentName')),
   1, 13, 1, NOW(), NOW(), 0);

-- 订阅规则: 学生自己 + 家长
INSERT INTO msg_subscription_rules
  (rule_name, event_type, target_mode, target_config, is_enabled, tenant_id, created_at)
VALUES
  ('个人成绩-通知学生本人', 'GRADE_PUBLISHED_PERSONAL', 'BY_SUBJECT', '{}', 1, 1, NOW()),
  ('个人成绩-通知家长', 'GRADE_PUBLISHED_PERSONAL', 'BY_RELATION',
   -- direction=inward 因为 guardian_of 关系存储方向是 "subject=家长 → resource=学生",
   -- 事件主体是学生时,我们要"找把学生当作 resource 的 user subject",即 inward
   JSON_OBJECT('relation','guardian_of','resource_type','user','direction','inward'),
   1, 1, NOW());

-- 删除旧的粗粒度 BY_FEATURE 规则 (被 GRADE_PUBLISHED_PERSONAL + BY_SUBJECT 精准替代)
UPDATE msg_subscription_rules SET deleted = 1
WHERE event_type = 'GRADE_PUBLISHED' AND target_mode = 'BY_FEATURE';
