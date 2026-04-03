package com.school.management.interfaces.rest.organization;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * Teacher Profile REST Controller
 *
 * Manages teacher academic profiles (title, qualifications, course capabilities).
 * Uses JdbcTemplate for direct DB access, consistent with asset module pattern.
 */
@Slf4j
@RestController
@RequestMapping("/teacher-profiles")
@RequiredArgsConstructor
@Tag(name = "TeacherProfiles", description = "Teacher profile management API")
public class TeacherProfileController {

    private final JdbcTemplate jdbc;

    private static final String PROFILE_COLUMNS =
        "tp.id, tp.user_id AS userId, tp.employee_no AS employeeNo, " +
        "tp.title, tp.title_level AS titleLevel, " +
        "tp.org_unit_id AS orgUnitId, ou.name AS orgUnitName, " +
        "tp.teaching_group AS teachingGroup, tp.max_weekly_hours AS maxWeeklyHours, " +
        "tp.qualification, tp.specialties, tp.hire_date AS hireDate, " +
        "tp.status, tp.remark, tp.created_at AS createdAt, tp.updated_at AS updatedAt, " +
        "u.real_name AS realName, u.phone, u.username AS userName";

    // ==================== List (paginated) ====================

    @Operation(summary = "Get teacher profile list (paginated)")
    @GetMapping
    @CasbinAccess(resource = "teacher:profile", action = "view")
    public Result<Map<String, Object>> listProfiles(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

        StringBuilder where = new StringBuilder(" WHERE tp.deleted = 0");
        List<Object> params = new ArrayList<>();

        if (orgUnitId != null) {
            where.append(" AND tp.org_unit_id = ?");
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

        String countSql = "SELECT COUNT(*) FROM teacher_profiles tp " +
            "LEFT JOIN users u ON tp.user_id = u.id" + where;
        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        int offset = (pageNum - 1) * pageSize;
        String dataSql = "SELECT " + PROFILE_COLUMNS +
            " FROM teacher_profiles tp" +
            " LEFT JOIN users u ON tp.user_id = u.id" +
            " LEFT JOIN org_units ou ON tp.org_unit_id = ou.id" +
            where +
            " ORDER BY tp.created_at DESC LIMIT ? OFFSET ?";
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbc.queryForList(dataSql, dataParams.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return Result.success(result);
    }

    // ==================== Get by ID ====================

    @Operation(summary = "Get teacher profile by ID")
    @GetMapping("/{id}")
    @CasbinAccess(resource = "teacher:profile", action = "view")
    public Result<Map<String, Object>> getProfile(@PathVariable Long id) {
        Map<String, Object> profile = jdbc.queryForMap(
            "SELECT " + PROFILE_COLUMNS +
            " FROM teacher_profiles tp" +
            " LEFT JOIN users u ON tp.user_id = u.id" +
            " LEFT JOIN org_units ou ON tp.org_unit_id = ou.id" +
            " WHERE tp.id = ? AND tp.deleted = 0", id
        );
        return Result.success(profile);
    }

    // ==================== Get by User ID ====================

    @Operation(summary = "Get teacher profile by user ID")
    @GetMapping("/by-user/{userId}")
    @CasbinAccess(resource = "teacher:profile", action = "view")
    public Result<Map<String, Object>> getByUserId(@PathVariable Long userId) {
        try {
            Map<String, Object> profile = jdbc.queryForMap(
                "SELECT " + PROFILE_COLUMNS +
                " FROM teacher_profiles tp" +
                " LEFT JOIN users u ON tp.user_id = u.id" +
                " LEFT JOIN org_units ou ON tp.org_unit_id = ou.id" +
                " WHERE tp.user_id = ? AND tp.deleted = 0", userId
            );
            return Result.success(profile);
        } catch (EmptyResultDataAccessException e) {
            return Result.success(null);
        }
    }

    // ==================== Create ====================

    @Operation(summary = "Create teacher profile")
    @PostMapping
    @CasbinAccess(resource = "teacher:profile", action = "edit")
    @Transactional
    public Result<Long> createProfile(@RequestBody Map<String, Object> data) {
        // Check if user already has a profile
        Long userId = toLong(data.get("userId"));
        if (userId == null) {
            return Result.error("userId is required");
        }
        Long existCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM teacher_profiles WHERE user_id = ? AND deleted = 0",
            Long.class, userId
        );
        if (existCount != null && existCount > 0) {
            return Result.error("该用户已有教师档案");
        }

        long id = IdWorker.getId();
        jdbc.update(
            "INSERT INTO teacher_profiles (id, user_id, employee_no, title, title_level, " +
            "org_unit_id, teaching_group, max_weekly_hours, qualification, specialties, " +
            "hire_date, status, remark) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id, userId,
            data.get("employeeNo"),
            data.get("title"),
            data.get("titleLevel"),
            toLong(data.get("orgUnitId")),
            data.get("teachingGroup"),
            data.get("maxWeeklyHours") != null ? ((Number) data.get("maxWeeklyHours")).intValue() : 20,
            data.get("qualification"),
            data.get("specialties") != null ? toJsonString(data.get("specialties")) : null,
            toDate(data.get("hireDate")),
            data.get("status") != null ? ((Number) data.get("status")).intValue() : 1,
            data.get("remark")
        );
        return Result.success(id);
    }

    // ==================== Update ====================

    @Operation(summary = "Update teacher profile")
    @PutMapping("/{id}")
    @CasbinAccess(resource = "teacher:profile", action = "edit")
    @Transactional
    public Result<Void> updateProfile(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        jdbc.update(
            "UPDATE teacher_profiles SET " +
            "employee_no = ?, title = ?, title_level = ?, org_unit_id = ?, " +
            "teaching_group = ?, max_weekly_hours = ?, qualification = ?, " +
            "specialties = ?, hire_date = ?, status = ?, remark = ? " +
            "WHERE id = ? AND deleted = 0",
            data.get("employeeNo"),
            data.get("title"),
            data.get("titleLevel"),
            toLong(data.get("orgUnitId")),
            data.get("teachingGroup"),
            data.get("maxWeeklyHours") != null ? ((Number) data.get("maxWeeklyHours")).intValue() : 20,
            data.get("qualification"),
            data.get("specialties") != null ? toJsonString(data.get("specialties")) : null,
            toDate(data.get("hireDate")),
            data.get("status") != null ? ((Number) data.get("status")).intValue() : 1,
            data.get("remark"),
            id
        );
        return Result.success();
    }

    // ==================== Delete (soft) ====================

    @Operation(summary = "Delete teacher profile")
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "teacher:profile", action = "edit")
    @Transactional
    public Result<Void> deleteProfile(@PathVariable Long id) {
        jdbc.update("UPDATE teacher_profiles SET deleted = 1 WHERE id = ?", id);
        return Result.success();
    }

    // ==================== Course Qualifications ====================

    @Operation(summary = "Get courses for a teacher")
    @GetMapping("/{id}/courses")
    @CasbinAccess(resource = "teacher:profile", action = "view")
    public Result<List<Map<String, Object>>> getCourses(@PathVariable Long id) {
        List<Map<String, Object>> courses = jdbc.queryForList(
            "SELECT tcq.id, tcq.teacher_profile_id AS teacherProfileId, " +
            "tcq.course_id AS courseId, tcq.qualification_level AS qualificationLevel, " +
            "tcq.remark, tcq.created_at AS createdAt, " +
            "c.course_name AS courseName, c.course_code AS courseCode " +
            "FROM teacher_course_qualifications tcq " +
            "LEFT JOIN courses c ON tcq.course_id = c.id " +
            "WHERE tcq.teacher_profile_id = ? " +
            "ORDER BY tcq.created_at DESC", id
        );
        return Result.success(courses);
    }

    @Operation(summary = "Add course qualification to teacher")
    @PostMapping("/{id}/courses")
    @CasbinAccess(resource = "teacher:profile", action = "edit")
    @Transactional
    public Result<Long> addCourse(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long courseId = toLong(data.get("courseId"));
        if (courseId == null) {
            return Result.error("courseId is required");
        }

        // Check duplicate
        Long existCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM teacher_course_qualifications " +
            "WHERE teacher_profile_id = ? AND course_id = ?",
            Long.class, id, courseId
        );
        if (existCount != null && existCount > 0) {
            return Result.error("该教师已添加此课程资质");
        }

        long newId = IdWorker.getId();
        jdbc.update(
            "INSERT INTO teacher_course_qualifications (id, teacher_profile_id, course_id, " +
            "qualification_level, remark) VALUES (?, ?, ?, ?, ?)",
            newId, id, courseId,
            data.get("qualificationLevel") != null ? ((Number) data.get("qualificationLevel")).intValue() : 1,
            data.get("remark")
        );
        return Result.success(newId);
    }

    @Operation(summary = "Remove course qualification from teacher")
    @DeleteMapping("/{id}/courses/{courseId}")
    @CasbinAccess(resource = "teacher:profile", action = "edit")
    @Transactional
    public Result<Void> removeCourse(@PathVariable Long id, @PathVariable Long courseId) {
        jdbc.update(
            "DELETE FROM teacher_course_qualifications " +
            "WHERE teacher_profile_id = ? AND course_id = ?", id, courseId
        );
        return Result.success();
    }

    // ==================== Query: available teachers for a course ====================

    @Operation(summary = "Get teachers qualified for a specific course")
    @GetMapping("/available")
    @CasbinAccess(resource = "teacher:profile", action = "view")
    public Result<List<Map<String, Object>>> getAvailableForCourse(
            @RequestParam Long courseId) {
        List<Map<String, Object>> teachers = jdbc.queryForList(
            "SELECT " + PROFILE_COLUMNS + ", " +
            "tcq.qualification_level AS qualificationLevel " +
            "FROM teacher_course_qualifications tcq " +
            "JOIN teacher_profiles tp ON tcq.teacher_profile_id = tp.id " +
            "LEFT JOIN users u ON tp.user_id = u.id " +
            "LEFT JOIN org_units ou ON tp.org_unit_id = ou.id " +
            "WHERE tcq.course_id = ? AND tp.deleted = 0 AND tp.status = 1 " +
            "ORDER BY tcq.qualification_level DESC, u.real_name",
            courseId
        );
        return Result.success(teachers);
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
