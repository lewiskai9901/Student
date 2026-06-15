package com.school.management.infrastructure.extension;

import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.ScopePreset;
import com.school.management.domain.access.model.valueobject.ScopeSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 角色 × 资源 × scope 默认绑定 UPSERT (Phase 5 P5-1).
 *
 * <p>{@link Contribution.RoleScopeBindingContribution} 由
 * {@link ContributionDispatcher} 分发到这里执行 UPSERT.
 *
 * <p>UPSERT 策略 — INSERT IGNORE:
 * <ul>
 *   <li>已有 (role_id, resource_code, tenant_id) 行 → 跳过, 保留 admin 手动调整</li>
 *   <li>没有该行 → 插入 (来源 plugin)</li>
 * </ul>
 *
 * <p>这是"声明式默认 + 命令式覆盖"模式: 插件提供 building blocks 的默认 wiring,
 * admin 在 UI 上可改, 重启不被覆盖.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleScopeBindingRegistrar {

    private final JdbcTemplate jdbc;

    public enum Result { CREATED, SKIPPED_EXISTING, SKIPPED_NO_ROLE, SKIPPED_NO_RESOURCE }

    @Transactional
    public Result upsert(String roleCode, String resourceCode, String scopeType, Long tenantId) {
        if (roleCode == null || resourceCode == null || scopeType == null) {
            return Result.SKIPPED_NO_ROLE;
        }
        Long actualTenantId = tenantId != null ? tenantId : 1L;

        // 1. 查 role_id (按 role_code + tenant)
        Long roleId;
        try {
            roleId = jdbc.queryForObject(
                "SELECT id FROM roles WHERE role_code = ? AND tenant_id = ? AND deleted = 0 LIMIT 1",
                Long.class, roleCode, actualTenantId);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            log.debug("[RoleScopeBinding] role_code '{}' not found in tenant {}, skip",
                roleCode, actualTenantId);
            return Result.SKIPPED_NO_ROLE;
        }
        if (roleId == null) return Result.SKIPPED_NO_ROLE;

        // 2. 校验 resource_code 在 data_resources 注册
        Integer rcCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM data_resources WHERE resource_code = ?",
            Integer.class, resourceCode);
        if (rcCount == null || rcCount == 0) {
            log.warn("[RoleScopeBinding] resource_code '{}' not in data_resources, skip ({} -> {})",
                resourceCode, roleCode, scopeType);
            return Result.SKIPPED_NO_RESOURCE;
        }

        // 3. 把命名范围码 (scopeType) 翻译为可组合轴① (T9: scope_type 列已删)。
        //    预设命中 → 锚点/参数/子树; 非预设 (BY_CLASS/BY_MAJOR…) → PLUGIN_DIM + anchorParam=scopeType。
        String orgAnchor;
        String anchorParam;
        int includeSubtree;
        ScopePreset preset = ScopePreset.fromLegacyScopeType(scopeType);
        if (preset != null) {
            ScopeSpec spec = preset.toSpec();
            orgAnchor = spec.getOrgAnchor().name();
            anchorParam = spec.getAnchorParam();
            includeSubtree = spec.isIncludeSubtree() ? 1 : 0;
        } else {
            orgAnchor = OrgAnchor.PLUGIN_DIM.name();
            anchorParam = scopeType;
            includeSubtree = 0;
        }

        // INSERT IGNORE (依赖 uk_role_res 唯一键: role_id + resource_code + apply_to + tenant_id)
        int affected = jdbc.update(
            "INSERT IGNORE INTO role_data_scopes (role_id, resource_code, apply_to, " +
            "org_anchor, anchor_param, include_subtree, priority, tenant_id, deleted) " +
            "VALUES (?, ?, 'BOTH', ?, ?, ?, ?, ?, 0)",
            roleId, resourceCode, orgAnchor, anchorParam, includeSubtree, 0, actualTenantId);

        if (affected > 0) {
            log.info("[RoleScopeBinding] CREATED: {} × {} = {} (role_id={}, tenant={})",
                roleCode, resourceCode, scopeType, roleId, actualTenantId);
            return Result.CREATED;
        }
        log.debug("[RoleScopeBinding] SKIPPED (existing): {} × {} (admin manual config preserved)",
            roleCode, resourceCode);
        return Result.SKIPPED_EXISTING;
    }
}
