package com.school.management.domain.access.event;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/** 关系授权申请审批事件(grant 进入 pending 队列时发出). */
@Data
@RequiredArgsConstructor
public class RelationApprovalRequestedEvent {
    private final Long pendingId;
    private final String resourceType;
    private final Long resourceId;
    private final String relation;
    private final String subjectType;
    private final Long subjectId;
    private final Long requestedBy;
}
