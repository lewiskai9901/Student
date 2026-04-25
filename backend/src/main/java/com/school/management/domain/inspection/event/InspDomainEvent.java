package com.school.management.domain.inspection.event;

import com.school.management.domain.shared.event.DomainEvent;

/**
 * 检查平台领域事件标记接口。
 * 所有 事件实现此接口，用于与 V6 事件隔离。
 * NotificationRuleEngine 和 WebhookDispatcher 仅监听此类型子类。
 */
public interface InspDomainEvent extends DomainEvent {
}
