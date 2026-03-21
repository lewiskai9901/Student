package com.school.management.application.event;

import com.school.management.application.message.MessageDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 检查事件监听器 — 将关键实体事件转化为通知
 * 通过 MessageDispatcher 根据订阅规则分发站内消息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InspectionEventListener {

    private final MessageDispatcher messageDispatcher;

    /**
     * 监听实体事件创建通知，根据订阅规则分发站内消息
     */
    @Async
    @EventListener
    public void onEntityEventCreated(EntityEventCreatedNotification notification) {
        String eventType = notification.getEventType();
        log.info("[事件通知] 收到事件: {} - {} {}",
                eventType, notification.getSubjectType(), notification.getSubjectName());

        // 通过消息分发器根据订阅规则路由消息
        messageDispatcher.dispatch(notification);
    }
}
