package com.school.management.application.events;

import com.school.management.domain.organization.event.*;
import com.school.management.infrastructure.event.DomainEventStore;
import com.school.management.infrastructure.external.NotificationService;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


/**
 * 组织管理领域事件处理器
 * 负责处理组织架构相关的领域事件，包括：
 * - 组织单元创建/更新
 *
 * 注意：班级创建/状态变更/教师任职事件已迁移到 StudentEventHandler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrganizationEventHandler {

    private final DomainEventStore eventStore;
    private final NotificationService notificationService;
    private final ActivityEventPublisher activityEventPublisher;

    /**
     * 处理组织单元创建事件
     */
    @Async
    @EventListener
    public void handle(OrgUnitCreatedEvent event) {
        log.info("Handling OrgUnitCreatedEvent: unitCode={}, unitName={}",
                 event.getUnitCode(), event.getUnitName());

        // 存储事件
        eventStore.store(event);

        // 记录操作日志
        saveOperationLog("CREATE", "ORG_UNIT", event.getOrgUnitId(),
                "创建组织单元: " + event.getUnitName() + " (" + event.getUnitCode() + ")");

        // TODO: resolve actual admin userId(s) via role-based lookup and send notification
        log.debug("Skipping admin notification for OrgUnitCreated (no target user resolution yet): unitName={}", event.getUnitName());
    }

    /**
     * 处理组织单元更新事件
     */
    @Async
    @EventListener
    public void handle(OrgUnitUpdatedEvent event) {
        log.info("Handling OrgUnitUpdatedEvent: unitId={}", event.getOrgUnitId());

        eventStore.store(event);
    }

    /**
     * 保存操作日志
     */
    private void saveOperationLog(String action, String resourceType, Long resourceId, String description) {
        try {
            activityEventPublisher.newEvent("organization", resourceType, action, description)
                .resourceId(resourceId != null ? resourceId.toString() : "")
                .publish();
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }
}
