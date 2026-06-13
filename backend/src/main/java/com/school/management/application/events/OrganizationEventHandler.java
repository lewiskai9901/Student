package com.school.management.application.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.organization.event.*;
import com.school.management.infrastructure.access.UserContextHolder;
import com.school.management.infrastructure.event.DomainEventStore;
import com.school.management.infrastructure.external.NotificationService;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
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

        // 通知管理员 (站内信)
        notifyAdmins("组织单元创建",
                "新建组织单元: " + event.getUnitName() + " (" + event.getUnitCode() + ")");
    }

    /**
     * 处理组织单元更新事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
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
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
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

        notifyAdmins("组织单元删除",
            "组织单元已删除: " + event.getUnitName() + " (" + event.getUnitCode() + ")");
    }

    /**
     * 处理组织合并事件 (P8-2 新加).
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(OrgUnitMergedEvent event) {
        log.info("Handling OrgUnitMergedEvent: source={} → target={}",
            event.getSourceOrgUnitId(), event.getTargetOrgUnitId());

        eventStore.store(event);

        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("sourceUnitCode", event.getSourceUnitCode());
        changes.put("targetUnitCode", event.getTargetUnitCode());
        changes.put("targetOrgUnitId", event.getTargetOrgUnitId());
        writeOrgChangeLog("ORG_UNIT", event.getSourceOrgUnitId(), "MERGE", changes, event.getReason());

        notifyAdmins("组织单元合并",
            "组织合并: " + event.getSourceUnitCode() + " → " + event.getTargetUnitCode()
                + (event.getReason() != null ? " (原因: " + event.getReason() + ")" : ""));
    }

    /**
     * 处理组织拆分事件 (P8-2 新加).
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(OrgUnitSplitEvent event) {
        log.info("Handling OrgUnitSplitEvent: source={} → {} new units",
            event.getSourceOrgUnitId(), event.getNewOrgUnitIds().size());

        eventStore.store(event);

        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("sourceUnitCode", event.getSourceUnitCode());
        changes.put("newOrgUnitIds", event.getNewOrgUnitIds());
        writeOrgChangeLog("ORG_UNIT", event.getSourceOrgUnitId(), "SPLIT", changes, event.getReason());

        notifyAdmins("组织单元拆分",
            "组织拆分: " + event.getSourceUnitCode() + " 拆出 " + event.getNewOrgUnitIds().size() + " 个新单元"
                + (event.getReason() != null ? " (原因: " + event.getReason() + ")" : ""));
    }

    /**
     * 通知管理员组织结构变更 (站内信)。
     *
     * <p>收件人 = SUPER_ADMIN / TENANT_ADMIN 角色持有者 (role-based admin lookup),
     * 排除操作人自己 (不给自己发"你刚做的操作")。组织结构变更 (创建/删除/合并/拆分)
     * 是管理级操作, 管理员需知悉; 改名 (update) 仅审计不通知, 避免噪音。
     *
     * <p>站内信走 {@code system_messages} 表 (NotificationService.sendInAppMessage 真实落库),
     * 微信模板通道当前是空壳故不触发。本方法静默失败 (通知非关键路径, 不应影响主事务)。
     */
    private void notifyAdmins(String title, String content) {
        try {
            Long actorId = UserContextHolder.getUserId();
            java.util.List<Long> recipients = jdbcTemplate.queryForList(
                "SELECT DISTINCT ur.user_id FROM user_roles ur " +
                "JOIN roles r ON r.id = ur.role_id " +
                "WHERE r.role_code IN ('SUPER_ADMIN','TENANT_ADMIN') AND r.deleted = 0",
                Long.class);
            recipients.removeIf(uid -> uid == null || uid.equals(actorId));
            if (recipients.isEmpty()) return;
            notificationService.sendInAppMessageBatch(recipients, title, content, "ORG_STRUCTURE_CHANGE");
        } catch (Exception e) {
            log.warn("[OrgNotify] admin notification failed ({}): {}", title, e.getMessage());
        }
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
