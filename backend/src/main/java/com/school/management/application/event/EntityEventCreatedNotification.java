package com.school.management.application.event;

import com.school.management.domain.event.model.EntityEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Spring Application Event — 实体事件创建后发布
 * 用于解耦事件创建与后续通知分发
 *
 * 由 TriggerService 在 INSERT entity_events 后发布,
 * 由 EntityEventDispatchListener 监听并转发给 MessageDispatcher
 */
@Getter
@RequiredArgsConstructor
public class EntityEventCreatedNotification {
    private final EntityEvent event;
}
