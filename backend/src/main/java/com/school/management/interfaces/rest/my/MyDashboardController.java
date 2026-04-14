package com.school.management.interfaces.rest.my;

import com.school.management.application.my.MyDashboardQueryService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.access.model.PermissionScope;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 教师工作台 / 个人首页 (/my/dashboard)。
 *
 * <p>所有接口使用 SELF scope：数据过滤条件 user_id = currentUser 由接口内部硬编码，
 * 接口参数不得接收 teacherId/userId 等身份参数（由 ArchUnitMyEndpointTest 强制）。
 *
 * <p>设计文档: docs/plans/2026-04-14-my-dashboard-design.md
 */
@Slf4j
@RestController
@RequestMapping("/my")
@RequiredArgsConstructor
@Tag(name = "我的工作台", description = "教师 /my/dashboard 首页相关接口")
public class MyDashboardController {

    private final MyDashboardQueryService queryService;

    @GetMapping("/dashboard/summary")
    @Operation(summary = "首页头部统计汇总")
    @CasbinAccess(resource = "my:schedule", action = "view", scope = PermissionScope.SELF)
    public Result<DashboardSummaryDTO> getSummary() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.success(queryService.getSummary(userId));
    }

    @GetMapping("/schedule/today")
    @Operation(summary = "今日课表")
    @CasbinAccess(resource = "my:schedule", action = "view", scope = PermissionScope.SELF)
    public Result<List<TodayLessonDTO>> getTodaySchedule(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        Long userId = SecurityUtils.requireCurrentUserId();
        LocalDate target = date != null ? date : LocalDate.now();
        return Result.success(queryService.getTodaySchedule(userId, target));
    }

    @GetMapping("/classes")
    @Operation(summary = "我授课/班主任的班级列表")
    @CasbinAccess(resource = "my:students", action = "view", scope = PermissionScope.SELF)
    public Result<List<MyClassDTO>> getMyClasses() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.success(queryService.getMyClasses(userId));
    }

    @GetMapping("/tasks/substitute")
    @Operation(summary = "分配给我的代课请求")
    @CasbinAccess(resource = "my:substitute", action = "view", scope = PermissionScope.SELF)
    public Result<List<SubstituteTaskDTO>> getSubstituteTasks() {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.success(queryService.getSubstituteTasks(userId));
    }

    // ==================== DTOs (inner records) ====================

    public record DashboardSummaryDTO(
            int todayLessons,
            int weeklyHoursCurrent,
            int weeklyHoursTotal,
            int substituteRequests
    ) {}

    public record TodayLessonDTO(
            Long instanceId,
            Integer startSlot,
            Integer endSlot,
            String startTime,
            String endTime,
            String courseName,
            String className,
            String classroomName,
            Integer status,
            boolean canSign
    ) {}

    public record MyClassDTO(
            Long classId,
            String className,
            Integer studentCount,
            boolean isHeadTeacher,
            List<String> subjects
    ) {}

    public record SubstituteTaskDTO(
            Long taskId,
            String courseName,
            LocalDate scheduledDate,
            Integer startSlot,
            Integer endSlot,
            String requesterName,
            String requestedAt
    ) {
        public static List<SubstituteTaskDTO> empty() { return Collections.emptyList(); }
    }
}
