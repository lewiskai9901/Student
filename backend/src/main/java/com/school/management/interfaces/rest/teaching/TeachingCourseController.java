package com.school.management.interfaces.rest.teaching;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 课程管理 REST Controller
 * 处理课程的 CRUD
 *
 * 使用 JdbcTemplate 直接操作 DB 表:
 * - courses
 */
@Slf4j
@RestController
@RequestMapping("/teaching")
@RequiredArgsConstructor
public class TeachingCourseController {

    private final JdbcTemplate jdbc;

    private static final String SELECT_COLUMNS =
        "id, course_code AS courseCode, course_name AS courseName, " +
        "course_name_en AS courseNameEn, course_category AS courseCategory, " +
        "course_type AS courseType, course_nature AS courseNature, " +
        "credits, total_hours AS totalHours, theory_hours AS theoryHours, " +
        "practice_hours AS practiceHours, weekly_hours AS weeklyHours, " +
        "exam_type AS examType, org_unit_id AS orgUnitId, " +
        "description, status, created_by AS createdBy, " +
        "created_at AS createdAt, updated_at AS updatedAt";

    // ==================== 课程列表(分页) ====================

    @GetMapping("/courses")
    public Result<Map<String, Object>> listCourses(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer courseCategory,
            @RequestParam(required = false) Integer courseType,
            @RequestParam(required = false) Integer status) {

        StringBuilder where = new StringBuilder(" WHERE deleted = 0");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (course_code LIKE ? OR course_name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }
        if (courseCategory != null) {
            where.append(" AND course_category = ?");
            params.add(courseCategory);
        }
        if (courseType != null) {
            where.append(" AND course_type = ?");
            params.add(courseType);
        }
        if (status != null) {
            where.append(" AND status = ?");
            params.add(status);
        }

        // Count
        String countSql = "SELECT COUNT(*) FROM courses" + where;
        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        // Data
        int offset = (pageNum - 1) * pageSize;
        String dataSql = "SELECT " + SELECT_COLUMNS + " FROM courses" + where +
            " ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbc.queryForList(dataSql, dataParams.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return Result.success(result);
    }

    // ==================== 全部课程(下拉用) ====================

    @GetMapping("/courses/all")
    public Result<List<Map<String, Object>>> listAllCourses() {
        List<Map<String, Object>> courses = jdbc.queryForList(
            "SELECT " + SELECT_COLUMNS + " FROM courses WHERE deleted = 0 ORDER BY course_code"
        );
        return Result.success(courses);
    }

    // ==================== 按ID获取 ====================

    @GetMapping("/courses/{id}")
    public Result<Map<String, Object>> getCourse(@PathVariable Long id) {
        Map<String, Object> course = jdbc.queryForMap(
            "SELECT " + SELECT_COLUMNS + " FROM courses WHERE id = ? AND deleted = 0", id
        );
        return Result.success(course);
    }

    // ==================== 按课程代码获取 ====================

    @GetMapping("/courses/code/{code}")
    public Result<Map<String, Object>> getCourseByCode(@PathVariable String code) {
        Map<String, Object> course = jdbc.queryForMap(
            "SELECT " + SELECT_COLUMNS + " FROM courses WHERE course_code = ? AND deleted = 0", code
        );
        return Result.success(course);
    }

    // ==================== 创建课程 ====================

    @PostMapping("/courses")
    public Result<Map<String, Object>> createCourse(@RequestBody Map<String, Object> data) {
        String courseCode = (String) data.get("courseCode");
        String courseName = (String) data.get("courseName");
        String courseNameEn = (String) data.get("courseNameEn");
        Integer courseCategory = data.get("courseCategory") != null ? ((Number) data.get("courseCategory")).intValue() : 1;
        Integer courseType = data.get("courseType") != null ? ((Number) data.get("courseType")).intValue() : 1;
        Integer courseNature = data.get("courseNature") != null ? ((Number) data.get("courseNature")).intValue() : 1;
        BigDecimal credits = data.get("credits") != null ? new BigDecimal(data.get("credits").toString()) : BigDecimal.ZERO;
        Integer totalHours = data.get("totalHours") != null ? ((Number) data.get("totalHours")).intValue() : 0;
        Integer theoryHours = data.get("theoryHours") != null ? ((Number) data.get("theoryHours")).intValue() : 0;
        Integer practiceHours = data.get("practiceHours") != null ? ((Number) data.get("practiceHours")).intValue() : 0;
        Integer weeklyHours = data.get("weeklyHours") != null ? ((Number) data.get("weeklyHours")).intValue() : 2;
        Integer examType = data.get("examType") != null ? ((Number) data.get("examType")).intValue() : 1;
        Long orgUnitId = data.get("orgUnitId") != null ? ((Number) data.get("orgUnitId")).longValue() : null;
        String description = (String) data.get("description");
        Integer status = data.get("status") != null ? ((Number) data.get("status")).intValue() : 1;
        Long createdBy = data.get("createdBy") != null ? ((Number) data.get("createdBy")).longValue() : null;

        long id = IdWorker.getId();
        jdbc.update(
            "INSERT INTO courses (id, course_code, course_name, course_name_en, " +
            "course_category, course_type, course_nature, credits, total_hours, " +
            "theory_hours, practice_hours, weekly_hours, exam_type, org_unit_id, " +
            "description, status, created_by, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
            id, courseCode, courseName, courseNameEn,
            courseCategory, courseType, courseNature, credits, totalHours,
            theoryHours, practiceHours, weeklyHours, examType, orgUnitId,
            description, status, createdBy
        );

        data.put("id", id);
        return Result.success(data);
    }

    // ==================== 更新课程 ====================

    @PutMapping("/courses/{id}")
    public Result<Void> updateCourse(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        String courseName = (String) data.get("courseName");
        String courseNameEn = (String) data.get("courseNameEn");
        Integer courseCategory = data.get("courseCategory") != null ? ((Number) data.get("courseCategory")).intValue() : null;
        Integer courseType = data.get("courseType") != null ? ((Number) data.get("courseType")).intValue() : null;
        Integer courseNature = data.get("courseNature") != null ? ((Number) data.get("courseNature")).intValue() : null;
        BigDecimal credits = data.get("credits") != null ? new BigDecimal(data.get("credits").toString()) : null;
        Integer totalHours = data.get("totalHours") != null ? ((Number) data.get("totalHours")).intValue() : null;
        Integer theoryHours = data.get("theoryHours") != null ? ((Number) data.get("theoryHours")).intValue() : null;
        Integer practiceHours = data.get("practiceHours") != null ? ((Number) data.get("practiceHours")).intValue() : null;
        Integer weeklyHours = data.get("weeklyHours") != null ? ((Number) data.get("weeklyHours")).intValue() : null;
        Integer examType = data.get("examType") != null ? ((Number) data.get("examType")).intValue() : null;
        Long orgUnitId = data.get("orgUnitId") != null ? ((Number) data.get("orgUnitId")).longValue() : null;
        String description = (String) data.get("description");
        Integer status = data.get("status") != null ? ((Number) data.get("status")).intValue() : null;

        StringBuilder sql = new StringBuilder("UPDATE courses SET updated_at = NOW()");
        List<Object> params = new ArrayList<>();

        if (courseName != null) { sql.append(", course_name = ?"); params.add(courseName); }
        if (courseNameEn != null) { sql.append(", course_name_en = ?"); params.add(courseNameEn); }
        if (courseCategory != null) { sql.append(", course_category = ?"); params.add(courseCategory); }
        if (courseType != null) { sql.append(", course_type = ?"); params.add(courseType); }
        if (courseNature != null) { sql.append(", course_nature = ?"); params.add(courseNature); }
        if (credits != null) { sql.append(", credits = ?"); params.add(credits); }
        if (totalHours != null) { sql.append(", total_hours = ?"); params.add(totalHours); }
        if (theoryHours != null) { sql.append(", theory_hours = ?"); params.add(theoryHours); }
        if (practiceHours != null) { sql.append(", practice_hours = ?"); params.add(practiceHours); }
        if (weeklyHours != null) { sql.append(", weekly_hours = ?"); params.add(weeklyHours); }
        if (examType != null) { sql.append(", exam_type = ?"); params.add(examType); }
        if (data.containsKey("orgUnitId")) { sql.append(", org_unit_id = ?"); params.add(orgUnitId); }
        if (data.containsKey("description")) { sql.append(", description = ?"); params.add(description); }
        if (status != null) { sql.append(", status = ?"); params.add(status); }

        sql.append(" WHERE id = ? AND deleted = 0");
        params.add(id);

        jdbc.update(sql.toString(), params.toArray());
        return Result.success();
    }

    // ==================== 删除课程(软删除) ====================

    @DeleteMapping("/courses/{id}")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        jdbc.update("UPDATE courses SET deleted = 1 WHERE id = ?", id);
        return Result.success();
    }

    // ==================== 更新状态 ====================

    @PatchMapping("/courses/{id}/status")
    public Result<Void> updateCourseStatus(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Integer status = ((Number) data.get("status")).intValue();
        jdbc.update("UPDATE courses SET status = ?, updated_at = NOW() WHERE id = ? AND deleted = 0", status, id);
        return Result.success();
    }
}
