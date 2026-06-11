package com.school.management.infrastructure.extension;

import com.school.management.infrastructure.extension.event.PermissionsRefreshedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 角色 × 功能权限默认绑定 Registrar — {@link RoleScopeBindingRegistrar} (P5-1, 数据权限)
 * 的功能权限对偶, 落 {@code role_permissions} 表。
 *
 * <h3>为什么是独立 @Order(600) Runner 而不挂 ContributionDispatcher</h3>
 * dispatcher 是 @Order(60), 跑在 PermissionRegistrar / RolePresetRegistrar (100~500)
 * 之前 — 全新库上彼时 roles / permissions 两张表还是空的, role_id / permission_id
 * 解析必然失败, 绑定会全部 skip (P5-1 的 RoleScopeBindingRegistrar 挂在 dispatcher 上
 * 就有此脆弱性, 靠库里已有数据掩盖)。本 Registrar 排在 600, 角色与权限注册完成后执行。
 *
 * <h3>UPSERT 策略 — INSERT IGNORE (与 P5-1 一致)</h3>
 * <ul>
 *   <li>已有 (role_id, permission_id) 行 → 跳过, 保留 admin 在 UI 上的手动调整</li>
 *   <li>没有该行 → 插入。这是"声明式默认 + 命令式覆盖"</li>
 * </ul>
 *
 * <h3>Casbin 重载</h3>
 * 批量结束后若有新增, 发一次 {@link PermissionsRefreshedEvent} →
 * {@code CasbinPolicyService.syncFromDatabase()} 把新 grants 装进内存 enforcer。
 * 稳态重启 (全部 SKIPPED_EXISTING) 不发 — RolePresetRegistrar 的事件已触发过同步,
 * 彼时 role_permissions 里的既有行已被装载。
 */
@Slf4j
@Component
@Order(600)
@RequiredArgsConstructor
public class RolePermissionBindingRegistrar implements ApplicationRunner {

    private final List<PluginPackage> packages;
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher eventPublisher;

    public enum Result { CREATED, SKIPPED_EXISTING, SKIPPED_NO_ROLE, SKIPPED_NO_PERMISSION }

    @Override
    public void run(ApplicationArguments args) {
        if (packages == null || packages.isEmpty()) {
            return;
        }
        AtomicInteger created = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        for (PluginPackage pkg : packages) {
            pkg.contribute()
               .filter(c -> c instanceof Contribution.RolePermissionBindingContribution)
               .map(c -> (Contribution.RolePermissionBindingContribution) c)
               .forEach(b -> {
                   try {
                       Result r = upsert(b.roleCode(), b.permissionCode(), 1L);
                       if (r == Result.CREATED) created.incrementAndGet();
                       else skipped.incrementAndGet();
                   } catch (Exception e) {
                       failed.incrementAndGet();
                       log.error("[RolePermissionBinding] 绑定失败 {}: {}", b.uniqueKey(), e.getMessage());
                   }
               });
        }

        if (created.get() > 0 || skipped.get() > 0 || failed.get() > 0) {
            log.info("[RolePermissionBinding] 默认功能权限绑定完成: created={}, skipped={}, failed={}",
                created.get(), skipped.get(), failed.get());
        }
        if (created.get() > 0) {
            eventPublisher.publishEvent(
                new PermissionsRefreshedEvent(this, "RolePermissionBindingRegistrar"));
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public Result upsert(String roleCode, String permissionCode, Long tenantId) {
        if (roleCode == null || permissionCode == null) {
            return Result.SKIPPED_NO_ROLE;
        }
        Long actualTenantId = tenantId != null ? tenantId : 1L;

        Long roleId;
        try {
            roleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE role_code = ? AND tenant_id = ? AND deleted = 0 LIMIT 1",
                Long.class, roleCode, actualTenantId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            log.debug("[RolePermissionBinding] role_code '{}' not found in tenant {}, skip",
                roleCode, actualTenantId);
            return Result.SKIPPED_NO_ROLE;
        }
        if (roleId == null) return Result.SKIPPED_NO_ROLE;

        Long permissionId;
        try {
            permissionId = jdbc.queryForObject(
                "SELECT id FROM permissions WHERE permission_code = ? AND deleted = 0 LIMIT 1",
                Long.class, permissionCode);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            log.warn("[RolePermissionBinding] permission_code '{}' 未注册, skip ({} → {})",
                permissionCode, roleCode, permissionCode);
            return Result.SKIPPED_NO_PERMISSION;
        }
        if (permissionId == null) return Result.SKIPPED_NO_PERMISSION;

        // 依赖 uk_role_permission 唯一键 (role_id, permission_id)
        int affected = jdbc.update(
            "INSERT IGNORE INTO role_permissions (role_id, permission_id, tenant_id) VALUES (?, ?, ?)",
            roleId, permissionId, actualTenantId);

        if (affected > 0) {
            log.info("[RolePermissionBinding] CREATED: {} ← {} (role_id={}, perm_id={})",
                roleCode, permissionCode, roleId, permissionId);
            return Result.CREATED;
        }
        log.debug("[RolePermissionBinding] SKIPPED (existing): {} ← {} (admin 配置保留)",
            roleCode, permissionCode);
        return Result.SKIPPED_EXISTING;
    }
}
