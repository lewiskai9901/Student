package com.school.management.infrastructure.extension.plugins.education.application.organization;

import com.school.management.application.organization.MembershipResolver;
import com.school.management.application.organization.OrgImpactContributor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 教育插件向 org-impact 贡献"学生数 / 教师数"。
 *
 * <p>原先这两项硬编码在核心 {@code OrgUnitJdbcApplicationService}, 直接写死行业 feature
 * {@code isLearner}/{@code canTeach} —— 违反"核心不依赖行业特性 (特性依赖只能向下)"。2026-06-28 迁出到本贡献点,
 * 与 {@code ClassImpactContributor} 同模式。核心只算通用 {@code memberCount}; EDU 禁用时本 bean 不存在 →
 * impact 自动无 studentCount/teacherCount。
 *
 * <p>{@link MembershipResolver#countMembersByFeatureInSubtree} 是核心通用基础设施 (按传入 feature 计数),
 * 这里由教育插件传入 edu 自己的 feature —— 方向正确 (插件用核心能力)。
 */
@Component
@RequiredArgsConstructor
public class StudentTeacherImpactContributor implements OrgImpactContributor {

    private final MembershipResolver membershipResolver;

    @Override
    public Map<String, Object> contribute(String treePath) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("studentCount", membershipResolver.countMembersByFeatureInSubtree(null, treePath, "isLearner"));
        m.put("teacherCount", membershipResolver.countMembersByFeatureInSubtree(null, treePath, "canTeach"));
        return m;
    }
}
