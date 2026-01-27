package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.AcademicCalendarApplicationService;
import com.school.management.application.teaching.command.CreateSchoolEventCommand;
import com.school.management.application.teaching.command.UpdateSchoolEventCommand;
import com.school.management.application.teaching.query.SchoolEventDTO;
import com.school.management.common.result.Result;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 校历事件控制器
 */
@Tag(name = "校历事件", description = "校历事件管理")
@RestController
@RequestMapping("/api/v2/teaching/school-events")
@RequiredArgsConstructor
public class SchoolEventController {

    private final AcademicCalendarApplicationService academicCalendarService;

    @Operation(summary = "获取日期范围内的事件")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('teaching:calendar:view', 'teaching:calendar:manage')")
    public Result<List<SchoolEventDTO>> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(academicCalendarService.listEventsByDateRange(startDate, endDate));
    }

    @Operation(summary = "获取学期的所有事件")
    @GetMapping("/by-semester/{semesterId}")
    @PreAuthorize("hasAnyAuthority('teaching:calendar:view', 'teaching:calendar:manage')")
    public Result<List<SchoolEventDTO>> listBySemester(@PathVariable Long semesterId) {
        return Result.success(academicCalendarService.listEventsBySemester(semesterId));
    }

    @Operation(summary = "获取事件详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('teaching:calendar:view', 'teaching:calendar:manage')")
    public Result<SchoolEventDTO> get(@PathVariable Long id) {
        return Result.success(academicCalendarService.getSchoolEvent(id));
    }

    @Operation(summary = "创建校历事件")
    @PostMapping
    @PreAuthorize("hasAuthority('teaching:calendar:manage')")
    public Result<Long> create(@RequestBody CreateSchoolEventRequest request,
                               @AuthenticationPrincipal CustomUserDetails user) {
        CreateSchoolEventCommand command = CreateSchoolEventCommand.builder()
                .semesterId(request.getSemesterId())
                .eventCode(request.getEventCode())
                .eventName(request.getEventName())
                .eventType(request.getEventType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .allDay(request.getAllDay())
                .affectSchedule(request.getAffectSchedule())
                .affectedOrgUnits(request.getAffectedOrgUnits())
                .swapToDate(request.getSwapToDate())
                .swapWeekday(request.getSwapWeekday())
                .color(request.getColor())
                .description(request.getDescription())
                .operatorId(user.getUserId())
                .build();
        Long id = academicCalendarService.createSchoolEvent(command);
        return Result.success(id);
    }

    @Operation(summary = "更新校历事件")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:calendar:manage')")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody UpdateSchoolEventRequest request,
                               @AuthenticationPrincipal CustomUserDetails user) {
        UpdateSchoolEventCommand command = UpdateSchoolEventCommand.builder()
                .id(id)
                .eventName(request.getEventName())
                .eventType(request.getEventType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .allDay(request.getAllDay())
                .affectSchedule(request.getAffectSchedule())
                .affectedOrgUnits(request.getAffectedOrgUnits())
                .swapToDate(request.getSwapToDate())
                .swapWeekday(request.getSwapWeekday())
                .color(request.getColor())
                .description(request.getDescription())
                .operatorId(user.getUserId())
                .build();
        academicCalendarService.updateSchoolEvent(command);
        return Result.success();
    }

    @Operation(summary = "删除校历事件")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:calendar:manage')")
    public Result<Void> delete(@PathVariable Long id) {
        academicCalendarService.deleteSchoolEvent(id);
        return Result.success();
    }
}
