-- ============================================================
-- D2: 事件类型 + 订阅规则 + 消息模板
-- 当学生迟到/违纪时, 由订阅规则把事件推给班主任
-- ============================================================

-- ========== 1. 事件类型 ==========
INSERT INTO entity_event_types (tenant_id, category_code, category_name, type_code, type_name,
                                 has_score, has_severity, applicable_subjects, is_system, is_enabled,
                                 category_polarity, industry, color, icon)
VALUES
(0, 'STUDENT', '学生事件', 'STUDENT_LATE', '学生迟到',
 0, 1, '["USER"]', 0, 1, 'NEGATIVE', 'EDU', '#f59e0b', 'clock'),
(0, 'STUDENT', '学生事件', 'STUDENT_VIOLATION', '学生违纪',
 1, 1, '["USER"]', 0, 1, 'NEGATIVE', 'EDU', '#dc2626', 'alert-triangle');

-- ========== 2. 消息模板 ==========
INSERT INTO msg_templates (tenant_id, template_code, template_name, title_template, content_template, is_enabled)
VALUES
(0, 'TPL_STUDENT_LATE', '学生迟到通知',
 '迟到提醒: {{subjectName}}',
 '您班学生 {{subjectName}} 在 {{occurredAt}} 迟到. 检查员: {{createdByName}}.',
 1),
(0, 'TPL_STUDENT_VIOLATION', '学生违纪通知',
 '违纪通报: {{subjectName}}',
 '您班学生 {{subjectName}} 在校园内被发现违纪行为. 详情: {{eventLabel}}. 检查员: {{createdByName}}.',
 1);

SET @TPL_LATE = (SELECT id FROM msg_templates WHERE template_code='TPL_STUDENT_LATE');
SET @TPL_VIO  = (SELECT id FROM msg_templates WHERE template_code='TPL_STUDENT_VIOLATION');

-- ========== 3. 订阅规则 — 用 BY_RELATION 把"学生事件"推到"班级班主任" ==========
-- 链路: subject(学生 USER) → 学生在班级 (member of org_unit) → 班主任 admin_of 班级 (反向)
-- 现有 ByRelationTargetMode 支持 outward 方向: 从 subject 沿 relation 到 resource.
-- 这里用 inward direction: 资源类型 user, relation = guardian-like? 实际配置:
-- 取学生的 班级 → 班级的 admin → 推给 admin (班主任)
-- 即: subject_user 通过 'member' 找 org_unit, 再通过 'admin' inward 到 user.
-- 但 BY_RELATION 一次只支持一条链, 我们用更直接的方式:
-- 班主任=user, 资源=org_unit 班级. 当事件 subject=学生 时, 学生的 primary_org_unit_id=班级,
-- 直接查 access_relations WHERE resource=该班级 AND relation='admin'.

-- 简化: 用 BY_FEATURE 不行 (不能动态用学生班级查管理员)
-- 最贴合: BY_RELATION 配置 relation='admin', direction='inward', resource_type='org_unit'
-- 但事件主体是学生 (USER 类型), 不是班级.
--
-- 正确做法: 通过 EntityEventRelation 把"学生 + 班级" 一起进事件, 然后规则用 BY_RELATION
-- 沿"班级关联实体"找 admin. 当前简化: 直接订阅者用 BY_ROLE+CLASS_TEACHER (粗一点, 全部班主任都收到).

INSERT INTO msg_subscription_rules (tenant_id, rule_name, event_category, event_type,
                                     target_mode, target_config, channel, template_id, is_enabled)
VALUES
(0, '学生迟到 → 通知班主任 (全员)', 'STUDENT', 'STUDENT_LATE',
 'BY_ROLE', '["CLASS_TEACHER"]', 'IN_APP', @TPL_LATE, 1),
(0, '学生违纪 → 通知班主任 (全员)', 'STUDENT', 'STUDENT_VIOLATION',
 'BY_ROLE', '["CLASS_TEACHER"]', 'IN_APP', @TPL_VIO, 1);

-- ========== 4. 验证 ==========
SELECT '=== 事件类型 ===' AS '';
SELECT type_code, type_name, applicable_subjects FROM entity_event_types WHERE type_code IN ('STUDENT_LATE','STUDENT_VIOLATION');
SELECT '=== 消息模板 ===' AS '';
SELECT id, template_code, template_name FROM msg_templates WHERE template_code LIKE 'TPL_STUDENT%';
SELECT '=== 订阅规则 ===' AS '';
SELECT id, rule_name, event_type, target_mode, target_config, channel, template_id FROM msg_subscription_rules WHERE event_type IN ('STUDENT_LATE','STUDENT_VIOLATION');
