package com.school.management.infrastructure.extension.plugins.education.application.teaching;

import com.school.management.application.dashboard.DashboardScope;
import com.school.management.application.dashboard.DashboardSectionContributor;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 教务统计看板分区 — 教育插件向核心总览看板贡献 "teaching" 分区。
 *
 * <p>原先 (semesters/courses/teaching_tasks 统计) 硬编码在核心
 * DashboardOverviewQueryService.getTeachingStats; 2026-06-02 改为本贡献点 (DashboardSectionContributor),
 * 核心不再感知教务概念。EDU 插件禁用时本 bean 不存在 → 总览自动无 teaching 分区。
 */
@Component
@RequiredArgsConstructor
public class TeachingDashboardContributor implements DashboardSectionContributor {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public String sectionKey() { return "teaching"; }

    @Override
    public Map<String, Object> contribute(DashboardScope scope) {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("currentSemester", stringSafe(
                "SELECT semester_code FROM semesters WHERE is_current = 1 LIMIT 1", "--"));

        long semesterId = longSafe("SELECT id FROM semesters WHERE is_current = 1 LIMIT 1");

        stats.put("courseCount", countSafe(
                "SELECT COUNT(*) FROM courses WHERE deleted = 0 AND status = 1"));

        if (semesterId <= 0 || scope.deniesAll()) {
            stats.put("taskCount", 0);
            stats.put("scheduledRate", 0);
            stats.put("unscheduledCount", 0);
            return stats;
        }

        int taskCount = countTeachingTasks(scope, semesterId, "deleted = 0 AND semester_id = ?");
        int totalTasks = countTeachingTasks(scope, semesterId,
                "deleted = 0 AND task_status = 1 AND semester_id = ?");
        int scheduledTasks = countTeachingTasks(scope, semesterId,
                "deleted = 0 AND task_status = 1 AND scheduling_status = 2 AND semester_id = ?");

        stats.put("taskCount", taskCount);
        stats.put("scheduledRate", totalTasks > 0 ? (int) Math.round(scheduledTasks * 100.0 / totalTasks) : 0);
        stats.put("unscheduledCount", totalTasks - scheduledTasks);
        return stats;
    }

    private int countTeachingTasks(DashboardScope scope, long semesterId, String predicate) {
        if (scope.unrestricted()) {
            return countSafe("SELECT COUNT(*) FROM teaching_tasks WHERE " + predicate, semesterId);
        }
        if (scope.subtree()) {
            return countSafe(
                    "SELECT COUNT(*) FROM teaching_tasks WHERE " + predicate +
                            " AND org_unit_id IN (SELECT id FROM org_units " +
                            "WHERE tenant_id = ? AND tree_path LIKE ? AND deleted = 0)",
                    semesterId, scope.tenantId(), scope.orgUnitPath() + "%");
        }
        return countSafe(
                "SELECT COUNT(*) FROM teaching_tasks WHERE " + predicate + " AND org_unit_id = ?",
                semesterId, scope.orgUnitId());
    }

    // ── jdbc helpers (容错: 表/列缺失或异常时回退默认值, 不打断看板) ──

    private int countSafe(String sql, Object... args) {
        try {
            Long n = jdbcTemplate.queryForObject(sql, Long.class, args);
            return n == null ? 0 : n.intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private long longSafe(String sql, Object... args) {
        try {
            Long n = jdbcTemplate.queryForObject(sql, Long.class, args);
            return n == null ? 0L : n;
        } catch (Exception e) {
            return 0L;
        }
    }

    private String stringSafe(String sql, String defaultValue, Object... args) {
        try {
            String s = jdbcTemplate.queryForObject(sql, String.class, args);
            return s == null ? defaultValue : s;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
