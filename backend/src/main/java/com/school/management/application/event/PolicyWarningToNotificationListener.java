package com.school.management.application.event;

import com.school.management.infrastructure.extension.Violation;
import com.school.management.infrastructure.extension.event.PolicyWarningEvent;
import com.school.management.infrastructure.extension.plugins.core.constants.CoreTriggerPoints;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * M4.3: Policy SPI WARN/INFO 违规 → 消息通知桥接.
 *
 * 监听 {@link PolicyWarningEvent} (PolicyRegistry 在非 BLOCK 违规时发布),
 * 每条违规独立 fire 一次 POLICY_WARNING 触发点, 走 TriggerService → MessageDispatcher
 * 统一管道. 实际是否产生通知取决于 admin 是否在 msg_subscription_rules 里配置了
 * POLICY_WARNING 的订阅规则 (target_mode / targetConfig 决定收件人).
 *
 * context payload 字段 (与 V20260425_2 seed 的 context_schema 一致):
 *   policyCode  — Violation.code(), 前端可据此做 i18n / 告警路由
 *   severity    — WARN / INFO
 *   message     — 人类可读描述
 *   entityType  — PolicyContext.entityType, 领域码 (place/org_unit/user/...)
 *   phase       — PolicyContext.phase, 操作阶段 (BEFORE_CREATE/AFTER_CHECKIN/...)
 *
 * 额外: subjectId/subjectType 用 entityType 兜底, 使 subjects_json 里的 USER/ORG_UNIT
 *       主体解析仍有机会命中 (即使原始 PolicyContext.payload 格式各异).
 *       TODO: 后续可在 Policy SPI 增加 subject 约定字段更可靠.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyWarningToNotificationListener {

    private final TriggerService triggerService;

    @EventListener
    public void onPolicyWarning(PolicyWarningEvent event) {
        for (Violation v : event.getViolations()) {
            Map<String, Object> payload = new HashMap<>();
            payload.put("policyCode", v.code());
            payload.put("severity", v.severity().name());
            payload.put("message", v.message());
            payload.put("entityType", event.getContext().entityType());
            payload.put("phase", event.getContext().phase());
            // 供 trigger subjects_json 解析兜底 (subject_type=USER, idSource=subjectId)
            payload.put("subjectType", "USER");
            try {
                triggerService.fire(CoreTriggerPoints.POLICY_WARNING, payload);
            } catch (Exception e) {
                // trigger fire 内部已经兜底异常, 此处只作最外层日志防御
                log.warn("[PolicyWarningToNotification] fire POLICY_WARNING 失败 code={}: {}",
                    v.code(), e.getMessage());
            }
        }
    }
}
