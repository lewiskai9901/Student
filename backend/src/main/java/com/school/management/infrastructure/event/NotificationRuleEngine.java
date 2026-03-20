package com.school.management.infrastructure.event;

import com.school.management.domain.inspection.event.v7.InspV7DomainEvent;
import com.school.management.domain.inspection.model.v7.platform.NotificationRule;
import com.school.management.domain.inspection.repository.v7.NotificationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知规则引擎 — 监听所有 InspV7DomainEvent，按规则触发多渠道通知。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationRuleEngine {

    private final NotificationRuleRepository notificationRuleRepository;

    @Async
    @EventListener
    public void onInspV7Event(InspV7DomainEvent event) {
        String eventType = event.getClass().getSimpleName();
        log.debug("NotificationRuleEngine received event: {}", eventType);

        try {
            List<NotificationRule> rules = notificationRuleRepository.findAllEnabled();
            for (NotificationRule rule : rules) {
                if (matchesEvent(rule, eventType) && evaluateCondition(rule, event)) {
                    dispatchNotification(rule, event);
                }
            }
        } catch (Exception e) {
            log.error("NotificationRuleEngine failed for event {}: {}", eventType, e.getMessage(), e);
        }
    }

    private boolean matchesEvent(NotificationRule rule, String eventType) {
        return eventType.equals(rule.getEventType());
    }

    private boolean evaluateCondition(NotificationRule rule, InspV7DomainEvent event) {
        // Condition is a JSON expression — for now, treat null/empty as "always match"
        String condition = rule.getCondition();
        if (condition == null || condition.isBlank()) {
            return true;
        }
        // TODO: implement JSON condition evaluation (e.g., SpEL or simple field matching)
        log.debug("Condition evaluation not yet implemented, defaulting to match: {}", condition);
        return true;
    }

    private void dispatchNotification(NotificationRule rule, InspV7DomainEvent event) {
        String channels = rule.getChannels();
        log.info("Dispatching notification: rule='{}', event={}, channels={}",
                rule.getRuleName(), event.getClass().getSimpleName(), channels);

        // Parse channels JSON array (e.g., ["IN_APP", "EMAIL"])
        if (channels != null) {
            if (channels.contains("IN_APP")) {
                sendInAppNotification(rule, event);
            }
            if (channels.contains("EMAIL")) {
                sendEmailNotification(rule, event);
            }
            if (channels.contains("SMS")) {
                sendSmsNotification(rule, event);
            }
            if (channels.contains("WECHAT")) {
                sendWechatNotification(rule, event);
            }
        }
    }

    private void sendInAppNotification(NotificationRule rule, InspV7DomainEvent event) {
        log.info("IN_APP notification sent: rule='{}', event={}", rule.getRuleName(), event.getClass().getSimpleName());
        // TODO: integrate with in-app notification service (e.g., WebSocket push)
    }

    private void sendEmailNotification(NotificationRule rule, InspV7DomainEvent event) {
        log.info("EMAIL notification queued: rule='{}', event={}", rule.getRuleName(), event.getClass().getSimpleName());
        // TODO: integrate with email service (e.g., Spring Mail)
    }

    private void sendSmsNotification(NotificationRule rule, InspV7DomainEvent event) {
        log.info("SMS notification queued: rule='{}', event={}", rule.getRuleName(), event.getClass().getSimpleName());
        // TODO: integrate with SMS gateway
    }

    private void sendWechatNotification(NotificationRule rule, InspV7DomainEvent event) {
        log.info("WECHAT notification queued: rule='{}', event={}", rule.getRuleName(), event.getClass().getSimpleName());
        // TODO: integrate with WeChat template message API
    }
}
