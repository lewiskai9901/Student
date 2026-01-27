package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.ScheduleApplicationService;
import com.school.management.application.teaching.command.AutoScheduleCommand;
import com.school.management.application.teaching.command.CreateScheduleEntryCommand;
import com.school.management.application.teaching.query.ScheduleConflictDTO;
import com.school.management.application.teaching.query.ScheduleEntryDTO;
import com.school.management.common.result.Result;
import com.school.management.domain.teaching.model.aggregate.CourseSchedule;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 排课管理控制器
 */
@Tag(name = "排课管理", description = "课表的创建、排课和查询")
@RestController
@RequestMapping("/api/v2/teaching/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleApplicationService scheduleService;

    @Operation(summary = "创建课表")
    @PostMapping
    @PreAuthorize("hasAuthority('teaching:schedule:create')")
    public Result<Long> createSchedule(
            @RequestParam Long semesterId,
            @RequestParam(required = false) String scheduleName,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long id = scheduleService.createSchedule(semesterId, scheduleName, user.getId());
        return Result.success(id);
    }

    @Operation(summary = "添加课表条目")
    @PostMapping("/entries")
    @PreAuthorize("hasAuthority('teaching:schedule:update')")
    public Result<Long> addScheduleEntry(
            @Valid @RequestBody CreateScheduleEntryRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        CreateScheduleEntryCommand command = CreateScheduleEntryCommand.builder()
                .taskId(request.getTaskId())
                .semesterId(request.getSemesterId())
                .weekday(request.getWeekday())
                .slot(request.getSlot())
                .startWeek(request.getStartWeek())
                .endWeek(request.getEndWeek())
                .weekType(request.getWeekType())
                .classroomId(request.getClassroomId())
                .operatorId(user.getId())
                .build();

        Long id = scheduleService.addScheduleEntry(command);
        return Result.success(id);
    }

    @Operation(summary = "删除课表条目")
    @DeleteMapping("/entries/{entryId}")
    @PreAuthorize("hasAuthority('teaching:schedule:update')")
    public Result<Void> deleteScheduleEntry(@PathVariable Long entryId) {
        scheduleService.deleteScheduleEntry(entryId);
        return Result.success();
    }

    @Operation(summary = "自动排课")
    @PostMapping("/auto-schedule")
    @PreAuthorize("hasAuthority('teaching:schedule:auto')")
    public Result<ScheduleApplicationService.AutoScheduleResult> autoSchedule(
            @Valid @RequestBody AutoScheduleRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        AutoScheduleCommand command = AutoScheduleCommand.builder()
                .semesterId(request.getSemesterId())
                .taskIds(request.getTaskIds())
                .maxIterations(request.getMaxIterations())
                .populationSize(request.getPopulationSize())
                .mutationRate(request.getMutationRate())
                .allowOverwrite(request.getAllowOverwrite())
                .operatorId(user.getId())
                .build();

        ScheduleApplicationService.AutoScheduleResult result = scheduleService.autoSchedule(command);
        return Result.success(result);
    }

    @Operation(summary = "检测冲突")
    @GetMapping("/detect-conflicts")
    @PreAuthorize("hasAuthority('teaching:schedule:view')")
    public Result<List<ScheduleConflictDTO>> detectConflicts(
            @RequestParam Long taskId,
            @RequestParam Integer weekday,
            @RequestParam Integer slot,
            @RequestParam(required = false) Integer startWeek,
            @RequestParam(required = false) Integer endWeek,
            @RequestParam(required = false) Integer weekType,
            @RequestParam(required = false) Long classroomId) {

        List<ScheduleConflictDTO> conflicts = scheduleService.detectConflicts(
                taskId, weekday, slot, startWeek, endWeek, weekType, classroomId
        );
        return Result.success(conflicts);
    }

    @Operation(summary = "发布课表")
    @PostMapping("/{scheduleId}/publish")
    @PreAuthorize("hasAuthority('teaching:schedule:publish')")
    public Result<Void> publishSchedule(
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal CustomUserDetails user) {
        scheduleService.publishSchedule(scheduleId, user.getId());
        return Result.success();
    }

    @Operation(summary = "撤销发布")
    @PostMapping("/{scheduleId}/unpublish")
    @PreAuthorize("hasAuthority('teaching:schedule:publish')")
    public Result<Void> unpublishSchedule(
            @PathVariable Long scheduleId,
            @AuthenticationPrincipal CustomUserDetails user) {
        scheduleService.unpublishSchedule(scheduleId, user.getId());
        return Result.success();
    }

    @Operation(summary = "获取课表详情")
    @GetMapping("/{scheduleId}")
    @PreAuthorize("hasAuthority('teaching:schedule:view')")
    public Result<CourseSchedule> getSchedule(@PathVariable Long scheduleId) {
        return Result.success(scheduleService.getSchedule(scheduleId));
    }

    @Operation(summary = "根据学期获取课表")
    @GetMapping("/semester/{semesterId}")
    @PreAuthorize("hasAuthority('teaching:schedule:view')")
    public Result<CourseSchedule> getScheduleBySemester(@PathVariable Long semesterId) {
        return Result.success(scheduleService.getScheduleBySemester(semesterId));
    }

    @Operation(summary = "获取课表条目列表")
    @GetMapping("/{scheduleId}/entries")
    @PreAuthorize("hasAuthority('teaching:schedule:view')")
    public Result<List<ScheduleEntryDTO>> getScheduleEntries(@PathVariable Long scheduleId) {
        return Result.success(scheduleService.getScheduleEntries(scheduleId));
    }

    @Operation(summary = "获取班级课表")
    @GetMapping("/class/{classId}")
    @PreAuthorize("hasAuthority('teaching:schedule:view')")
    public Result<List<ScheduleEntryDTO>> getClassSchedule(
            @RequestParam Long semesterId,
            @PathVariable Long classId) {
        return Result.success(scheduleService.getClassSchedule(semesterId, classId));
    }

    @Operation(summary = "获取教师课表")
    @GetMapping("/teacher/{teacherId}")
    @PreAuthorize("hasAuthority('teaching:schedule:view')")
    public Result<List<ScheduleEntryDTO>> getTeacherSchedule(
            @RequestParam Long semesterId,
            @PathVariable Long teacherId) {
        return Result.success(scheduleService.getTeacherSchedule(semesterId, teacherId));
    }

    @Operation(summary = "获取教室课表")
    @GetMapping("/classroom/{classroomId}")
    @PreAuthorize("hasAuthority('teaching:schedule:view')")
    public Result<List<ScheduleEntryDTO>> getClassroomSchedule(
            @RequestParam Long semesterId,
            @PathVariable Long classroomId) {
        return Result.success(scheduleService.getClassroomSchedule(semesterId, classroomId));
    }
}
