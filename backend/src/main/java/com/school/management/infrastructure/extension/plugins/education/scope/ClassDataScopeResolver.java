package com.school.management.infrastructure.extension.plugins.education.scope;

import com.school.management.infrastructure.extension.DataScopePlugin.DataScopeResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * BY_CLASS 维度 — 按用户作为班主任/任课老师/管理员所绑定的班级反查可访问的学生数据.
 *
 * <p>班级归属来源 — {@code access_relations}:
 * <ul>
 *   <li>relation = CLASS_TEACHER + subject_type=user + subject_id=userId → 班主任</li>
 *   <li>relation = SUBJECT_TEACHER + subject_type=user + subject_id=userId → 任课</li>
 *   <li>relation = admin + subject_type=user + subject_id=userId + resource_type=org_unit
 *       (且 org_unit 是 CLASS 类型) → 班级管理员</li>
 *   <li>classes.teacher_id = userId → 直接挂的班主任 (兼容老数据)</li>
 * </ul>
 *
 * <p>resource_id (access_relations) 是班级 org_unit_id, 反查 user_student.org_unit_id IN.
 */
@Slf4j
@Component
public class ClassDataScopeResolver implements DataScopeResolver {

    private final JdbcTemplate jdbc;

    public ClassDataScopeResolver(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Long> resolve(Long userId, String resourceType) {
        if (userId == null || resourceType == null) return null;

        List<Long> classOrgUnitIds = findUserClassOrgUnitIds(userId);
        if (classOrgUnitIds == null) return null;
        if (classOrgUnitIds.isEmpty()) return Collections.emptyList();

        return switch (resourceType) {
            case "student" -> queryStudentIdsByClasses(classOrgUnitIds);
            case "school_class" -> classOrgUnitIds;     // 直接返回 class 自己的 id
            case "attendance", "student_grade" -> queryByStudentClass(classOrgUnitIds, resourceType);
            default -> {
                log.debug("[BY_CLASS] resourceType '{}' not supported, degrade SELF", resourceType);
                yield null;
            }
        };
    }

    private List<Long> findUserClassOrgUnitIds(Long userId) {
        try {
            // (1) access_relations 命中 (CLASS_TEACHER / SUBJECT_TEACHER / admin on CLASS)
            List<Long> fromRelations = jdbc.queryForList(
                "SELECT DISTINCT ar.resource_id FROM access_relations ar " +
                "WHERE ar.deleted = 0 AND ar.subject_type = 'user' AND ar.subject_id = ? " +
                "  AND ar.resource_type = 'org_unit' " +
                "  AND ar.relation IN ('CLASS_TEACHER', 'SUBJECT_TEACHER', 'admin')",
                Long.class, userId);

            // (2) classes.teacher_id 直接命中 (老数据兼容)
            List<Long> fromClassesTable = jdbc.queryForList(
                "SELECT org_unit_id FROM classes WHERE teacher_id = ? AND deleted = 0",
                Long.class, userId);

            java.util.Set<Long> all = new java.util.HashSet<>();
            all.addAll(fromRelations);
            all.addAll(fromClassesTable);
            return new java.util.ArrayList<>(all);
        } catch (Exception e) {
            log.warn("[BY_CLASS] findUserClassOrgUnitIds failed for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private List<Long> queryStudentIdsByClasses(List<Long> classOrgUnitIds) {
        if (classOrgUnitIds.isEmpty()) return Collections.emptyList();
        String placeholders = classOrgUnitIds.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("");
        return jdbc.queryForList(
            "SELECT id FROM user_student " +
            "WHERE org_unit_id IN (" + placeholders + ") AND deleted = 0",
            Long.class, classOrgUnitIds.toArray());
    }

    private List<Long> queryByStudentClass(List<Long> classOrgUnitIds, String resourceType) {
        // attendance / student_grade 这类按 student 间接关联
        List<Long> studentIds = queryStudentIdsByClasses(classOrgUnitIds);
        if (studentIds.isEmpty()) return Collections.emptyList();

        String table = switch (resourceType) {
            case "attendance" -> "attendance_records";
            case "student_grade" -> "student_grades";
            default -> null;
        };
        if (table == null) return Collections.emptyList();

        String placeholders = studentIds.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("");
        try {
            return jdbc.queryForList(
                "SELECT id FROM " + table +
                " WHERE student_id IN (" + placeholders + ") AND deleted = 0",
                Long.class, studentIds.toArray());
        } catch (Exception e) {
            log.warn("[BY_CLASS] query {} failed: {}", table, e.getMessage());
            return Collections.emptyList();
        }
    }
}
