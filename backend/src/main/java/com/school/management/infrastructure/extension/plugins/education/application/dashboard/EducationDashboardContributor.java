package com.school.management.infrastructure.extension.plugins.education.application.dashboard;

import com.school.management.application.dashboard.DashboardScope;
import com.school.management.application.dashboard.DashboardSectionContributor;
import com.school.management.application.organization.MembershipResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 办学规模看板分区 — 教育插件向核心总览看板贡献 "education" 分区
 * (专业数 / 班级数 / 学生数 / 教师数)。
 *
 * <p>原先这 4 个统计硬编码在核心 DashboardOverviewQueryService.getOrgStats
 * (majors/classes 表名经参数拼接绕过了 NoIndustryTableInCoreTest 的 FROM 扫描,
 * 教育语义 feature 键 isLearner/canTeach 也写死核心常量)。2026-06-12 dashboard
 * 去教育侵入: 统计与 feature 键整体移入本贡献点, 核心 organization 分区只留
 * 通用 orgUnitCount。EDU 禁用时本 bean 不存在 → 总览自动无 education 分区。
 *
 * <p>学生/教师数走核心 {@link MembershipResolver} 的通用 feature 计数能力
 * (member 归属 + 用户类型 feature, 不直查 user_student/user_teacher);
 * scope 收敛口径与 {@code TeachingDashboardContributor} 同构。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EducationDashboardContributor implements DashboardSectionContributor {

    /** 学生语义 feature (StudentPlugin.getFeatures). */
    private static final String FEATURE_LEARNER = "isLearner";
    /** 教师语义 feature (TeacherPlugin.getFeatures). */
    private static final String FEATURE_TEACHER = "canTeach";

    private final JdbcTemplate jdbcTemplate;
    private final MembershipResolver membershipResolver;

    @Override
    public String sectionKey() { return "education"; }

    @Override
    public Map<String, Object> contribute(DashboardScope scope) {
        Map<String, Object> stats = new LinkedHashMap<>();
        if (scope.deniesAll()) {
            stats.put("majorCount", 0);
            stats.put("classCount", 0);
            stats.put("studentCount", 0L);
            stats.put("teacherCount", 0L);
            return stats;
        }
        stats.put("majorCount", countByOrgColumn("majors",
                "deleted = 0 AND status = 1", "org_unit_id", scope));
        stats.put("classCount", countByOrgColumn("classes",
                "deleted = 0 AND status = 1", "org_unit_id", scope));
        stats.put("studentCount", countMembersByFeature(scope, FEATURE_LEARNER));
        stats.put("teacherCount", countMembersByFeature(scope, FEATURE_TEACHER));
        return stats;
    }

    /**
     * 对带 org 列的业务表按 scope 收敛计数:
     *   unrestricted → 不加 scope 条件
     *   subtree      → col IN (SELECT id FROM org_units WHERE tenant_id=? AND tree_path LIKE ?)
     *   single       → col = ?
     */
    private int countByOrgColumn(String table, String where, String orgColumn, DashboardScope scope) {
        if (scope.unrestricted()) {
            return countSafe("SELECT COUNT(*) FROM " + table + " WHERE " + where);
        }
        if (scope.subtree()) {
            return countSafe(
                    "SELECT COUNT(*) FROM " + table + " WHERE " + where +
                            " AND " + orgColumn + " IN (" +
                            "SELECT id FROM org_units WHERE tenant_id = ? AND tree_path LIKE ? AND deleted = 0)",
                    scope.tenantId(), scope.orgUnitPath() + "%");
        }
        return countSafe(
                "SELECT COUNT(*) FROM " + table + " WHERE " + where + " AND " + orgColumn + " = ?",
                scope.orgUnitId());
    }

    private long countMembersByFeature(DashboardScope scope, String featureKey) {
        try {
            if (scope.unrestricted()) {
                return membershipResolver.countUsersByFeature(featureKey);
            }
            if (scope.subtree()) {
                return membershipResolver.countMembersByFeatureInSubtree(
                        scope.tenantId(), scope.orgUnitPath(), featureKey);
            }
            return membershipResolver.countMembersByFeature(scope.orgUnitId(), featureKey);
        } catch (Exception e) {
            log.debug("Education dashboard feature count failed (feature={}): {}", featureKey, e.getMessage());
            return 0L;
        }
    }

    private int countSafe(String sql, Object... args) {
        try {
            Long n = jdbcTemplate.queryForObject(sql, Long.class, args);
            return n == null ? 0 : n.intValue();
        } catch (Exception e) {
            log.debug("Education dashboard count failed: {}", e.getMessage());
            return 0;
        }
    }
}
