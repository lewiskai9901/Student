package com.school.management.application.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.organization.event.*;
import com.school.management.infrastructure.access.UserContextHolder;
import com.school.management.infrastructure.event.DomainEventStore;
import com.school.management.infrastructure.external.NotificationService;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;


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
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

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

        // 写 org_change_logs (审计)
        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("unitCode", event.getUnitCode());
        changes.put("unitName", event.getUnitName());
        changes.put("unitType", event.getUnitType());
        writeOrgChangeLog("ORG_UNIT", event.getOrgUnitId(), "CREATE", changes, null);

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

        // 写 org_change_logs (审计) — 简单记录"发生了 update", 字段级 diff 留给上层 service 填
        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("event", "OrgUnitUpdated");
        writeOrgChangeLog("ORG_UNIT", event.getOrgUnitId(), "UPDATE", changes, null);
    }

    /**
     * 处理组织删除事件 (P8-2 新加).
     */
    @Async
    @EventListener
    public void handle(OrgUnitDeletedEvent event) {
        log.info("Handling OrgUnitDeletedEvent: unitId={} ({})",
            event.getOrgUnitId(), event.getUnitName());

        eventStore.store(event);

        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("unitCode", event.getUnitCode());
        changes.put("unitName", event.getUnitName());
        changes.put("unitType", event.getUnitType());
        changes.put("parentId", event.getParentId());
        writeOrgChangeLog("ORG_UNIT", event.getOrgUnitId(), "DELETE", changes, null);

        saveOperationLog("DELETE", "ORG_UNIT", event.getOrgUnitId(),
            "删除组织单元: " + event.getUnitName() + " (" + event.getUnitCode() + ")");
    }

    /**
     * 处理组织合并事件 (P8-2 新加).
     */
    @Async
    @EventListener
    public void handle(OrgUnitMergedEvent event) {
        log.info("Handling OrgUnitMergedEvent: source={} → target={}",
            event.getSourceOrgUnitId(), event.getTargetOrgUnitId());

        eventStore.store(event);

        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("sourceUnitCode", event.getSourceUnitCode());
        changes.put("targetUnitCode", event.getTargetUnitCode());
        changes.put("targetOrgUnitId", event.getTargetOrgUnitId());
        writeOrgChangeLog("ORG_UNIT", event.getSourceOrgUnitId(), "MERGE", changes, event.getReason());
    }

    /**
     * 处理组织拆分事件 (P8-2 新加).
     */
    @Async
    @EventListener
    public void handle(OrgUnitSplitEvent event) {
        log.info("Handling OrgUnitSplitEvent: source={} → {} new units",
            event.getSourceOrgUnitId(), event.getNewOrgUnitIds().size());

        eventStore.store(event);

        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("sourceUnitCode", event.getSourceUnitCode());
        changes.put("newOrgUnitIds", event.getNewOrgUnitIds());
        writeOrgChangeLog("ORG_UNIT", event.getSourceOrgUnitId(), "SPLIT", changes, event.getReason());
    }

    /**
     * 写 org_change_logs 审计表 — 直接 JDBC, 不走 MyBatis 防递归.
     *
     * @param entityType  实体类型 (ORG_UNIT / ROLE / USER_ROLE 等)
     * @param entityId    实体 id
     * @param changeType  CREATE / UPDATE / DELETE / MOVE
     * @param changes     变更详情 (序列化为 JSON)
     * @param reason      可选原因
     */
    private void writeOrgChangeLog(String entityType, Long entityId, String changeType,
                                    Map<String, Object> changes, String reason) {
        try {
            Long operatorId = UserContextHolder.getUserId();
            if (operatorId == null) operatorId = 0L;   // 系统调用 fallback
            String operatorName = UserContextHolder.getUsername();
            String changesJson = objectMapper.writeValueAsString(changes != null ? changes : Map.of());
            jdbcTemplate.update(
                "INSERT INTO org_change_logs (entity_type, entity_id, change_type, changes, " +
                "reason, operator_id, operator_name, tenant_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                entityType, entityId, changeType, changesJson,
                reason, operatorId, operatorName, 1L);
        } catch (Exception e) {
            log.warn("[OrgChangeLog] write failed for {} {}: {}",
                entityType, entityId, e.getMessage());
        }
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
