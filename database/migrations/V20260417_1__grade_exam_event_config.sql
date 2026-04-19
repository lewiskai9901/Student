-- ============================================================
-- 成绩/考试流程事件配置
-- 为 GRADE_SUBMITTED / GRADE_APPROVED / GRADE_PUBLISHED / EXAM_PUBLISHED
-- 注册触发点、事件类型、触发器、订阅规则
-- ============================================================

-- 1. 注册触发点 (trigger_points)
INSERT INTO trigger_points (module_code, module_name, point_code, point_name, description, is_enabled, sort_order, tenant_id)
VALUES
  ('teaching', '教务管理', 'GRADE_SUBMITTED', '成绩提交',   '教师提交成绩批次待审核', 1, 20, 1),
  ('teaching', '教务管理', 'GRADE_APPROVED', '成绩审核通过', '管理员审核通过成绩批次',   1, 21, 1),
  ('teaching', '教务管理', 'EXAM_PUBLISHED', '考试发布',     '考试计划发布',             1, 22, 1)
ON DUPLICATE KEY UPDATE point_name = VALUES(point_name);

-- 2. 注册事件类型 (entity_event_types) — 归入 TEACHING（教务流程）分类
INSERT INTO entity_event_types (tenant_id, category_code, category_name, type_code, type_name, icon, color, applicable_subjects, is_system, is_enabled, sort_order, category_polarity)
VALUES
  (1, 'TEACHING', '教务流程', 'GRADE_SUBMITTED', '成绩待审核',   'edit',      '#f59e0b', '["USER"]',     1, 1, 10, 'NEUTRAL'),
  (1, 'TEACHING', '教务流程', 'GRADE_APPROVED', '成绩审核通过', 'check',     '#10b981', '["USER"]',     1, 1, 11, 'NEUTRAL'),
  (1, 'TEACHING', '教务流程', 'GRADE_PUBLISHED','成绩发布',     'megaphone', '#2563eb', '["ORG_UNIT"]', 1, 1, 12, 'NEUTRAL'),
  (1, 'TEACHING', '教务流程', 'EXAM_PUBLISHED', '考试发布',     'calendar',  '#8b5cf6', '["ORG_UNIT"]', 1, 1, 13, 'NEUTRAL')
ON DUPLICATE KEY UPDATE type_name = VALUES(type_name);

-- 3. 事件触发器 (event_triggers) — 主体映射
-- 成绩提交: 主体 USER=createdBy (让管理员知道谁提交了)
-- 成绩审核通过/退回: 主体 USER=createdBy (通知原提交教师)
-- 成绩发布: 主体 ORG_UNIT=orgUnitId (通知班级学生)
-- 考试发布: 主体 ORG_UNIT=orgUnitId (通知班级; 若为全学期则用 semesterId)
INSERT INTO event_triggers (name, trigger_point_code, event_type_mode, event_type_code, subjects_json, is_enabled, sort_order, tenant_id)
VALUES
  ('成绩提交通知',   'GRADE_SUBMITTED', 'FIXED', 'GRADE_SUBMITTED', '[{"type":"USER","idSource":"createdBy","nameSource":"batchName"}]', 1, 10, 1),
  ('成绩审核通过通知','GRADE_APPROVED', 'FIXED', 'GRADE_APPROVED', '[{"type":"USER","idSource":"createdBy","nameSource":"batchName"}]', 1, 11, 1),
  ('成绩发布通知',   'GRADE_PUBLISHED', 'FIXED', 'GRADE_PUBLISHED', '[{"type":"ORG_UNIT","idSource":"orgUnitId","nameSource":"batchName"}]', 1, 12, 1),
  ('考试发布通知',   'EXAM_PUBLISHED', 'FIXED', 'EXAM_PUBLISHED', '[{"type":"ORG_UNIT","idSource":"orgUnitId","nameSource":"batchName"}]', 1, 13, 1);

-- 4. 订阅规则 (msg_subscription_rules) — 站内通知分发
-- GRADE_SUBMITTED → 推送给具有审核职责的管理员 (DEPT_ADMIN / teaching_admin 类角色)
-- GRADE_APPROVED  → 推送给主体本身 (即 createdBy 这位教师)
-- GRADE_PUBLISHED → 推送给主体班级的相关用户 (班级下属用户)
-- EXAM_PUBLISHED  → 推送给相关班级用户
INSERT INTO msg_subscription_rules (tenant_id, rule_name, event_category, event_type, target_mode, target_config, channel, is_enabled, sort_order)
VALUES
  (1, '成绩待审核-通知审核人',  'TEACHING', 'GRADE_SUBMITTED', 'BY_ROLE',    '["DEPT_ADMIN","ACADEMIC_ADMIN","SUPER_ADMIN"]', 'IN_APP', 1, 10),
  (1, '成绩审核通过-通知教师',  'TEACHING', 'GRADE_APPROVED', 'BY_SUBJECT', '{}',                                            'IN_APP', 1, 11),
  (1, '成绩发布-通知班级用户',  'TEACHING', 'GRADE_PUBLISHED', 'BY_RELATED', '{}',                                            'IN_APP', 1, 12),
  (1, '考试发布-通知班级用户',  'TEACHING', 'EXAM_PUBLISHED', 'BY_RELATED', '{}',                                            'IN_APP', 1, 13);
