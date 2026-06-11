package com.school.management.infrastructure.extension.plugins.education.scope;

import com.school.management.infrastructure.extension.DataScopePlugin.DataScopeResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * BY_GRADE 维度 — 按用户负责的年级反查可访问的学生数据.
 *
 * <p>语义:用户是某年级的 director / deputy_director / counselor → 该年级下所有学生.
 *
 * <p>年级归属来源 — {@code grade_directors} 表 (1 grade ↔ 1 row):
 * <ul>
 *   <li>{@code director_id = userId}</li>
 *   <li>{@code deputy_director_ids} (JSON 数组) 包含 userId</li>
 *   <li>{@code counselor_ids} (JSON 数组) 包含 userId</li>
 * </ul>
 *
 * <p>年级 = org_unit 类型 = GRADE. grade_directors.org_unit_id 得到用户负责的年级 org,
 * 再经 tree_path 子树展开到后代班级 org, 由 access_relations member 关系反查归属学生
 * (user_student 已无 grade_id 列, 见 {@link #queryStudentIdsByGrades}).
 */
@Slf4j
@Component
public class GradeDataScopeResolver implements DataScopeResolver {

    private final JdbcTemplate jdbc;

    public GradeDataScopeResolver(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Long> resolve(Long userId, String resourceType) {
        if (userId == null || resourceType == null) return null;

        List<Long> gradeOrgUnitIds = findUserGradeOrgUnitIds(userId);
        if (gradeOrgUnitIds == null) return null;
        if (gradeOrgUnitIds.isEmpty()) return Collections.emptyList();

        return switch (resourceType) {
            case "student" -> queryStudentIdsByGrades(gradeOrgUnitIds);
            case "school_class" -> queryClassIdsByGrades(gradeOrgUnitIds);
            default -> {
                log.debug("[BY_GRADE] resourceType '{}' not supported, degrade SELF", resourceType);
                yield null;
            }
        };
    }

    private List<Long> findUserGradeOrgUnitIds(Long userId) {
        try {
            // 一条 SQL 合并 3 种命中
            // director_id 直接命中 OR deputy_director_ids/counselor_ids JSON 包含
            return jdbc.queryForList(
                "SELECT DISTINCT org_unit_id FROM grade_directors " +
                "WHERE deleted = 0 AND (" +
                "  director_id = ? " +
                "  OR JSON_CONTAINS(COALESCE(deputy_director_ids, JSON_ARRAY()), CAST(? AS JSON)) " +
                "  OR JSON_CONTAINS(COALESCE(counselor_ids, JSON_ARRAY()), CAST(? AS JSON))" +
                ")",
                Long.class, userId, userId, userId);
        } catch (Exception e) {
            log.warn("[BY_GRADE] findUserGradeOrgUnitIds failed for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private List<Long> queryStudentIdsByGrades(List<Long> gradeOrgUnitIds) {
        if (gradeOrgUnitIds.isEmpty()) return Collections.emptyList();
        String placeholders = gradeOrgUnitIds.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("");
        // 学生归属已统一到 access_relations member 关系 (subject=学生 user_id, resource=班级 org_unit),
        // user_student 不再有 grade_id 列。年级**不直接含学生**, 经子班级:
        //   年级 org → tree_path 子树展开到后代班级 org → member 学生
        // (与 DataPermissionInterceptor.buildMembershipCondition 的 CUSTOM 子树口径、
        //  以及 ClassDataScopeResolver.queryStudentIdsByClasses 的 member 口径一致)
        return jdbc.queryForList(
            "SELECT s.id FROM user_student s " +
            "JOIN access_relations mar ON mar.subject_id = s.user_id " +
            "  AND mar.relation = 'member' AND mar.resource_type = 'org_unit' " +
            "  AND mar.subject_type = 'user' AND mar.deleted = 0 " +
            "  AND (mar.valid_to IS NULL OR mar.valid_to > NOW()) " +
            "WHERE mar.resource_id IN (" +
            "  SELECT o.id FROM org_units o JOIN org_units g ON g.id IN (" + placeholders + ") " +
            "  WHERE o.deleted = 0 AND o.tree_path LIKE CONCAT(g.tree_path, '%')" +
            ") AND s.deleted = 0",
            Long.class, gradeOrgUnitIds.toArray());
    }

    private List<Long> queryClassIdsByGrades(List<Long> gradeOrgUnitIds) {
        if (gradeOrgUnitIds.isEmpty()) return Collections.emptyList();
        String placeholders = gradeOrgUnitIds.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("");
        return jdbc.queryForList(
            "SELECT id FROM classes " +
            "WHERE grade_id IN (" + placeholders + ") AND deleted = 0",
            Long.class, gradeOrgUnitIds.toArray());
    }
}
