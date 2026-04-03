package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.TeachingTaskApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教学任务管理 REST Controller (DDD)
 * 替代 TeachingTaskMgmtController
 */
@RestController
@RequestMapping("/teaching")
@RequiredArgsConstructor
public class TaskController {

    private final TeachingTaskApplicationService taskService;

    // ==================== 教学任务 CRUD ====================

    @GetMapping("/tasks")
    @CasbinAccess(resource = "teaching:task", action = "view")
    public Result<Map<String, Object>> listTasks(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(taskService.list(semesterId, status, page, size));
    }

    @GetMapping("/tasks/{id}")
    @CasbinAccess(resource = "teaching:task", action = "view")
    public Result<Map<String, Object>> getTask(@PathVariable Long id) {
        return Result.success(taskService.getById(id));
    }

    @PostMapping("/tasks")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Map<String, Object>> createTask(@RequestBody Map<String, Object> data) {
        return Result.success(taskService.create(data, SecurityUtils.requireCurrentUserId()));
    }

    @PutMapping("/tasks/{id}")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Void> updateTask(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        taskService.update(id, data);
        return Result.success();
    }

    @DeleteMapping("/tasks/{id}")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Void> deleteTask(@PathVariable Long id) {
        taskService.delete(id);
        return Result.success();
    }

    // ==================== 状态变更 ====================

    @PatchMapping("/tasks/{id}/status")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Void> updateTaskStatus(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        Integer taskStatus = data.get("taskStatus") != null
                ? ((Number) data.get("taskStatus")).intValue() : null;
        if (taskStatus == null) {
            return Result.error("taskStatus is required");
        }
        taskService.updateStatus(id, taskStatus);
        return Result.success();
    }

    // ==================== 教师分配 ====================

    @SuppressWarnings("unchecked")
    @PostMapping("/tasks/{id}/assign-teachers")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Void> assignTeachers(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        List<Number> teacherIds = (List<Number>) data.get("teacherIds");
        Number mainTeacherId = (Number) data.get("mainTeacherId");
        taskService.assignTeachers(id, teacherIds, mainTeacherId);
        return Result.success();
    }

    @DeleteMapping("/tasks/{id}/teachers/{teacherId}")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<Void> removeTeacher(@PathVariable Long id, @PathVariable Long teacherId) {
        taskService.removeTeacher(id, teacherId);
        return Result.success();
    }

    // ==================== 批量创建 ====================

    @SuppressWarnings("unchecked")
    @PostMapping("/tasks/batch-create")
    @CasbinAccess(resource = "teaching:task", action = "edit")
    public Result<List<Map<String, Object>>> batchCreateTasks(@RequestBody Map<String, Object> data) {
        Long semesterId = data.get("semesterId") != null ? ((Number) data.get("semesterId")).longValue() : null;
        Long planId = data.get("planId") != null ? ((Number) data.get("planId")).longValue() : null;
        List<Number> classIds = (List<Number>) data.get("classIds");

        if (semesterId == null || planId == null || classIds == null || classIds.isEmpty()) {
            return Result.error("semesterId, planId, classIds are required");
        }

        return Result.success(taskService.batchCreate(semesterId, planId, classIds,
                SecurityUtils.requireCurrentUserId()));
    }
}
