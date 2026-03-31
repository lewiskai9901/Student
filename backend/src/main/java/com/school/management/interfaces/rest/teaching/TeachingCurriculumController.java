package com.school.management.interfaces.rest.teaching;

import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 培养方案管理 REST Controller
 * 处理培养方案及方案课程的 CRUD
 *
 * 使用 JdbcTemplate 直接操作 DB 表:
 * - curriculum_plans
 * - curriculum_plan_courses
 */
@Slf4j
@RestController
@RequestMapping("/teaching")
@RequiredArgsConstructor
public class TeachingCurriculumController {

    private final JdbcTemplate jdbc;

    private static final String PLAN_COLUMNS =
        "cp.id, cp.plan_code AS planCode, cp.plan_name AS planName, " +
        "cp.major_id AS majorId, cp.major_direction_id AS majorDirectionId, " +
        "cp.grade_year AS gradeYear, " +
        "cp.total_credits AS totalCredits, cp.required_credits AS requiredCredits, " +
        "cp.elective_credits AS electiveCredits, cp.practice_credits AS practiceCredits, " +
        "cp.training_objective AS trainingObjective, cp.graduation_requirement AS graduationRequirement, " +
        "cp.version, cp.status, cp.published_at AS publishedAt, cp.published_by AS publishedBy, " +
        "cp.created_by AS createdBy, cp.created_at AS createdAt, cp.updated_at AS updatedAt, " +
        "md.direction_name AS majorDirectionName";

    // ==================== 培养方案列表(分页) ====================

    @GetMapping("/curriculum-plans")
    public Result<Map<String, Object>> listPlans(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer gradeYear,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long majorDirectionId) {

        StringBuilder where = new StringBuilder(" WHERE cp.deleted = 0");
        List<Object> params = new ArrayList<>();

        if (gradeYear != null) {
            where.append(" AND cp.grade_year = ?");
            params.add(gradeYear);
        }
        if (status != null) {
            where.append(" AND cp.status = ?");
            params.add(status);
        }
        if (majorDirectionId != null) {
            where.append(" AND cp.major_direction_id = ?");
            params.add(majorDirectionId);
        }

        // Count
        String countSql = "SELECT COUNT(*) FROM curriculum_plans cp" + where;
        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        // Data
        int offset = (pageNum - 1) * pageSize;
        String dataSql = "SELECT " + PLAN_COLUMNS +
            " FROM curriculum_plans cp" +
            " LEFT JOIN major_directions md ON cp.major_direction_id = md.id" +
            where +
            " ORDER BY cp.created_at DESC LIMIT ? OFFSET ?";
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbc.queryForList(dataSql, dataParams.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return Result.success(result);
    }

    // ==================== 按ID获取 ====================

    @GetMapping("/curriculum-plans/{id}")
    public Result<Map<String, Object>> getPlan(@PathVariable Long id) {
        Map<String, Object> plan = jdbc.queryForMap(
            "SELECT " + PLAN_COLUMNS +
            " FROM curriculum_plans cp" +
            " LEFT JOIN major_directions md ON cp.major_direction_id = md.id" +
            " WHERE cp.id = ? AND cp.deleted = 0", id
        );
        return Result.success(plan);
    }

    // ==================== 创建培养方案 ====================

    @PostMapping("/curriculum-plans")
    public Result<Map<String, Object>> createPlan(@RequestBody Map<String, Object> data) {
        String planCode = (String) data.get("planCode");
        String planName = (String) data.get("planName");
        Long majorId = data.get("majorId") != null ? ((Number) data.get("majorId")).longValue() : null;
        Long majorDirectionId = data.get("majorDirectionId") != null ? ((Number) data.get("majorDirectionId")).longValue() : null;
        Integer gradeYear = data.get("gradeYear") != null ? ((Number) data.get("gradeYear")).intValue() : null;
        BigDecimal totalCredits = data.get("totalCredits") != null ? new BigDecimal(data.get("totalCredits").toString()) : null;
        BigDecimal requiredCredits = data.get("requiredCredits") != null ? new BigDecimal(data.get("requiredCredits").toString()) : null;
        BigDecimal electiveCredits = data.get("electiveCredits") != null ? new BigDecimal(data.get("electiveCredits").toString()) : null;
        BigDecimal practiceCredits = data.get("practiceCredits") != null ? new BigDecimal(data.get("practiceCredits").toString()) : null;
        String trainingObjective = (String) data.get("trainingObjective");
        String graduationRequirement = (String) data.get("graduationRequirement");
        Integer status = data.get("status") != null ? ((Number) data.get("status")).intValue() : 0;
        Long createdBy = data.get("createdBy") != null ? ((Number) data.get("createdBy")).longValue() : null;

        long id = System.currentTimeMillis();
        jdbc.update(
            "INSERT INTO curriculum_plans (id, plan_code, plan_name, major_id, major_direction_id, grade_year, " +
            "total_credits, required_credits, elective_credits, practice_credits, " +
            "training_objective, graduation_requirement, version, status, created_by, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, 0)",
            id, planCode, planName, majorId, majorDirectionId, gradeYear,
            totalCredits, requiredCredits, electiveCredits, practiceCredits,
            trainingObjective, graduationRequirement, status, createdBy
        );

        data.put("id", id);
        return Result.success(data);
    }

    // ==================== 更新培养方案 ====================

    @PutMapping("/curriculum-plans/{id}")
    public Result<Void> updatePlan(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        StringBuilder sql = new StringBuilder("UPDATE curriculum_plans SET updated_at = NOW()");
        List<Object> params = new ArrayList<>();

        if (data.containsKey("planName")) { sql.append(", plan_name = ?"); params.add(data.get("planName")); }
        if (data.containsKey("majorId")) {
            Long majorId = data.get("majorId") != null ? ((Number) data.get("majorId")).longValue() : null;
            sql.append(", major_id = ?"); params.add(majorId);
        }
        if (data.containsKey("majorDirectionId")) {
            Long majorDirectionId = data.get("majorDirectionId") != null ? ((Number) data.get("majorDirectionId")).longValue() : null;
            sql.append(", major_direction_id = ?"); params.add(majorDirectionId);
        }
        if (data.containsKey("gradeYear")) {
            Integer gradeYear = data.get("gradeYear") != null ? ((Number) data.get("gradeYear")).intValue() : null;
            sql.append(", grade_year = ?"); params.add(gradeYear);
        }
        if (data.containsKey("totalCredits")) {
            BigDecimal v = data.get("totalCredits") != null ? new BigDecimal(data.get("totalCredits").toString()) : null;
            sql.append(", total_credits = ?"); params.add(v);
        }
        if (data.containsKey("requiredCredits")) {
            BigDecimal v = data.get("requiredCredits") != null ? new BigDecimal(data.get("requiredCredits").toString()) : null;
            sql.append(", required_credits = ?"); params.add(v);
        }
        if (data.containsKey("electiveCredits")) {
            BigDecimal v = data.get("electiveCredits") != null ? new BigDecimal(data.get("electiveCredits").toString()) : null;
            sql.append(", elective_credits = ?"); params.add(v);
        }
        if (data.containsKey("practiceCredits")) {
            BigDecimal v = data.get("practiceCredits") != null ? new BigDecimal(data.get("practiceCredits").toString()) : null;
            sql.append(", practice_credits = ?"); params.add(v);
        }
        if (data.containsKey("trainingObjective")) { sql.append(", training_objective = ?"); params.add(data.get("trainingObjective")); }
        if (data.containsKey("graduationRequirement")) { sql.append(", graduation_requirement = ?"); params.add(data.get("graduationRequirement")); }

        sql.append(" WHERE id = ? AND deleted = 0");
        params.add(id);

        jdbc.update(sql.toString(), params.toArray());
        return Result.success();
    }

    // ==================== 删除培养方案(软删除) ====================

    @DeleteMapping("/curriculum-plans/{id}")
    public Result<Void> deletePlan(@PathVariable Long id) {
        jdbc.update("UPDATE curriculum_plans SET deleted = 1 WHERE id = ?", id);
        return Result.success();
    }

    // ==================== 发布方案 ====================

    @PostMapping("/curriculum-plans/{id}/publish")
    public Result<Void> publishPlan(@PathVariable Long id) {
        jdbc.update(
            "UPDATE curriculum_plans SET status = 1, published_at = NOW(), updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0", id
        );
        return Result.success();
    }

    // ==================== 归档方案 ====================

    @PostMapping("/curriculum-plans/{id}/deprecate")
    public Result<Void> deprecatePlan(@PathVariable Long id) {
        jdbc.update(
            "UPDATE curriculum_plans SET status = 2, updated_at = NOW() WHERE id = ? AND deleted = 0", id
        );
        return Result.success();
    }

    // ==================== 方案课程列表 ====================

    @GetMapping("/curriculum-plans/{planId}/courses")
    public Result<List<Map<String, Object>>> listPlanCourses(@PathVariable Long planId) {
        List<Map<String, Object>> courses = jdbc.queryForList(
            "SELECT pc.id, pc.plan_id AS planId, pc.course_id AS courseId, " +
            "pc.semester_number AS semesterNumber, pc.course_category AS courseCategory, " +
            "pc.course_type AS courseType, pc.credits, pc.total_hours AS totalHours, " +
            "pc.weekly_hours AS weeklyHours, pc.exam_type AS examType, " +
            "pc.sort_order AS sortOrder, pc.remark, " +
            "pc.created_at AS createdAt, pc.updated_at AS updatedAt, " +
            "c.course_code AS courseCode, c.course_name AS courseName " +
            "FROM curriculum_plan_courses pc " +
            "LEFT JOIN courses c ON pc.course_id = c.id AND c.deleted = 0 " +
            "WHERE pc.plan_id = ? " +
            "ORDER BY pc.semester_number, pc.sort_order, pc.id",
            planId
        );
        return Result.success(courses);
    }

    // ==================== 添加方案课程 ====================

    @PostMapping("/curriculum-plans/{planId}/courses")
    public Result<Map<String, Object>> addPlanCourse(@PathVariable Long planId, @RequestBody Map<String, Object> data) {
        Long courseId = ((Number) data.get("courseId")).longValue();
        Integer semesterNumber = data.get("semesterNumber") != null ? ((Number) data.get("semesterNumber")).intValue() : null;
        Integer courseCategory = data.get("courseCategory") != null ? ((Number) data.get("courseCategory")).intValue() : null;
        Integer courseType = data.get("courseType") != null ? ((Number) data.get("courseType")).intValue() : null;
        BigDecimal credits = data.get("credits") != null ? new BigDecimal(data.get("credits").toString()) : null;
        Integer totalHours = data.get("totalHours") != null ? ((Number) data.get("totalHours")).intValue() : null;
        Integer weeklyHours = data.get("weeklyHours") != null ? ((Number) data.get("weeklyHours")).intValue() : null;
        Integer examType = data.get("examType") != null ? ((Number) data.get("examType")).intValue() : null;
        Integer sortOrder = data.get("sortOrder") != null ? ((Number) data.get("sortOrder")).intValue() : 0;
        String remark = (String) data.get("remark");

        long id = System.currentTimeMillis();
        jdbc.update(
            "INSERT INTO curriculum_plan_courses (id, plan_id, course_id, semester_number, " +
            "course_category, course_type, credits, total_hours, weekly_hours, " +
            "exam_type, sort_order, remark) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id, planId, courseId, semesterNumber,
            courseCategory, courseType, credits, totalHours, weeklyHours,
            examType, sortOrder, remark
        );

        data.put("id", id);
        data.put("planId", planId);
        return Result.success(data);
    }

    // ==================== 更新方案课程 ====================

    @PutMapping("/curriculum-plans/{planId}/courses/{courseId}")
    public Result<Void> updatePlanCourse(@PathVariable Long planId, @PathVariable Long courseId,
                                         @RequestBody Map<String, Object> data) {
        StringBuilder sql = new StringBuilder("UPDATE curriculum_plan_courses SET updated_at = NOW()");
        List<Object> params = new ArrayList<>();

        if (data.containsKey("semesterNumber")) {
            Integer v = data.get("semesterNumber") != null ? ((Number) data.get("semesterNumber")).intValue() : null;
            sql.append(", semester_number = ?"); params.add(v);
        }
        if (data.containsKey("courseCategory")) {
            Integer v = data.get("courseCategory") != null ? ((Number) data.get("courseCategory")).intValue() : null;
            sql.append(", course_category = ?"); params.add(v);
        }
        if (data.containsKey("courseType")) {
            Integer v = data.get("courseType") != null ? ((Number) data.get("courseType")).intValue() : null;
            sql.append(", course_type = ?"); params.add(v);
        }
        if (data.containsKey("credits")) {
            BigDecimal v = data.get("credits") != null ? new BigDecimal(data.get("credits").toString()) : null;
            sql.append(", credits = ?"); params.add(v);
        }
        if (data.containsKey("totalHours")) {
            Integer v = data.get("totalHours") != null ? ((Number) data.get("totalHours")).intValue() : null;
            sql.append(", total_hours = ?"); params.add(v);
        }
        if (data.containsKey("weeklyHours")) {
            Integer v = data.get("weeklyHours") != null ? ((Number) data.get("weeklyHours")).intValue() : null;
            sql.append(", weekly_hours = ?"); params.add(v);
        }
        if (data.containsKey("examType")) {
            Integer v = data.get("examType") != null ? ((Number) data.get("examType")).intValue() : null;
            sql.append(", exam_type = ?"); params.add(v);
        }
        if (data.containsKey("sortOrder")) {
            Integer v = data.get("sortOrder") != null ? ((Number) data.get("sortOrder")).intValue() : null;
            sql.append(", sort_order = ?"); params.add(v);
        }
        if (data.containsKey("remark")) { sql.append(", remark = ?"); params.add(data.get("remark")); }

        sql.append(" WHERE id = ? AND plan_id = ?");
        params.add(courseId);
        params.add(planId);

        jdbc.update(sql.toString(), params.toArray());
        return Result.success();
    }

    // ==================== 删除方案课程 ====================

    @DeleteMapping("/curriculum-plans/{planId}/courses/{courseId}")
    public Result<Void> removePlanCourse(@PathVariable Long planId, @PathVariable Long courseId) {
        jdbc.update("DELETE FROM curriculum_plan_courses WHERE id = ? AND plan_id = ?", courseId, planId);
        return Result.success();
    }

    // ==================== 复制培养方案 ====================

    @PostMapping("/curriculum-plans/{id}/copy")
    public Result<Map<String, Object>> copyPlan(@PathVariable Long id) {
        // Get original plan
        Map<String, Object> original = jdbc.queryForMap(
            "SELECT * FROM curriculum_plans WHERE id = ? AND deleted = 0", id
        );

        // Get current max version for same plan_code
        String planCode = (String) original.get("plan_code");
        Integer maxVersion;
        try {
            maxVersion = jdbc.queryForObject(
                "SELECT MAX(version) FROM curriculum_plans WHERE plan_code = ? AND deleted = 0",
                Integer.class, planCode
            );
        } catch (Exception e) {
            maxVersion = null;
        }
        int newVersion = (maxVersion != null ? maxVersion : 1) + 1;

        // Create new plan
        long newId = System.currentTimeMillis();
        jdbc.update(
            "INSERT INTO curriculum_plans (id, plan_code, plan_name, major_id, major_direction_id, grade_year, " +
            "total_credits, required_credits, elective_credits, practice_credits, " +
            "training_objective, graduation_requirement, version, status, created_by, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, 0)",
            newId, planCode, original.get("plan_name"), original.get("major_id"),
            original.get("major_direction_id"), original.get("grade_year"),
            original.get("total_credits"), original.get("required_credits"),
            original.get("elective_credits"), original.get("practice_credits"),
            original.get("training_objective"), original.get("graduation_requirement"),
            newVersion, original.get("created_by")
        );

        // Copy all plan courses
        List<Map<String, Object>> courses = jdbc.queryForList(
            "SELECT * FROM curriculum_plan_courses WHERE plan_id = ?", id
        );
        for (Map<String, Object> course : courses) {
            long courseRecordId = System.currentTimeMillis() + new Random().nextInt(10000);
            jdbc.update(
                "INSERT INTO curriculum_plan_courses (id, plan_id, course_id, semester_number, " +
                "course_category, course_type, credits, total_hours, weekly_hours, " +
                "exam_type, sort_order, remark) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                courseRecordId, newId, course.get("course_id"), course.get("semester_number"),
                course.get("course_category"), course.get("course_type"), course.get("credits"),
                course.get("total_hours"), course.get("weekly_hours"),
                course.get("exam_type"), course.get("sort_order"), course.get("remark")
            );
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", newId);
        result.put("version", newVersion);
        result.put("copiedCourses", courses.size());
        return Result.success(result);
    }
}
