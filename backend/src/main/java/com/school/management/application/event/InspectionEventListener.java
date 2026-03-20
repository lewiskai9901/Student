package com.school.management.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 检查事件监听器 — 将关键实体事件转化为通知
 * 简化实现：目前只记录日志，后续扩展为站内消息/WebSocket/微信推送
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InspectionEventListener {

    /**
     * 监听实体事件创建通知，根据事件类型路由到对应的通知逻辑
     */
    @Async
    @EventListener
    public void onEntityEventCreated(EntityEventCreatedNotification notification) {
        String eventType = notification.getEventType();

        // 违规事件 → 通知相关管理者
        if ("INSP_VIOLATION".equals(eventType)) {
            log.info("[通知] 违规事件: {} {} - {}",
                    notification.getSubjectType(), notification.getSubjectName(), notification.getEventLabel());
            // TODO: 查找该主体的管理者（通过 access_relations），发送站内消息
            // TODO: 可扩展为 WebSocket 推送或微信模板消息
        }

        // 评级事件 → 通知相关组织
        if ("INSP_RATING".equals(eventType)) {
            log.info("[通知] 评级事件: {} {} - {}",
                    notification.getSubjectType(), notification.getSubjectName(), notification.getEventLabel());
            // TODO: 通知组织负责人
        }

        // 检查完成事件 → 通知被检查对象
        if ("INSP_COMPLETED".equals(eventType)) {
            log.info("[通知] 检查完成: {} {} - {}",
                    notification.getSubjectType(), notification.getSubjectName(), notification.getEventLabel());
            // TODO: 通知被检查对象查看结果
        }
    }
}
