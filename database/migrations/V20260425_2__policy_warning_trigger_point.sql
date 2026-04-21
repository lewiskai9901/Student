-- V20260425_2: 注册 POLICY_WARNING 触发点 (M4.3)
-- 用途: Policy SPI 产生的 WARN/INFO 违规会通过 PolicyWarningToNotificationListener
--       调 triggerService.fire("POLICY_WARNING", {...}). 此 seed 让 TriggerService
--       在启动自检 (TriggerPipelineHealthCheck) 时认可这是一个合法触发点.
-- 不 seed event_trigger / subscription_rule: 订阅规则由 admin 按业务需求配置
-- (避免强绑默认收件人造成骚扰).
INSERT IGNORE INTO trigger_points
    (module_code, module_name, point_code, point_name, description, context_schema, tenant_id)
VALUES (
    'policy',
    'Policy 违规',
    'POLICY_WARNING',
    'Policy 违规事件',
    'Policy SPI 产生的 WARN/INFO 级违规, 业务规则软约束触发, 可配置订阅通知',
    '{"policyCode":{"type":"String","label":"策略码"},"severity":{"type":"String","label":"严重度"},"message":{"type":"String","label":"描述"},"entityType":{"type":"String","label":"实体类型"},"phase":{"type":"String","label":"阶段"},"subjectType":{"type":"String","label":"主体类型"}}',
    1
);
