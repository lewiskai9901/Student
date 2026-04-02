package com.school.management.interfaces.rest.teaching;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import com.school.management.infrastructure.casbin.CasbinAccess;

/**
 * 教学任务管理 REST Controller
 * 处理教学任务及教学任务教师分配的 CRUD
 *
 * 使用 JdbcTemplate 直接操作已有的 DB 表:
 * - teaching_tasks
 * - teaching_task_teachers
 */
@Slf4j
@RestController
@RequestMapping("/teaching")
@RequiredArgsConstructor
public class TeachingTaskMgmtController {

    private final JdbcTemplate jdbc;

    // ==================== 教学任务 ====================

    @GetMapping("/tasks")
    @CasbinAccess(resource = "teaching:task", action = "view")
    public Result<Map<String, Object>> listTasks(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        StringBuilder where = new StringBuilder(" WHERE t.deleted = 0");
        List<Object> params = new ArrayList<>();
        if (semesterId != null) {
            where.append(" AND t.semester_id = ?");
            params.add(semesterId);
        }
        if (status != null) {
            where.append(" AND t.task_status = ?");
            params.add(status);
        }

        // count
        String countSql = "SELECT COUNT(*) FROM teaching_tasks t" + where;
        Long total = jdbc.queryForObject(countSql, Long.class, params.toArray());

        // records
        String sql = "SELECT t.id, t.task_code AS taskCode, t.semester_id AS semesterId, " +
            "t.course_id AS courseId, t.class_id AS classId, t.org_unit_id AS orgUnitId, " +
            "t.student_count AS studentCount, t.weekly_hours AS weeklyHours, " +
            "t.total_hours AS totalHours, t.start_week AS startWeek, t.end_week AS endWeek, " +
            "t.scheduling_status AS schedulingStatus, t.task_status AS taskStatus, " +
            "t.remark, t.created_at AS createdAt, t.updated_at AS updatedAt, " +
            "c.course_name AS courseName, cls.class_name AS className " +
            "FROM teaching_tasks t " +
            "LEFT JOIN courses c ON c.id = t.course_id " +
            "LEFT JOIN classes cls ON cls.id = t.class_id" +
            where +
            " ORDER BY t.created_at DESC LIMIT ? OFFSET ?";
        params.add(size);
        params.add((page - 1) * size);

        List<Map<String, Object>> records = jdbc.queryForList(sql, params.toArray());

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return Result.success(result);
    }

    @GetMapping("/tasks/{id}")
    @CasbinAccess(resource = "teaching:task", action = "view")
    public Result<Map<String, Object>> getTask(@PathVariable Long id) {
        Map<String, Object> task = jdbc.queryForMap(
            "SELECT t.id, t.task_code AS taskCode, t.semester_id AS semesterId, " +
            "t.course_id AS courseId, t.class_id AS classId, t.org_unit_id AS orgUnitId, " +
            "t.student_count AS studentCount, t.weekly_hours AS weeklyHours, " +
            "t.total_hours AS totalHours, t.start_week AS startWeek, t.end_week AS endWeek, " +
            "t.scheduling_status AS schedulingStatus, t.task_status AS taskStatus, " +
            "t.remark, t.created_at AS createdAt, t.updated_at AS updatedAt, " +
            "c.course_name AS courseName, cls.class_name AS className " +
            "FROM teaching_tasks t " +
            "LEFT JOIN courses c ON c.id = t.course_id " +
            "LEFT JOIN classes cls ON cls.id = t.class_id " +
            "WHERE t.id = ? AND t.deleted = 0", id
        );
        return Result.success(task);
    }

    @PostMapping("/tasks")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Map<String, Object>> createTask(@RequestBody Map<String, Object> data) {
        long id = IdWorker.getId();
        String taskCode = "TT" + id;
        Long semesterId = data.get("semesterId") != null ? ((Number) data.get("semesterId")).longValue() : null;
        Long courseId = data.get("courseId") != null ? ((Number) data.get("courseId")).longValue() : null;
        Long classId = data.get("classId") != null ? ((Number) data.get("classId")).longValue() : null;
        Long orgUnitId = data.get("orgUnitId") != null ? ((Number) data.get("orgUnitId")).longValue() : null;
        Integer studentCount = data.get("studentCount") != null ? ((Number) data.get("studentCount")).intValue() : 0;
        Integer weeklyHours = data.get("weeklyHours") != null ? ((Number) data.get("weeklyHours")).intValue() : null;
        Integer totalHours = data.get("totalHours") != null ? ((Number) data.get("totalHours")).intValue() : null;
        Integer startWeek = data.get("startWeek") != null ? ((Number) data.get("startWeek")).intValue() : 1;
        Integer endWeek = data.get("endWeek") != null ? ((Number) data.get("endWeek")).intValue() : 16;
        Integer taskStatus = data.get("taskStatus") != null ? ((Number) data.get("taskStatus")).intValue() : 1;
        String remark = (String) data.get("remark");

        jdbc.update(
            "INSERT INTO teaching_tasks (id, task_code, semester_id, course_id, class_id, org_unit_id, " +
            "student_count, weekly_hours, total_hours, start_week, end_week, " +
            "scheduling_status, task_status, remark, created_by, created_at, updated_at, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, NOW(), NOW(), 0)",
            id, taskCode, semesterId, courseId, classId, orgUnitId,
            studentCount, weeklyHours, totalHours, startWeek, endWeek,
            taskStatus, remark, SecurityUtils.requireCurrentUserId()
        );

        data.put("id", id);
        data.put("taskCode", taskCode);
        return Result.success(data);
    }

    @PutMapping("/tasks/{id}")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Void> updateTask(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Long courseId = data.get("courseId") != null ? ((Number) data.get("courseId")).longValue() : null;
        Long classId = data.get("classId") != null ? ((Number) data.get("classId")).longValue() : null;
        Long orgUnitId = data.get("orgUnitId") != null ? ((Number) data.get("orgUnitId")).longValue() : null;
        Integer studentCount = data.get("studentCount") != null ? ((Number) data.get("studentCount")).intValue() : null;
        Integer weeklyHours = data.get("weeklyHours") != null ? ((Number) data.get("weeklyHours")).intValue() : null;
        Integer totalHours = data.get("totalHours") != null ? ((Number) data.get("totalHours")).intValue() : null;
        Integer startWeek = data.get("startWeek") != null ? ((Number) data.get("startWeek")).intValue() : null;
        Integer endWeek = data.get("endWeek") != null ? ((Number) data.get("endWeek")).intValue() : null;
        String remark = (String) data.get("remark");

        jdbc.update(
            "UPDATE teaching_tasks SET course_id = ?, class_id = ?, org_unit_id = ?, " +
            "student_count = ?, weekly_hours = ?, total_hours = ?, " +
            "start_week = ?, end_week = ?, remark = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            courseId, classId, orgUnitId, studentCount, weeklyHours, totalHours,
            startWeek, endWeek, remark, id
        );
        return Result.success();
    }

    @DeleteMapping("/tasks/{id}")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Void> deleteTask(@PathVariable Long id) {
        jdbc.update("UPDATE teaching_tasks SET deleted = 1 WHERE id = ?", id);
        return Result.success();
    }

    // ==================== 教师分配 ====================

    @PostMapping("/tasks/{id}/assign-teachers")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    @Transactional
    public Result<Void> assignTeachers(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Number> teacherIds = (List<Number>) data.get("teacherIds");
        Number mainTeacherId = (Number) data.get("mainTeacherId");

        // Delete existing teacher assignments
        jdbc.update("DELETE FROM teaching_task_teachers WHERE task_id = ?", id);

        // Insert new assignments
        if (teacherIds != null) {
            for (Number tid : teacherIds) {
                long teacherId = tid.longValue();
                int teacherRole = (mainTeacherId != null && teacherId == mainTeacherId.longValue()) ? 1 : 2;
                jdbc.update(
                    "INSERT INTO teaching_task_teachers (id, task_id, teacher_id, teacher_role, workload_ratio, created_at) " +
                    "VALUES (?, ?, ?, ?, 1.00, NOW())",
                    IdWorker.getId(), id, teacherId, teacherRole
                );
            }
        }
        return Result.success();
    }

    @DeleteMapping("/tasks/{id}/teachers/{teacherId}")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Void> removeTeacher(@PathVariable Long id, @PathVariable Long teacherId) {
        jdbc.update("DELETE FROM teaching_task_teachers WHERE task_id = ? AND teacher_id = ?", id, teacherId);
        return Result.success();
    }

    // ==================== 批量创建 ====================

    @PostMapping("/tasks/batch-create")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    @Transactional
    public Result<List<Map<String, Object>>> batchCreateTasks(@RequestBody Map<String, Object> data) {
        Long semesterId = data.get("semesterId") != null ? ((Number) data.get("semesterId")).longValue() : null;
        Long planId = data.get("planId") != null ? ((Number) data.get("planId")).longValue() : null;
        @SuppressWarnings("unchecked")
        List<Number> classIds = (List<Number>) data.get("classIds");

        if (semesterId == null || planId == null || classIds == null || classIds.isEmpty()) {
            return Result.error("semesterId, planId, classIds are required");
        }

        // Get courses from curriculum plan
        List<Map<String, Object>> planCourses = jdbc.queryForList(
            "SELECT course_id, weekly_hours, total_hours, start_week, end_week " +
            "FROM curriculum_plan_courses WHERE plan_id = ? AND deleted = 0", planId
        );

        List<Map<String, Object>> createdTasks = new ArrayList<>();

        for (Number clsId : classIds) {
            long classId = clsId.longValue();
            for (Map<String, Object> pc : planCourses) {
                long taskId = IdWorker.getId();
                String taskCode = "TT" + taskId;
                Long courseId = ((Number) pc.get("course_id")).longValue();
                Integer weeklyHours = pc.get("weekly_hours") != null ? ((Number) pc.get("weekly_hours")).intValue() : null;
                Integer totalHours = pc.get("total_hours") != null ? ((Number) pc.get("total_hours")).intValue() : null;
                Integer startWeek = pc.get("start_week") != null ? ((Number) pc.get("start_week")).intValue() : 1;
                Integer endWeek = pc.get("end_week") != null ? ((Number) pc.get("end_week")).intValue() : 16;

                jdbc.update(
                    "INSERT INTO teaching_tasks (id, task_code, semester_id, course_id, class_id, " +
                    "student_count, weekly_hours, total_hours, start_week, end_week, " +
                    "scheduling_status, task_status, created_by, created_at, updated_at, deleted) " +
                    "VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?, ?, 0, 1, ?, NOW(), NOW(), 0)",
                    taskId, taskCode, semesterId, courseId, classId,
                    weeklyHours, totalHours, startWeek, endWeek,
                    SecurityUtils.requireCurrentUserId()
                );

                Map<String, Object> task = new HashMap<>();
                task.put("id", taskId);
                task.put("taskCode", taskCode);
                task.put("semesterId", semesterId);
                task.put("courseId", courseId);
                task.put("classId", classId);
                createdTasks.add(task);
            }
        }

        return Result.success(createdTasks);
    }

    // ==================== 状态变更 ====================

    @PatchMapping("/tasks/{id}/status")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Void> updateTaskStatus(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Integer taskStatus = data.get("taskStatus") != null ? ((Number) data.get("taskStatus")).intValue() : null;
        if (taskStatus == null) {
            return Result.error("taskStatus is required");
        }
        jdbc.update(
            "UPDATE teaching_tasks SET task_status = ?, updated_at = NOW() WHERE id = ? AND deleted = 0",
            taskStatus, id
        );
        return Result.success();
    }
}
