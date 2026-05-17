package com.school.management.infrastructure.inspection;

import com.school.management.domain.access.model.ScopeType;
import com.school.management.infrastructure.access.UserContext;
import com.school.management.infrastructure.access.UserContextHolder;
import com.school.management.infrastructure.tenant.TenantContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 检查平台 JdbcTemplate 旁路的数据权限收窄.
 *
 * <p>I1 (2026-05-17): {@link com.school.management.infrastructure.access.DataPermissionInterceptor}
 * 只在 MyBatis 拦截器层工作, 不覆盖 JdbcTemplate 直查. inspection 模块 governance KPI / 导出 /
 * 审计查询等场景大量直走 JdbcTemplate, 这些路径之前 0 过滤, 跨组织全可见.
 *
 * <p>本 helper 把 UserContext + ScopedRoles 折叠成 org_unit_id 集合,
 * 调用方拼成 {@code AND org_unit_id IN (...)} 即可.
 *
 * <p>返回值约定:
 * <ul>
 *   <li>{@code null}    — 不受限 (super admin / data permission 关闭 / ScopedRole ALL)</li>
 *   <li>空 Set         — 拒绝所有 (调用方 short-circuit 或拼 {@code AND 1=0})</li>
 *   <li>非空 Set       — 允许的 org_unit_id 集合 (调用方拼 IN 列表)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InspectionScopeHelper {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 返回当前用户在 inspection 模块的允许 org_unit_id 集合.
     * null = 不受限; 空 = 拒绝; 非空 = 收窄到这些 org.
     */
    public Set<Long> allowedOrgIds() {
        if (!UserContextHolder.isDataPermissionEnabled()) {
            return null;
        }
        UserContext ctx = UserContextHolder.getContext();
        if (ctx == null || ctx.isSuperAdmin()) {
            return null;
        }

        List<UserContext.ScopedRoleInfo> roles = ctx.getScopedRoles();
        if (roles == null || roles.isEmpty()) {
            // 老路径: 用 user 主归属 org 子树作 fallback
            String path = ctx.getOrgUnitPath();
            if (path == null || path.isBlank()) {
                return Collections.emptySet(); // 既无 scoped role 又无 primary org → deny all
            }
            return querySubtreeIds(path);
        }

        Set<Long> union = new HashSet<>();
        for (UserContext.ScopedRoleInfo sr : roles) {
            if (ScopeType.ALL.equals(sr.getScopeType())) {
                // 任一角色 ALL scope → 整体不受限
                return null;
            }
            if (sr.getScopeOrgPath() != null && !sr.getScopeOrgPath().isBlank()) {
                union.addAll(querySubtreeIds(sr.getScopeOrgPath()));
            } else if (sr.getScopeId() != null) {
                union.add(sr.getScopeId());
            }
        }
        return union;
    }

    /**
     * 构造 SQL 片段: 不受限返回 ""; 拒绝返回 " AND 1=0"; 非空返回 " AND {column} IN (csv)".
     *
     * <p>调用方把它直接拼到 WHERE 末尾即可. csv 内是 long, 无注入面.
     *
     * @param column 表里 org_unit_id 字段名 (或带表别名: "t.org_unit_id")
     */
    public String orgScopeClause(String column) {
        Set<Long> ids = allowedOrgIds();
        if (ids == null) {
            return "";
        }
        if (ids.isEmpty()) {
            return " AND 1=0";
        }
        String csv = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return " AND " + column + " IN (" + csv + ")";
    }

    /**
     * 当前用户是否完全不受限 (super admin / data permission 关闭 / ALL scope 角色).
     * 调用方可在不受限时跳过整段 scope SQL 拼装.
     */
    public boolean isUnbounded() {
        return allowedOrgIds() == null;
    }

    private Set<Long> querySubtreeIds(String orgPath) {
        Long tenantId = TenantContextHolder.getTenantId();
        try {
            List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ? AND deleted = 0",
                Long.class, tenantId, orgPath + "%");
            return new HashSet<>(ids);
        } catch (Exception e) {
            log.warn("[InspectionScopeHelper] subtree query failed for path '{}': {}", orgPath, e.getMessage());
            return Collections.emptySet();
        }
    }
}
