package com.school.management.application.organization;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Application service holding teacher profile data-access logic.
 *
 * Extracted from TeacherProfileController to remove direct JdbcTemplate usage
 * from the REST layer, consistent with the asset/system module refactor pattern.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherProfileApplicationService {

    private final JdbcTemplate jdbc;
    private final MembershipResolver membershipResolver;

    /**
     * 教师归属 (系部) 改由 access_relations member 关系派生 —— 不再读 user_teacher.org_unit_id (已删)。
     * LEFT JOIN 取该教师 user 的 member 归属行 (resource_type=org_unit), 用别名 mar 暴露 resource_id。
     */
    private static final String MEMBER_JOIN =
        " LEFT JOIN access_relations mar ON mar.subject_type = 'user' AND mar.subject_id = tp.user_id " +
        "   AND mar.relation = 'member' AND mar.resource_type = 'org_unit' AND mar.deleted = 0 " +
        "   AND (mar.valid_to IS NULL OR mar.valid_to > NOW())";

    private static final String PROFILE_COLUMNS =
        "tp.id, tp.user_id AS userId, tp.employee_no AS employeeNo, " +
        "tp.title, tp.title_level AS titleLevel, " +
        "mar.resource_id AS orgUnitId, ou.unit_name AS orgUnitName, " +
        "tp.teaching_group AS teachingGroup, tp.max_weekly_hours AS maxWeeklyHours, " +
        "tp.qualification, tp.specialties, tp.hire_date AS hireDate, " +
        "tp.status, tp.remark, tp.created_at AS createdAt, tp.updated_at AS updatedAt, " +
        "u.real_name AS realName, u.phone, u.username AS userName";

    // ==================== List (paginated) ====================

    @Transactional(readOnly = true)
    public Map<String, Object> listProfiles(int pageNum, int pageSize, Long orgUnitId,
                                            String title, Integer status, String keyword) {
        StringBuilder where = new StringBuilder(" WHERE tp.deleted = 0");
        List<Object> params = new ArrayList<>();

        if (orgUnitId != null) {
            where.append(" AND mar.resource_id = ?");
            params.add(orgUnitId);
        }
        if (title != null && !title.isEmpty()) {
            where.append(" AND tp.title = ?");
            params.add(title);
        }
        if (status != null) {
            where.append(" AND tp.status = ?");
            params.add(status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (u.real_name LIKE ? OR tp.employee_no LIKE ? OR u.phone LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        String countSql = "SELECT COUNT(*) FROM user_teacher tp " +
            "LEFT JOIN users u ON tp.user_id = u.id" + MEMBER_JOIN + where;
        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        int offset = (pageNum - 1) * pageSize;
        String dataSql = "SELECT " + PROFILE_COLUMNS +
            " FROM user_teacher tp" +
            " LEFT JOIN users u ON tp.user_id = u.id" +
            MEMBER_JOIN +
            " LEFT JOIN org_units ou ON ou.id = mar.resource_id" +
            where +
            " ORDER BY tp.created_at DESC LIMIT ? OFFSET ?";
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbc.queryForList(dataSql, dataParams.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    // ==================== Get by ID ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getProfile(Long id) {
        return jdbc.queryForMap(
            "SELECT " + PROFILE_COLUMNS +
            " FROM user_teacher tp" +
            " LEFT JOIN users u ON tp.user_id = u.id" +
            MEMBER_JOIN +
            " LEFT JOIN org_units ou ON ou.id = mar.resource_id" +
            " WHERE tp.id = ? AND tp.deleted = 0", id
        );
    }

    // ==================== Get by User ID ====================

    @Transactional(readOnly = true)
    public Map<String, Object> getByUserId(Long userId) {
        try {
            return jdbc.queryForMap(
                "SELECT " + PROFILE_COLUMNS +
                " FROM user_teacher tp" +
                " LEFT JOIN users u ON tp.user_id = u.id" +
                MEMBER_JOIN +
                " LEFT JOIN org_units ou ON ou.id = mar.resource_id" +
                " WHERE tp.user_id = ? AND tp.deleted = 0", userId
            );
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    // ==================== Create ====================

    /**
     * @return the new profile id, or {@code null} if the user already has a profile.
     */
    @Transactional
    public Long createProfile(Long userId, Map<String, Object> data) {
        Long existCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM user_teacher WHERE user_id = ? AND deleted = 0",
            Long.class, userId
        );
        if (existCount != null && existCount > 0) {
            return null;
        }

        long id = IdWorker.getId();
        jdbc.update(
            "INSERT INTO user_teacher (id, user_id, employee_no, title, title_level, " +
            "teaching_group, max_weekly_hours, qualification, specialties, " +
            "hire_date, status, remark) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id, userId,
            data.get("employeeNo"),
            data.get("title"),
            data.get("titleLevel"),
            data.get("teachingGroup"),
            data.get("maxWeeklyHours") != null ? ((Number) data.get("maxWeeklyHours")).intValue() : 20,
            data.get("qualification"),
            data.get("specialties") != null ? toJsonString(data.get("specialties")) : null,
            toDate(data.get("hireDate")),
            data.get("status") != null ? ((Number) data.get("status")).intValue() : 1,
            data.get("remark")
        );
        // 教师归属 (系部) 统一写 access_relations member 关系 (不再写 user_teacher.org_unit_id)
        Long orgUnitId = toLong(data.get("orgUnitId"));
        if (orgUnitId != null) {
            membershipResolver.setMembership(userId, orgUnitId);
        }
        return id;
    }

    // ==================== Update ====================

    @Transactional
    public void updateProfile(Long id, Map<String, Object> data) {
        jdbc.update(
            "UPDATE user_teacher SET " +
            "employee_no = ?, title = ?, title_level = ?, " +
            "teaching_group = ?, max_weekly_hours = ?, qualification = ?, " +
            "specialties = ?, hire_date = ?, status = ?, remark = ? " +
            "WHERE id = ? AND deleted = 0",
            data.get("employeeNo"),
            data.get("title"),
            data.get("titleLevel"),
            data.get("teachingGroup"),
            data.get("maxWeeklyHours") != null ? ((Number) data.get("maxWeeklyHours")).intValue() : 20,
            data.get("qualification"),
            data.get("specialties") != null ? toJsonString(data.get("specialties")) : null,
            toDate(data.get("hireDate")),
            data.get("status") != null ? ((Number) data.get("status")).intValue() : 1,
            data.get("remark"),
            id
        );
        // 教师归属 (系部) 变更统一写 access_relations member 关系 (不再写 user_teacher.org_unit_id)
        Long orgUnitId = toLong(data.get("orgUnitId"));
        if (orgUnitId != null) {
            Long userId = jdbc.queryForObject(
                "SELECT user_id FROM user_teacher WHERE id = ? AND deleted = 0", Long.class, id);
            if (userId != null) {
                membershipResolver.setMembership(userId, orgUnitId);
            }
        }
    }

    // ==================== Delete (soft) ====================

    @Transactional
    public void deleteProfile(Long id) {
        jdbc.update("UPDATE user_teacher SET deleted = 1 WHERE id = ?", id);
    }

    // ==================== Course Qualifications ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCourses(Long teacherProfileId) {
        return jdbc.queryForList(
            "SELECT tcq.id, tcq.teacher_profile_id AS teacherProfileId, " +
            "tcq.course_id AS courseId, tcq.qualification_level AS qualificationLevel, " +
            "tcq.remark, tcq.created_at AS createdAt, " +
            "c.course_name AS courseName, c.course_code AS courseCode " +
            "FROM teacher_course_qualifications tcq " +
            "LEFT JOIN courses c ON tcq.course_id = c.id " +
            "WHERE tcq.teacher_profile_id = ? " +
            "ORDER BY tcq.created_at DESC", teacherProfileId
        );
    }

    /**
     * @return the new qualification id, or {@code null} if the teacher already
     *         has a qualification for this course.
     */
    @Transactional
    public Long addCourse(Long teacherProfileId, Long courseId, Map<String, Object> data) {
        Long existCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM teacher_course_qualifications " +
            "WHERE teacher_profile_id = ? AND course_id = ?",
            Long.class, teacherProfileId, courseId
        );
        if (existCount != null && existCount > 0) {
            return null;
        }

        long newId = IdWorker.getId();
        jdbc.update(
            "INSERT INTO teacher_course_qualifications (id, teacher_profile_id, course_id, " +
            "qualification_level, remark) VALUES (?, ?, ?, ?, ?)",
            newId, teacherProfileId, courseId,
            data.get("qualificationLevel") != null ? ((Number) data.get("qualificationLevel")).intValue() : 1,
            data.get("remark")
        );
        return newId;
    }

    @Transactional
    public void removeCourse(Long teacherProfileId, Long courseId) {
        jdbc.update(
            "DELETE FROM teacher_course_qualifications " +
            "WHERE teacher_profile_id = ? AND course_id = ?", teacherProfileId, courseId
        );
    }

    // ==================== Query: available teachers for a course ====================

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAvailableForCourse(Long courseId) {
        return jdbc.queryForList(
            "SELECT " + PROFILE_COLUMNS + ", " +
            "tcq.qualification_level AS qualificationLevel " +
            "FROM teacher_course_qualifications tcq " +
            "JOIN user_teacher tp ON tcq.teacher_profile_id = tp.id " +
            "LEFT JOIN users u ON tp.user_id = u.id " +
            MEMBER_JOIN +
            "LEFT JOIN org_units ou ON ou.id = mar.resource_id " +
            "WHERE tcq.course_id = ? AND tp.deleted = 0 AND tp.status = 1 " +
            "ORDER BY tcq.qualification_level DESC, u.real_name",
            courseId
        );
    }

    // ==================== Helpers ====================

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LocalDate toDate(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate) return (LocalDate) val;
        return LocalDate.parse(val.toString());
    }

    @SuppressWarnings("unchecked")
    private String toJsonString(Object val) {
        if (val == null) return null;
        if (val instanceof String) return (String) val;
        // If it's a List (from JSON deserialization), convert to JSON string
        if (val instanceof List) {
            List<String> list = (List<String>) val;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append("\"").append(list.get(i).replace("\"", "\\\"")).append("\"");
            }
            sb.append("]");
            return sb.toString();
        }
        return val.toString();
    }
}
