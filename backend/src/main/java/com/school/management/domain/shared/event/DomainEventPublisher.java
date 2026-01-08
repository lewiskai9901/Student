package com.school.management.domain.shared.event;

/**
 * 领域事件发布器接口
 */
public interface DomainEventPublisher {

    /**
     * 发布领域事件
     * @param event 领域事件
     */
    void publish(DomainEvent event);

    /**
     * 发布多个领域事件
     * @param events 领域事件列表
     */
    default void publishAll(Iterable<? extends DomainEvent> events) {
        events.forEach(this::publish);
    }
}
