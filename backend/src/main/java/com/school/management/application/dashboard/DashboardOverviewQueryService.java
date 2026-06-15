package com.school.management.application.dashboard;

import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.valueobject.ScopeSpec;
import com.school.management.infrastructure.access.DataPermissionPolicyService;
import com.school.management.infrastructure.access.UserContext;
import com.school.management.infrastructure.access.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * /dashboard/overview 聚合查询服务。
 *
 * <p>将 {@code DashboardController} 里的 JdbcTemplate 原生 SQL 抽离到应用层，
 * 并按 {@code dashboard} 模块的数据范围 (可组合 {@link ScopeSpec} 轴①) 收敛查询：
 *
 * <ul>
 *   <li>ALL / 超管 — 全校聚合（历史行为）。</li>
 *   <li>DEPARTMENT_AND_BELOW — 以用户 {@code orgUnitPath} 为根的子树。</li>
 *   <li>DEPARTMENT — 仅用户所属 org_unit。</li>
 *   <li>CUSTOM / SELF / 无配置 — 范围返回零值（Casbin 层的 MANAGEMENT 门禁
 *       已经挡住纯学生/老师，这里只是第二道防线）。</li>
 * </ul>
 *
 * 学期/课程/今日登录等平台级全局指标不参与范围收敛。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardOverviewQueryService {

    private static final String MODULE_CODE = "dashboard";

    private final JdbcTemplate jdbcTemplate;
    private final DataPermissionPolicyService policyService;
    /** 行业插件贡献的看板分区 (如教务 teaching / 办学规模 education)。无 bean 时 Spring 注入空 List, 核心不感知行业。 */
    private final java.util.List<DashboardSectionContributor> sectionContributors;

    public Map<String, Object> getOverview() {
        ScopeFilter filter = resolveFilter(UserContextHolder.getContext());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("organization", getOrgStats(filter));
        result.put("inspection", getInspectionStats());
        result.put("system", getSystemStats(filter));
        // 行业插件分区 (教务统计等由 EDU 的 DashboardSectionContributor 贡献; 核心不硬编码行业)
        DashboardScope scope = filter.toScope();
        for (DashboardSectionContributor c : sectionContributors) {
            result.put(c.sectionKey(), c.contribute(scope));
        }
        return result;
    }

    // ================= Scope resolution =================

    private ScopeFilter resolveFilter(UserContext ctx) {
        Long tenantId = ctx == null ? 1L : ctx.getTenantId();
        if (ctx == null || ctx.isSuperAdmin()) {
            return ScopeFilter.unrestricted(tenantId);
        }
        List<Long> roleIds = ctx.getRoleIds();
        if (roleIds == null || roleIds.isEmpty()) {
            return ScopeFilter.empty(tenantId);
        }

        // 多角色取最宽: 逐角色读 dashboard 资源的可组合范围 (轴①), 命中最宽者即停。
        // 优先级 ALL > 子树(PRIMARY_ORG+subtree) > 本组织(PRIMARY_ORG) > 其它(空)。
        boolean anySubtree = false;
        boolean anySingle = false;
        for (Long roleId : roleIds) {
            ScopeSpec spec = policyService.getScopeSpec(tenantId, roleId, MODULE_CODE, "READ");
            if (spec == null) continue;
            OrgAnchor anchor = spec.getOrgAnchor();
            if (anchor == OrgAnchor.ALL) {
                return ScopeFilter.unrestricted(tenantId);
            }
            if (anchor == OrgAnchor.PRIMARY_ORG) {
                if (spec.isIncludeSubtree()) {
                    anySubtree = true;
                } else {
                    anySingle = true;
                }
            }
            // RELATION / CUSTOM_ORG / PLUGIN_DIM / SELF — 仪表盘层面不收敛, 视为空
        }

        if (anySubtree) {
            String path = ctx.getOrgUnitPath();
            if (path == null || path.isBlank()) return ScopeFilter.empty(tenantId);
            return ScopeFilter.subtree(tenantId, path);
        }
        if (anySingle) {
            Long orgUnitId = ctx.getOrgUnitId();
            if (orgUnitId == null) return ScopeFilter.empty(tenantId);
            return ScopeFilter.single(tenantId, orgUnitId);
        }
        return ScopeFilter.empty(tenantId);
    }

    // ================= Organization =================

    private Map<String, Object> getOrgStats(ScopeFilter filter) {
        // 行业规模统计 (如教育的专业/班级/学生/教师数) 由插件分区贡献
        // (EDU 的 EducationDashboardContributor, sectionKey=education);
        // 核心 organization 分区只产通用组织结构统计。
        Map<String, Object> stats = new LinkedHashMap<>();
        if (filter.deniesAll()) {
            stats.put("orgUnitCount", 0);
            return stats;
        }
        stats.put("orgUnitCount", countOrgUnits(filter));
        return stats;
    }

    private int countOrgUnits(ScopeFilter filter) {
        if (filter.unrestricted()) {
            return countSafe("SELECT COUNT(*) FROM org_units WHERE deleted = 0 AND status = 'ACTIVE'");
        }
        if (filter.isSubtree()) {
            return countSafe(
                    "SELECT COUNT(*) FROM org_units WHERE deleted = 0 AND status = 'ACTIVE' " +
                            "AND tenant_id = ? AND tree_path LIKE ?",
                    filter.tenantId, filter.orgUnitPath + "%");
        }
        // DEPARTMENT — 只计自身一个节点
        return countSafe(
                "SELECT COUNT(*) FROM org_units WHERE deleted = 0 AND status = 'ACTIVE' " +
                        "AND tenant_id = ? AND id = ?",
                filter.tenantId, filter.orgUnitId);
    }

    /**
     * 统计 scope 内的活跃用户数. 归属来自 access_relations member 关系 (users.primary_org_unit_id 已删).
     *   unrestricted → 全系统活跃用户数 (不要求归属)
     *   subtree      → 在子树内某 org 有 member 归属的去重用户数
     *   single       → 仅该 org 直接 member 归属的去重用户数
     */
    private int countUsersInScope(ScopeFilter filter) {
        if (filter.unrestricted()) {
            return countSafe("SELECT COUNT(*) FROM users WHERE deleted = 0 AND status = 1");
        }
        String memberJoin =
            "SELECT COUNT(DISTINCT u.id) FROM users u " +
            "JOIN access_relations ar ON ar.subject_type = 'user' AND ar.subject_id = u.id " +
            "  AND ar.relation = 'member' AND ar.resource_type = 'org_unit' AND ar.deleted = 0 " +
            "  AND (ar.valid_to IS NULL OR ar.valid_to > NOW()) " +
            "WHERE u.deleted = 0 AND u.status = 1 ";
        if (filter.isSubtree()) {
            return countSafe(memberJoin +
                    "AND ar.resource_id IN (SELECT id FROM org_units " +
                    "WHERE tenant_id = ? AND tree_path LIKE ? AND deleted = 0)",
                    filter.tenantId, filter.orgUnitPath + "%");
        }
        return countSafe(memberJoin + "AND ar.resource_id = ?", filter.orgUnitId);
    }

    // ================= Industry sections =================
    // 行业统计一律走 DashboardSectionContributor 贡献点:
    //   teaching  — TeachingDashboardContributor (semesters/courses/teaching_tasks)
    //   education — EducationDashboardContributor (专业/班级/学生/教师规模, 含 feature 计数)
    // 核心不持有任何行业表名/行业 feature 键。

    // ================= Inspection =================

    /**
     * 检查平台统计不参与 dashboard 模块的组织范围收敛——检查模块有自己的 {@code DataPermission}
     * 配置与 {@code access_relations} 绑定。这里展示的是"当前可见检查态势"。
     * 非授权用户无法抵达 {@code /dashboard/overview}（被 @CasbinAccess 拦截）。
     */
    private Map<String, Object> getInspectionStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activeProjectCount", countSafe(
                "SELECT COUNT(*) FROM insp_projects WHERE deleted = 0 AND status = 'PUBLISHED'"));
        stats.put("pendingTaskCount", countSafe(
                "SELECT COUNT(*) FROM insp_tasks WHERE deleted = 0 " +
                        "AND status IN ('PENDING','CLAIMED','IN_PROGRESS')"));
        stats.put("correctiveOpenCount", countSafe(
                "SELECT COUNT(*) FROM insp_corrective_cases WHERE deleted = 0 " +
                        "AND status IN ('OPEN','ASSIGNED','IN_PROGRESS','SUBMITTED')"));
        return stats;
    }

    // ================= System =================

    private Map<String, Object> getSystemStats(ScopeFilter filter) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", countUsersInScope(filter));
        // 全局登录次数对平台管理员有意义，子部门管理员看到同一数字
        stats.put("todayLoginCount", countSafe(
                "SELECT COUNT(*) FROM users WHERE deleted = 0 AND DATE(last_login_time) = CURDATE()"));
        return stats;
    }

    // ================= Low-level helpers =================

    private int countSafe(String sql, Object... args) {
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.debug("Dashboard count query failed ({}): {}", sql, e.getMessage());
            return 0;
        }
    }

    // ================= Scope filter value object =================

    private enum ScopeKind { UNRESTRICTED, SUBTREE, SINGLE, DENY }

    /**
     * 内部范围描述器，用于在 SQL 生成时携带组织过滤条件。
     * 不对外暴露范围枚举概念——Controller 侧无需关心 SQL 细节。
     */
    private static final class ScopeFilter {
        final ScopeKind kind;
        final Long tenantId;
        final String orgUnitPath;
        final Long orgUnitId;

        private ScopeFilter(ScopeKind kind, Long tenantId, String orgUnitPath, Long orgUnitId) {
            this.kind = kind;
            this.tenantId = tenantId;
            this.orgUnitPath = orgUnitPath;
            this.orgUnitId = orgUnitId;
        }

        /** 转为传给插件贡献点的公共范围 DTO。 */
        DashboardScope toScope() {
            return new DashboardScope(kind.name(), tenantId, orgUnitPath, orgUnitId);
        }

        static ScopeFilter unrestricted(Long tenantId) {
            return new ScopeFilter(ScopeKind.UNRESTRICTED, tenantId, null, null);
        }

        static ScopeFilter subtree(Long tenantId, String orgUnitPath) {
            String normalized = orgUnitPath.endsWith("/") ? orgUnitPath : orgUnitPath + "/";
            return new ScopeFilter(ScopeKind.SUBTREE, tenantId, normalized, null);
        }

        static ScopeFilter single(Long tenantId, Long orgUnitId) {
            return new ScopeFilter(ScopeKind.SINGLE, tenantId, null, orgUnitId);
        }

        static ScopeFilter empty(Long tenantId) {
            return new ScopeFilter(ScopeKind.DENY, tenantId, null, null);
        }

        boolean unrestricted() { return kind == ScopeKind.UNRESTRICTED; }
        boolean isSubtree() { return kind == ScopeKind.SUBTREE; }
        boolean deniesAll() { return kind == ScopeKind.DENY; }
    }
}
