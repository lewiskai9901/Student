package com.school.management.application.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Spring Application Event — 实体事件创建后发布
 * 用于解耦事件创建与后续通知逻辑
 */
@Getter
@RequiredArgsConstructor
public class EntityEventCreatedNotification {
    private final Long eventId;
    private final String subjectType;
    private final Long subjectId;
    private final String subjectName;
    private final String eventType;
    private final String eventLabel;
}
