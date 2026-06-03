package com.school.management.application.dashboard;

import com.school.management.application.organization.MembershipResolver;
import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.valueobject.MergedDataScope;
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
 * 并按 {@code dashboard} 模块的数据范围 ({@link DataScope}) 收敛查询：
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

    /** 学生语义 feature (StudentPlugin.getFeatures) — 不写死类型码 STUDENT. */
    private static final String FEATURE_LEARNER = "isLearner";
    /** 教师语义 feature (TeacherPlugin.getFeatures) — 不写死类型码 TEACHER. */
    private static final String FEATURE_TEACHER = "canTeach";

    private final JdbcTemplate jdbcTemplate;
    private final DataPermissionPolicyService policyService;
    private final MembershipResolver membershipResolver;
    /** 行业插件贡献的看板分区 (如教务 teaching)。无 bean 时 Spring 注入空 List, 核心不感知行业。 */
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
        MergedDataScope merged = policyService.getMergedScope(tenantId, roleIds, MODULE_CODE);
        DataScope scope = merged.getEffectiveScope();

        switch (scope) {
            case ALL:
                return ScopeFilter.unrestricted(tenantId);
            case DEPARTMENT_AND_BELOW: {
                String path = ctx.getOrgUnitPath();
                if (path == null || path.isBlank()) return ScopeFilter.empty(tenantId);
                return ScopeFilter.subtree(tenantId, path);
            }
            case DEPARTMENT: {
                Long orgUnitId = ctx.getOrgUnitId();
                if (orgUnitId == null) return ScopeFilter.empty(tenantId);
                return ScopeFilter.single(tenantId, orgUnitId);
            }
            default:
                // CUSTOM / SELF — 仪表盘层面无意义，返回空
                return ScopeFilter.empty(tenantId);
        }
    }

    // ================= Organization =================

    private Map<String, Object> getOrgStats(ScopeFilter filter) {
        Map<String, Object> stats = new LinkedHashMap<>();
        if (filter.deniesAll()) {
            stats.put("orgUnitCount", 0);
            stats.put("majorCount", 0);
            stats.put("classCount", 0);
            stats.put("studentCount", 0);
            stats.put("teacherCount", 0);
            return stats;
        }
        stats.put("orgUnitCount", countOrgUnits(filter));
        stats.put("majorCount", countByOrgColumn("majors",
                "deleted = 0 AND status = 1", "org_unit_id", filter));
        stats.put("classCount", countByOrgColumn("classes",
                "deleted = 0 AND status = 1", "org_unit_id", filter));
        // 学生/教师数走 member 归属 + 用户类型 feature, 不直查行业扩展表 user_student/user_teacher.
        stats.put("studentCount", countMembersByFeature(filter, FEATURE_LEARNER));
        stats.put("teacherCount", countMembersByFeature(filter, FEATURE_TEACHER));
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
     * 对带 org 列的业务表：
     *   unrestricted → 不加 scope 条件
     *   subtree      → col IN (SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ?)
     *   single       → col = ?
     */
    private int countByOrgColumn(String table, String where, String orgColumn, ScopeFilter filter) {
        if (filter.unrestricted()) {
            return countSafe("SELECT COUNT(*) FROM " + table + " WHERE " + where);
        }
        if (filter.isSubtree()) {
            String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + where +
                    " AND " + orgColumn + " IN (" +
                    "SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ? AND deleted = 0)";
            return countSafe(sql, filter.tenantId, filter.orgUnitPath + "%");
        }
        return countSafe(
                "SELECT COUNT(*) FROM " + table + " WHERE " + where + " AND " + orgColumn + " = ?",
                filter.orgUnitId);
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

    /**
     * 按 feature 统计人数, 按当前 scope 收敛.
     *   unrestricted → 全系统该类型(feature)用户数 (类型口径, 不要求 org 归属)
     *   subtree      → 子树范围 member 归属 (tree_path 前缀)
     *   single       → 仅该 org 直接 member 归属
     */
    private long countMembersByFeature(ScopeFilter filter, String featureKey) {
        try {
            if (filter.unrestricted()) {
                return membershipResolver.countUsersByFeature(featureKey);
            }
            if (filter.isSubtree()) {
                return membershipResolver.countMembersByFeatureInSubtree(
                        filter.tenantId, filter.orgUnitPath, featureKey);
            }
            return membershipResolver.countMembersByFeature(filter.orgUnitId, featureKey);
        } catch (Exception e) {
            log.debug("Dashboard feature count failed (feature={}): {}", featureKey, e.getMessage());
            return 0L;
        }
    }

    // ================= Teaching =================
    // 教务统计 (semesters/courses/teaching_tasks) 已迁至教育插件
    // plugins/education 的 TeachingDashboardContributor (DashboardSectionContributor 实现)。
    // 核心不再硬编码行业统计。

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

    private long longSafe(String sql, Object... args) {
        try {
            Long val = jdbcTemplate.queryForObject(sql, Long.class, args);
            return val != null ? val : 0L;
        } catch (Exception e) {
            log.debug("Dashboard long query failed: {}", e.getMessage());
            return 0L;
        }
    }

    private String stringSafe(String sql, String defaultValue, Object... args) {
        try {
            String val = jdbcTemplate.queryForObject(sql, String.class, args);
            return val != null ? val : defaultValue;
        } catch (Exception e) {
            log.debug("Dashboard string query failed: {}", e.getMessage());
            return defaultValue;
        }
    }

    // ================= Scope filter value object =================

    private enum ScopeKind { UNRESTRICTED, SUBTREE, SINGLE, DENY }

    /**
     * 内部范围描述器，用于在 SQL 生成时携带组织过滤条件。
     * 不对外暴露 DataScope 概念——Controller 侧无需关心 SQL 细节。
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
