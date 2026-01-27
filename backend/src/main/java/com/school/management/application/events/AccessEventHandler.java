package com.school.management.application.events;

import com.school.management.domain.access.event.*;
import com.school.management.infrastructure.event.DomainEventStore;
import com.school.management.infrastructure.external.NotificationService;
import com.school.management.infrastructure.audit.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;


/**
 * 权限管理领域事件处理器
 * 负责处理权限相关的领域事件，包括：
 * - 角色创建
 * - 角色权限变更
 * - 用户角色分配
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessEventHandler {

    private final DomainEventStore eventStore;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    /**
     * 处理角色创建事件
     */
    @Async
    @EventListener
    public void handle(RoleCreatedEvent event) {
        log.info("Handling RoleCreatedEvent: roleId={}, roleCode={}",
                 event.getRoleId(), event.getRoleCode());

        eventStore.store(event);

        // 记录审计日志
        saveOperationLog("CREATE", "ROLE", event.getRoleId(),
                "创建角色: " + event.getRoleName() + " (" + event.getRoleCode() + ")");

        log.info("角色创建事件处理完成: roleId={}", event.getRoleId());
    }

    /**
     * 处理角色权限变更事件
     */
    @Async
    @EventListener
    public void handle(RolePermissionsChangedEvent event) {
        log.info("Handling RolePermissionsChangedEvent: roleId={}, addedCount={}, removedCount={}",
                 event.getRoleId(),
                 event.getAddedPermissionIds().size(),
                 event.getRemovedPermissionIds().size());

        eventStore.store(event);

        // 刷新权限缓存
        handlePermissionsChanged(event);
    }

    /**
     * 处理用户角色分配事件
     */
    @Async
    @EventListener
    public void handle(UserRoleAssignedEvent event) {
        log.info("Handling UserRoleAssignedEvent: userId={}, roleId={}, orgUnitId={}",
                 event.getUserId(), event.getRoleId(), event.getOrgUnitId());

        eventStore.store(event);

        // 处理用户角色变更
        handleUserRoleAssigned(event);
    }

    /**
     * 处理权限变更
     * - 清除该角色用户的权限缓存
     * - 记录审计日志
     */
    private void handlePermissionsChanged(RolePermissionsChangedEvent event) {
        log.info("Role {} permissions changed, added={}, removed={}",
                event.getRoleId(), event.getAddedPermissionIds().size(), event.getRemovedPermissionIds().size());

        // 记录审计日志
        String changeDesc = String.format("角色权限变更: 新增%d个权限, 移除%d个权限",
                event.getAddedPermissionIds().size(), event.getRemovedPermissionIds().size());
        saveOperationLog("UPDATE", "ROLE_PERMISSION", event.getRoleId(), changeDesc);

        log.info("角色权限变更处理完成: roleId={}", event.getRoleId());
    }

    /**
     * 处理用户角色分配
     * - 清除用户权限缓存
     * - 发送通知给用户
     * - 记录审计日志
     */
    private void handleUserRoleAssigned(UserRoleAssignedEvent event) {
        log.info("User {} assigned role {} in org {}",
                 event.getUserId(), event.getRoleId(), event.getOrgUnitId());

        // 发送通知给用户
        notificationService.sendInAppMessage(
                event.getUserId(),
                "角色分配通知",
                String.format("您已被分配新角色(ID:%d)，权限已更新", event.getRoleId()),
                NotificationService.MessageType.SYSTEM_NOTICE
        );

        // 记录审计日志
        saveOperationLog("ASSIGN", "USER_ROLE", event.getUserId(),
                String.format("分配角色: userId=%d, roleId=%d, orgUnitId=%d",
                        event.getUserId(), event.getRoleId(), event.getOrgUnitId()));

        log.info("用户角色分配处理完成: userId={}, roleId={}", event.getUserId(), event.getRoleId());
    }

    /**
     * 保存操作日志
     */
    private void saveOperationLog(String operationType, String targetType, Long targetId, String description) {
        try {
            auditLogService.logCreate(targetType, targetId != null ? String.valueOf(targetId) : "", description, null, description);
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }
}
