package com.school.management.application.events;

import com.school.management.domain.access.event.*;
import com.school.management.domain.access.repository.AccessRelationRepository;
import com.school.management.domain.access.repository.RoleRepository;
import com.school.management.domain.access.service.PolicyEnforcementService;
import com.school.management.infrastructure.event.DomainEventStore;
import com.school.management.infrastructure.external.NotificationService;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
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
    private final ActivityEventPublisher activityEventPublisher;
    private final PolicyEnforcementService policyEnforcementService;
    private final RoleRepository roleRepository;
    private final AccessRelationRepository accessRelationRepository;

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
        log.info("Handling UserRoleAssignedEvent: userId={}, roleId={}, scopeType={}, scopeId={}",
                 event.getUserId(), event.getRoleId(), event.getScopeType(), event.getScopeId());

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

        // 同步 Casbin enforcer 内存策略 — role_permissions 已经被 application service 写入 DB,
        // 这里只 reload 一次让内存与 DB 一致. 比逐条 add/remove 简单, 适合批量变更场景.
        try {
            policyEnforcementService.syncFromDatabase();
            log.debug("[Casbin] in-memory policy reloaded after role {} permission change",
                event.getRoleId());
        } catch (Exception e) {
            log.error("[Casbin] failed to reload policy after role permission change: {}",
                e.getMessage(), e);
        }

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
        log.info("User {} assigned role {} with scope {}:{}",
                 event.getUserId(), event.getRoleId(), event.getScopeType(), event.getScopeId());

        // 同步到 Casbin enforcer 内存策略 — 单条 addGroupingPolicy 比全量 reload 快得多.
        // tenantId 从 Role 取(每个 Role 属于一个租户).
        try {
            roleRepository.findById(event.getRoleId()).ifPresent(role -> {
                policyEnforcementService.assignRole(event.getUserId(), role.getRoleCode(),
                    role.getTenantId());
                log.debug("[Casbin] grouping policy added: user={}, role={}",
                    event.getUserId(), role.getRoleCode());
            });
        } catch (Exception e) {
            log.error("[Casbin] failed to add grouping policy for user {}: {}",
                event.getUserId(), e.getMessage(), e);
        }

        // ORG_UNIT scope 的角色分配 — 自动写 access_relations 让 ReBAC 子查询和 plugin
        // Resolver 反查能命中. relation 用 role_code, resource_type=org_unit, resource_id=scope_id.
        // 这是单一写入路径, 业务层只调 assignRoleToUserWithScope, access_relations 自动写.
        if ("ORG_UNIT".equals(event.getScopeType()) && event.getScopeId() != null && event.getScopeId() > 0) {
            try {
                roleRepository.findById(event.getRoleId()).ifPresent(role -> {
                    java.util.Optional<Long> existing = accessRelationRepository.findActiveByTuple(
                        "org_unit", event.getScopeId(),
                        role.getRoleCode(),
                        "user", event.getUserId());
                    if (existing.isPresent()) {
                        log.debug("[AccessRelation] already exists for user={} role={} org={}",
                            event.getUserId(), role.getRoleCode(), event.getScopeId());
                        return;
                    }
                    AccessRelationRepository.InsertDirectCommand cmd =
                        new AccessRelationRepository.InsertDirectCommand(
                            "org_unit", event.getScopeId(),
                            role.getRoleCode(),
                            "user", event.getUserId(),
                            false, null, null, null, null,
                            "auto-created from role assignment",
                            role.getTenantId(), event.getAssignedBy());
                    Long arId = accessRelationRepository.insertDirect(cmd);
                    log.info("[AccessRelation] auto-created from role assignment: id={} user={} role={} org={}",
                        arId, event.getUserId(), role.getRoleCode(), event.getScopeId());
                });
            } catch (Exception e) {
                log.error("[AccessRelation] failed to auto-create from role assignment user={}: {}",
                    event.getUserId(), e.getMessage(), e);
            }
        }

        // 发送通知给用户
        notificationService.sendInAppMessage(
                event.getUserId(),
                "角色分配通知",
                String.format("您已被分配新角色(ID:%d)，权限已更新", event.getRoleId()),
                NotificationService.MessageType.SYSTEM_NOTICE
        );

        // 记录审计日志
        saveOperationLog("ASSIGN", "USER_ROLE", event.getUserId(),
                String.format("分配角色: userId=%d, roleId=%d, scope=%s:%d",
                        event.getUserId(), event.getRoleId(), event.getScopeType(), event.getScopeId()));

        log.info("用户角色分配处理完成: userId={}, roleId={}", event.getUserId(), event.getRoleId());
    }

    /**
     * 保存操作日志
     */
    private void saveOperationLog(String action, String resourceType, Long resourceId, String description) {
        try {
            activityEventPublisher.newEvent("access", resourceType, action, description)
                .resourceId(resourceId != null ? resourceId.toString() : "")
                .publish();
        } catch (Exception e) {
            log.warn("保存操作日志失败: {}", e.getMessage());
        }
    }
}
