-- 订阅规则升级到 v3 目标模式
-- 规则 4 (GRADE_PUBLISHED 班级层) : BY_RELATED {} -> BY_RELATION{admin, org_unit}
-- 规则 5 (EXAM_PUBLISHED)          : 同上
-- 新增规则 (GRADE_PUBLISHED 学生层): BY_FEATURE{isLearner, receivesPersonalGrade}

UPDATE msg_subscription_rules
SET target_mode = 'BY_RELATION',
    target_config = '{"relation":"admin","resource_type":"org_unit","direction":"inward"}'
WHERE id = 4 AND target_mode = 'BY_RELATED';

UPDATE msg_subscription_rules
SET target_mode = 'BY_RELATION',
    target_config = '{"relation":"admin","resource_type":"org_unit","direction":"inward"}'
WHERE id = 5 AND target_mode = 'BY_RELATED';

-- 新增 BY_FEATURE 规则: 成绩发布通知所有学生
INSERT INTO msg_subscription_rules
  (rule_name, event_type, target_mode, target_config, is_enabled, tenant_id, created_at)
SELECT '成绩发布-通知学生本人', 'GRADE_PUBLISHED', 'BY_FEATURE',
       '{"features":["isLearner","receivesPersonalGrade"]}', 1, 1, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM msg_subscription_rules
  WHERE event_type='GRADE_PUBLISHED' AND target_mode='BY_FEATURE'
);
