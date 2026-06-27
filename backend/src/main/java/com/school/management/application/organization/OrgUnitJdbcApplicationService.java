package com.school.management.application.organization;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Application service holding org-unit jdbc query logic that does not belong to
 * the OrgUnit domain aggregate (impact analysis / cross-aggregate counts).
 *
 * Extracted from OrgUnitController to remove direct JdbcTemplate usage from the
 * REST layer, consistent with the asset/system module refactor pattern.
 * Named OrgUnitJdbcApplicationService to avoid colliding with the existing
 * OrgUnitApplicationService that owns OrgUnit domain logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgUnitJdbcApplicationService {

    private final JdbcTemplate jdbcTemplate;
    private final MembershipResolver membershipResolver;
    /** 行业插件贡献的影响项 (如教育班级数)。无 bean 时 Spring 注入空 List, 核心不感知行业。 */
    private final java.util.List<OrgImpactContributor> impactContributors;

    /** Marker thrown when the requested org unit does not exist. */
    public static class OrgUnitNotFoundException extends RuntimeException {
        public OrgUnitNotFoundException(Long id) {
            super("OrgUnit not found: " + id);
        }
    }

    /**
     * Org unit impact analysis — counts dependent entities under this org_unit
     * subtree (descendants, students, classes, places, teachers, access_relations).
     *
     * @throws OrgUnitNotFoundException if the org unit does not exist.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getImpact(Long id) {
        Map<String, Object> impact = new LinkedHashMap<>();
        impact.put("orgUnitId", id);

        // 1. 组织树后代数 (走 tree_path 子树)
        String treePath;
        try {
            treePath = jdbcTemplate.queryForObject(
                "SELECT tree_path FROM org_units WHERE id = ? AND deleted = 0",
                String.class, id);
        } catch (Exception e) {
            throw new OrgUnitNotFoundException(id);
        }
        if (treePath == null) treePath = "";

        Integer descendants = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM org_units WHERE deleted = 0 AND tree_path LIKE ? AND id != ?",
            Integer.class, treePath + "%", id);
        impact.put("descendantOrgCount", descendants != null ? descendants : 0);

        // 2. 子树下的全部成员数 (通用 — 不限 feature)。学生/教师等行业拆分由 OrgImpactContributor
        //    (教育插件 StudentTeacherImpactContributor) 贡献, 核心不碰 isLearner/canTeach 等行业 feature。
        long memberCount = membershipResolver.countMembersInSubtree(null, treePath);
        impact.put("memberCount", memberCount);

        // 3. 子树下的 classes(教育)/ students / teachers — 均由教育插件 OrgImpactContributor 贡献,
        //    见下方 impactContributors 循环。班级本身是 org_unit, 已计入 descendantOrgCount。

        // 4. 子树下的 places
        Integer placeCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM places WHERE deleted = 0 AND effective_org_unit_id IN " +
            "(SELECT id FROM org_units WHERE deleted = 0 AND tree_path LIKE ?)",
            Integer.class, treePath + "%");
        impact.put("placeCount", placeCount != null ? placeCount : 0);

        // 5. (teachers 计数已移至教育插件 StudentTeacherImpactContributor)

        // 6. access_relations 引用本子树 org_unit 的关系数
        Integer relationCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM access_relations WHERE deleted = 0 " +
            "AND resource_type = 'org_unit' AND resource_id IN " +
            "(SELECT id FROM org_units WHERE deleted = 0 AND tree_path LIKE ?)",
            Integer.class, treePath + "%");
        impact.put("accessRelationCount", relationCount != null ? relationCount : 0);

        // 7. 行业插件贡献的影响项 (如教育班级数 classCount; 仅展示, 不计入 total — 班级已计入 descendants)
        for (OrgImpactContributor c : impactContributors) {
            impact.putAll(c.contribute(treePath));
        }

        // 8. 警告级别 (用于 UI 是否显示二次确认)
        long total = (descendants != null ? descendants : 0)
                  + memberCount;
        String severity = total == 0 ? "NONE"
                        : total < 10 ? "LOW"
                        : total < 100 ? "MEDIUM"
                        : "HIGH";
        impact.put("severity", severity);
        impact.put("totalAffected", total);

        return impact;
    }
}
