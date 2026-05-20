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

        // 2. 子树下的 students (含本节点 + 后代)
        Integer studentCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM user_student WHERE deleted = 0 AND org_unit_id IN " +
            "(SELECT id FROM org_units WHERE deleted = 0 AND tree_path LIKE ?)",
            Integer.class, treePath + "%");
        impact.put("studentCount", studentCount != null ? studentCount : 0);

        // 3. 子树下的 classes
        Integer classCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM classes WHERE deleted = 0 AND org_unit_id IN " +
            "(SELECT id FROM org_units WHERE deleted = 0 AND tree_path LIKE ?)",
            Integer.class, treePath + "%");
        impact.put("classCount", classCount != null ? classCount : 0);

        // 4. 子树下的 places
        Integer placeCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM places WHERE deleted = 0 AND org_unit_id IN " +
            "(SELECT id FROM org_units WHERE deleted = 0 AND tree_path LIKE ?)",
            Integer.class, treePath + "%");
        impact.put("placeCount", placeCount != null ? placeCount : 0);

        // 5. 子树下的 teachers (primary_org_unit_id 在子树)
        Integer teacherCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM users WHERE deleted = 0 AND user_type_code = 'TEACHER' " +
            "AND primary_org_unit_id IN " +
            "(SELECT id FROM org_units WHERE deleted = 0 AND tree_path LIKE ?)",
            Integer.class, treePath + "%");
        impact.put("teacherCount", teacherCount != null ? teacherCount : 0);

        // 6. access_relations 引用本子树 org_unit 的关系数
        Integer relationCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM access_relations WHERE deleted = 0 " +
            "AND resource_type = 'org_unit' AND resource_id IN " +
            "(SELECT id FROM org_units WHERE deleted = 0 AND tree_path LIKE ?)",
            Integer.class, treePath + "%");
        impact.put("accessRelationCount", relationCount != null ? relationCount : 0);

        // 7. 警告级别 (用于 UI 是否显示二次确认)
        int total = (descendants != null ? descendants : 0)
                  + (studentCount != null ? studentCount : 0)
                  + (classCount != null ? classCount : 0)
                  + (teacherCount != null ? teacherCount : 0);
        String severity = total == 0 ? "NONE"
                        : total < 10 ? "LOW"
                        : total < 100 ? "MEDIUM"
                        : "HIGH";
        impact.put("severity", severity);
        impact.put("totalAffected", total);

        return impact;
    }
}
