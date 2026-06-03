package com.school.management.infrastructure.extension.plugins.education.application.organization;

import com.school.management.application.organization.OrgImpactContributor;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 教育插件向 org-impact 贡献"班级数" — 原先硬编码在核心
 * OrgUnitJdbcApplicationService.getImpact (COUNT FROM classes); 2026-06-02 改为本贡献点。
 * EDU 禁用时本 bean 不存在 → impact 自动无 classCount。
 */
@Component
@RequiredArgsConstructor
public class ClassImpactContributor implements OrgImpactContributor {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, Object> contribute(String treePath) {
        Integer classCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM classes WHERE deleted = 0 AND org_unit_id IN " +
            "(SELECT id FROM org_units WHERE deleted = 0 AND tree_path LIKE ?)",
            Integer.class, treePath + "%");
        return Map.of("classCount", classCount != null ? classCount : 0);
    }
}
