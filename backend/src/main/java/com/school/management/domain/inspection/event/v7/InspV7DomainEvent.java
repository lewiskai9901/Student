package com.school.management.domain.inspection.event.v7;

import com.school.management.domain.shared.event.DomainEvent;

/**
 * V7 检查平台领域事件标记接口。
 * 所有 V7 事件实现此接口，用于与 V6 事件隔离。
 * NotificationRuleEngine 和 WebhookDispatcher 仅监听此类型子类。
 */
public interface InspV7DomainEvent extends DomainEvent {
}
