package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.TeachingTaskApplicationService;
import com.school.management.application.teaching.command.CreateTeachingTaskCommand;
import com.school.management.application.teaching.command.UpdateTeachingTaskCommand;
import com.school.management.application.teaching.query.TeachingTaskDTO;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 教学任务管理控制器
 */
@Tag(name = "教学任务管理", description = "教学任务的分配和管理")
@RestController
@RequestMapping("/api/v2/teaching/tasks")
@RequiredArgsConstructor
public class TeachingTaskController {

    private final TeachingTaskApplicationService taskService;

    @Operation(summary = "创建教学任务")
    @PostMapping
    @PreAuthorize("hasAuthority('teaching:task:create')")
    public Result<Long> createTask(
            @Valid @RequestBody CreateTeachingTaskRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        CreateTeachingTaskCommand command = CreateTeachingTaskCommand.builder()
                .semesterId(request.getSemesterId())
                .courseId(request.getCourseId())
                .classId(request.getClassId())
                .classroomId(request.getClassroomId())
                .weeklyHours(request.getWeeklyHours())
                .startWeek(request.getStartWeek())
                .endWeek(request.getEndWeek())
                .examType(request.getExamType())
                .remark(request.getRemark())
                .teachers(request.getTeachers() != null ? request.getTeachers().stream()
                        .map(t -> CreateTeachingTaskCommand.TaskTeacherItem.builder()
                                .teacherId(t.getTeacherId())
                                .isMain(t.getIsMain())
                                .teachingContent(t.getTeachingContent())
                                .build())
                        .collect(Collectors.toList()) : null)
                .operatorId(user.getId())
                .build();

        Long id = taskService.createTask(command);
        return Result.success(id);
    }

    @Operation(summary = "更新教学任务")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:task:update')")
    public Result<Void> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTeachingTaskRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        UpdateTeachingTaskCommand command = UpdateTeachingTaskCommand.builder()
                .id(id)
                .classroomId(request.getClassroomId())
                .weeklyHours(request.getWeeklyHours())
                .startWeek(request.getStartWeek())
                .endWeek(request.getEndWeek())
                .examType(request.getExamType())
                .remark(request.getRemark())
                .status(request.getStatus())
                .teachers(request.getTeachers() != null ? request.getTeachers().stream()
                        .map(t -> UpdateTeachingTaskCommand.TaskTeacherItem.builder()
                                .teacherId(t.getTeacherId())
                                .isMain(t.getIsMain())
                                .teachingContent(t.getTeachingContent())
                                .build())
                        .collect(Collectors.toList()) : null)
                .operatorId(user.getId())
                .build();

        taskService.updateTask(command);
        return Result.success();
    }

    @Operation(summary = "分配教师")
    @PostMapping("/{id}/assign-teachers")
    @PreAuthorize("hasAuthority('teaching:task:assign')")
    public Result<Void> assignTeachers(
            @PathVariable Long id,
            @Valid @RequestBody AssignTeachersRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        List<CreateTeachingTaskCommand.TaskTeacherItem> teachers = request.getTeachers().stream()
                .map(t -> CreateTeachingTaskCommand.TaskTeacherItem.builder()
                        .teacherId(t.getTeacherId())
                        .isMain(t.getIsMain())
                        .teachingContent(t.getTeachingContent())
                        .build())
                .collect(Collectors.toList());

        taskService.assignTeachers(id, teachers, user.getId());
        return Result.success();
    }

    @Operation(summary = "开始教学任务")
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAuthority('teaching:task:update')")
    public Result<Void> startTask(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        taskService.startTask(id, user.getId());
        return Result.success();
    }

    @Operation(summary = "完成教学任务")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('teaching:task:update')")
    public Result<Void> completeTask(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        taskService.completeTask(id, user.getId());
        return Result.success();
    }

    @Operation(summary = "删除教学任务")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:task:delete')")
    public Result<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return Result.success();
    }

    @Operation(summary = "获取教学任务详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:task:view')")
    public Result<TeachingTaskDTO> getTask(@PathVariable Long id) {
        return Result.success(taskService.getTask(id));
    }

    @Operation(summary = "根据学期获取教学任务列表")
    @GetMapping("/semester/{semesterId}")
    @PreAuthorize("hasAuthority('teaching:task:view')")
    public Result<List<TeachingTaskDTO>> getTasksBySemester(@PathVariable Long semesterId) {
        return Result.success(taskService.getTasksBySemester(semesterId));
    }

    @Operation(summary = "根据课程获取教学任务列表")
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAuthority('teaching:task:view')")
    public Result<List<TeachingTaskDTO>> getTasksByCourse(@PathVariable Long courseId) {
        return Result.success(taskService.getTasksByCourse(courseId));
    }

    @Operation(summary = "根据班级获取教学任务列表")
    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAuthority('teaching:task:view')")
    public Result<List<TeachingTaskDTO>> getTasksByClass(@PathVariable Long classId) {
        return Result.success(taskService.getTasksByClass(classId));
    }

    @Operation(summary = "根据教师获取教学任务列表")
    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAuthority('teaching:task:view')")
    public Result<List<TeachingTaskDTO>> getTasksByTeacher(@PathVariable Long teacherId) {
        return Result.success(taskService.getTasksByTeacher(teacherId));
    }

    @Operation(summary = "分页查询教学任务")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('teaching:task:view')")
    public Result<PageResult<TeachingTaskDTO>> getTasksPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Integer status) {

        List<TeachingTaskDTO> list = taskService.getTasksPage(page, size, semesterId, courseId, classId, status);
        long total = taskService.countTasks(semesterId, courseId, classId, status);

        return Result.success(new PageResult<>(list, total, page, size));
    }

    @Operation(summary = "批量导入教学任务")
    @PostMapping("/batch-import")
    @PreAuthorize("hasAuthority('teaching:task:create')")
    public Result<Integer> batchImport(
            @RequestParam Long semesterId,
            @RequestParam Long planId,
            @AuthenticationPrincipal CustomUserDetails user) {
        int count = taskService.batchImport(semesterId, planId, user.getId());
        return Result.success(count);
    }
}
