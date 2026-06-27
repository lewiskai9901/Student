package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.RoleAssignmentScope;
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
 * 通用 JdbcTemplate 旁路数据权限收窄 helper (S3, 2026-05-20 安全审计 B1).
 *
 * <p>{@link DataPermissionInterceptor} 只在 MyBatis 拦截器层工作, 不覆盖 JdbcTemplate
 * 直查. 任何走 raw JdbcTemplate 读用户范围数据 (学生/成绩/考勤等) 的代码都绕过了
 * 数据权限 — 跨组织全可见. 本 helper 把 UserContext + ScopedRoles 折叠成允许的
 * org_unit_id 集合, 调用方拼成 {@code AND org_unit_id IN (...)} 即可.
 *
 * <p>这是 {@code infrastructure.inspection.InspectionScopeHelper} 的通用化版本 —
 * 逻辑本就与行业无关, 提到 core 层供任何模块 (教育插件等) 复用. inspection 的
 * 旧 helper 暂保留, 后续可迁来委托本类.
 *
 * <p>返回值约定:
 * <ul>
 *   <li>{@code null}  — 不受限 (super admin / data permission 关闭 / ScopedRole ALL)</li>
 *   <li>空 Set        — 拒绝所有 (调用方拼 {@code AND 1=0})</li>
 *   <li>非空 Set      — 允许的 org_unit_id 集合</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrgScopeHelper {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 当前用户允许访问的 org_unit_id 集合.
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
            if (RoleAssignmentScope.ALL.equals(sr.getScopeType())) {
                return null; // 任一角色 ALL scope → 整体不受限
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
     * <p>调用方直接拼到 WHERE 末尾. csv 内是 long, 无注入面.
     *
     * @param column org_unit_id 字段名 (可带表别名: "g.org_unit_id")
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

    /** 当前用户是否完全不受限 (super admin / data permission 关闭 / ALL scope 角色). */
    public boolean isUnbounded() {
        return allowedOrgIds() == null;
    }

    /**
     * 校验某个 org_unit_id 是否在当前用户范围内.
     * 用于写操作 (update/delete by id) 前的归属校验.
     */
    public boolean isOrgAllowed(Long orgUnitId) {
        Set<Long> ids = allowedOrgIds();
        if (ids == null) {
            return true; // 不受限
        }
        return orgUnitId != null && ids.contains(orgUnitId);
    }

    private Set<Long> querySubtreeIds(String orgPath) {
        Long tenantId = TenantContextHolder.getTenantId();
        try {
            List<Long> ids = jdbcTemplate.queryForList(
                "SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ? AND deleted = 0",
                Long.class, tenantId, orgPath + "%");
            return new HashSet<>(ids);
        } catch (Exception e) {
            log.warn("[OrgScopeHelper] subtree query failed for path '{}': {}", orgPath, e.getMessage());
            return Collections.emptySet();
        }
    }
}
