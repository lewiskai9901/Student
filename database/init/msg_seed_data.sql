-- Seed message templates
INSERT IGNORE INTO msg_templates (tenant_id, template_code, template_name, title_template, content_template, is_system, is_enabled, created_at) VALUES
(0, 'PUBLISH', '发布通知', '{{resourceType}} 已发布', '{{operatorName}} 发布了 {{resourceType}}', 1, 1, NOW()),
(0, 'APPROVE', '审批通知', '{{resourceType}} 审批通过', '{{operatorName}} 已审批 {{resourceType}}', 1, 1, NOW()),
(0, 'REJECT', '驳回通知', '{{resourceType}} 已驳回', '{{operatorName}} 已驳回 {{resourceType}}', 1, 1, NOW()),
(0, 'ENROLL', '招生通知', '新学生报到', '{{studentName}} 已完成报到注册', 1, 1, NOW()),
(0, 'SYSTEM', '系统通知', '{{title}}', '{{content}}', 1, 1, NOW()),
(0, 'VIOLATION', '违规通知', '{{subjectName}} 发生违规', '{{subjectName}} 于 {{time}} 被记录违规', 1, 1, NOW()),
(0, 'GRADE', '评级通知', '{{subjectName}} 评级结果', '{{subjectName}} 检查评级：{{grade}}', 1, 1, NOW());

-- Seed default subscription rule: admins get all events
INSERT IGNORE INTO msg_subscription_rules (tenant_id, rule_name, event_category, event_type, target_mode, target_config, channel, is_enabled, sort_order, created_at) VALUES
(0, '管理员通知(全部事件)', NULL, NULL, 'BY_ROLE', '["SUPER_ADMIN","ADMIN"]', 'IN_APP', 1, 1, NOW());
