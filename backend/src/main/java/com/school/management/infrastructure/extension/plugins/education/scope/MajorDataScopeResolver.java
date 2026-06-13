package com.school.management.infrastructure.extension.plugins.education.scope;

import com.school.management.infrastructure.extension.DataScopeResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * BY_MAJOR 维度 — 按用户所属专业反查可访问的学生/课程数据.
 *
 * <p>语义:
 * <ul>
 *   <li>"我能看到哪些 student?" = 我所在专业 OR 我作为负责教师的专业 下的所有学生</li>
 *   <li>"我能看到哪些 course?" = 同一专业 ({@code teaching_task.major_id}) 下的课程</li>
 * </ul>
 *
 * <p>专业归属来源 (取并集):
 * <ol>
 *   <li>{@code user_teacher.specialties} (JSON 数组 of major_ids) — 教师明示的专长</li>
 *   <li>{@code majors.lead_teacher_id = userId} — 该用户是某专业的负责人</li>
 *   <li>{@code user_teacher.org_unit_id} 是某专业的所属系部 — 间接关联 (放宽口径)</li>
 * </ol>
 *
 * <p>返回:
 * <ul>
 *   <li>null = 用户没有任何专业归属 (degrade SELF)</li>
 *   <li>empty list = 专业归属但该专业下 0 资源 (deny all)</li>
 *   <li>non-empty = 可访问 resource id 列表</li>
 * </ul>
 *
 * <p>data_scope_dims.resolver_type FQCN 必须与此类路径一致,
 * PluginDataScopeRouter.loadResolver 通过反射 Class.forName 解析.
 */
@Slf4j
@Component
public class MajorDataScopeResolver implements DataScopeResolver {

    private final JdbcTemplate jdbc;

    public MajorDataScopeResolver(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Long> resolve(Long userId, String resourceType) {
        if (userId == null || resourceType == null) return null;

        // Step 1: 反推用户所属专业 ids
        List<Long> majorIds = findUserMajorIds(userId);
        if (majorIds == null) return null;       // 不是老师 / 不在任何专业 → degrade SELF
        if (majorIds.isEmpty()) return Collections.emptyList(); // 是老师但无专业 → deny

        // Step 2: 按 resourceType 反查 id 列表
        return switch (resourceType) {
            case "student" -> queryStudentIdsByMajors(majorIds);
            case "teaching_task" -> queryTeachingTaskIdsByMajors(majorIds);
            case "school_class" -> queryClassIdsByMajors(majorIds);
            default -> {
                log.debug("[BY_MAJOR] resourceType '{}' not supported, degrade to SELF", resourceType);
                yield null;
            }
        };
    }

    /** 返 null = 完全没有专业归属 (degrade SELF). 返 empty = 是老师但无专业关联. */
    private List<Long> findUserMajorIds(Long userId) {
        try {
            // (1) majors.lead_teacher_id 直接命中
            List<Long> leadOf = jdbc.queryForList(
                "SELECT id FROM majors WHERE lead_teacher_id = ? AND deleted = 0",
                Long.class, userId);

            // (2) 教师的 member 归属系部是某 major 的归属系部
            //     —— 教师"属于哪个系部"已统一到 access_relations member 关系
            //     (subject=user:userId, resource=org_unit), 不再读 user_teacher.org_unit_id。
            List<Long> orgMajors = jdbc.queryForList(
                "SELECT DISTINCT m.id FROM majors m " +
                "JOIN access_relations ar ON ar.resource_id = m.org_unit_id " +
                "  AND ar.relation = 'member' AND ar.resource_type = 'org_unit' " +
                "  AND ar.subject_type = 'user' AND ar.deleted = 0 " +
                "  AND (ar.valid_to IS NULL OR ar.valid_to > NOW()) " +
                "WHERE ar.subject_id = ? AND m.deleted = 0",
                Long.class, userId);

            // (3) user_teacher.specialties JSON 包含的 major_id (兼容 JSON 字符串数组)
            // 用 JSON_OVERLAPS 需 MySQL 8+, JSON_CONTAINS 兼容性更好
            List<Long> specialtyMajors = jdbc.queryForList(
                "SELECT m.id FROM majors m " +
                "JOIN user_teacher ut ON JSON_CONTAINS(ut.specialties, CAST(m.id AS JSON)) " +
                "WHERE ut.user_id = ? AND ut.deleted = 0 AND m.deleted = 0",
                Long.class, userId);

            // 合并去重
            java.util.Set<Long> all = new java.util.HashSet<>();
            all.addAll(leadOf);
            all.addAll(orgMajors);
            all.addAll(specialtyMajors);

            // 没有任何 user_teacher 记录 → 用户不是教师, degrade SELF
            Integer teacherCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM user_teacher WHERE user_id = ? AND deleted = 0",
                Integer.class, userId);
            if (teacherCount == null || teacherCount == 0) return null;

            return new java.util.ArrayList<>(all);
        } catch (Exception e) {
            log.warn("[BY_MAJOR] findUserMajorIds failed for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private List<Long> queryStudentIdsByMajors(List<Long> majorIds) {
        if (majorIds.isEmpty()) return Collections.emptyList();
        String placeholders = majorIds.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("");
        // 学生归属已统一到 access_relations member 关系 (subject=学生 user_id, resource=班级 org_unit),
        // user_student 不再有 major_id 列。专业**不直接含学生**, 经班级:
        //   major → classes.major_id 反查归属该专业的班级 → member 学生。
        // 注意 classes 是 org_units 上的 VIEW (WHERE type_code='CLASS'): c.id = 班级自身 org_unit id
        // (学生即 member 于它), 而 c.org_unit_id = 父节点(年级)。故取 c.id 不取 c.org_unit_id
        // (与 queryClassIdsByMajors 的 SELECT id 一致)。
        return jdbc.queryForList(
            "SELECT s.id FROM user_student s " +
            "JOIN access_relations mar ON mar.subject_id = s.user_id " +
            "  AND mar.relation = 'member' AND mar.resource_type = 'org_unit' " +
            "  AND mar.subject_type = 'user' AND mar.deleted = 0 " +
            "  AND (mar.valid_to IS NULL OR mar.valid_to > NOW()) " +
            "WHERE mar.resource_id IN (" +
            "  SELECT c.id FROM classes c " +
            "  WHERE c.major_id IN (" + placeholders + ") AND c.deleted = 0" +
            ") AND s.deleted = 0",
            Long.class, majorIds.toArray());
    }

    private List<Long> queryTeachingTaskIdsByMajors(List<Long> majorIds) {
        if (majorIds.isEmpty()) return Collections.emptyList();
        // teaching_task 没有 major_id 字段, 走 课程→专业 间接关联
        // 简化: 暂返回 empty, 真业务需求时扩展
        return Collections.emptyList();
    }

    private List<Long> queryClassIdsByMajors(List<Long> majorIds) {
        if (majorIds.isEmpty()) return Collections.emptyList();
        String placeholders = majorIds.stream().map(id -> "?").reduce((a, b) -> a + "," + b).orElse("");
        return jdbc.queryForList(
            "SELECT id FROM classes WHERE major_id IN (" + placeholders + ") AND deleted = 0",
            Long.class, majorIds.toArray());
    }
}
