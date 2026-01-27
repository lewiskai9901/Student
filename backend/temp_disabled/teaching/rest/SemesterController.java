package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.AcademicCalendarApplicationService;
import com.school.management.application.teaching.command.CreateSemesterCommand;
import com.school.management.application.teaching.command.UpdateSemesterCommand;
import com.school.management.application.teaching.query.SemesterDTO;
import com.school.management.application.teaching.query.TeachingWeekDTO;
import com.school.management.common.result.Result;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学期管理控制器
 */
@Tag(name = "学期管理", description = "学期和教学周管理")
@RestController
@RequestMapping("/api/v2/teaching/semesters")
@RequiredArgsConstructor
public class SemesterController {

    private final AcademicCalendarApplicationService academicCalendarService;

    @Operation(summary = "获取所有学期列表")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('teaching:calendar:view', 'teaching:calendar:manage')")
    public Result<List<SemesterDTO>> list() {
        return Result.success(academicCalendarService.listAllSemesters());
    }

    @Operation(summary = "获取学年下的学期列表")
    @GetMapping("/by-year/{academicYearId}")
    @PreAuthorize("hasAnyAuthority('teaching:calendar:view', 'teaching:calendar:manage')")
    public Result<List<SemesterDTO>> listByYear(@PathVariable Long academicYearId) {
        return Result.success(academicCalendarService.listSemestersByAcademicYear(academicYearId));
    }

    @Operation(summary = "获取学期详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('teaching:calendar:view', 'teaching:calendar:manage')")
    public Result<SemesterDTO> get(@PathVariable Long id) {
        return Result.success(academicCalendarService.getSemester(id));
    }

    @Operation(summary = "获取当前学期")
    @GetMapping("/current")
    @PreAuthorize("hasAnyAuthority('teaching:calendar:view', 'teaching:calendar:manage')")
    public Result<SemesterDTO> getCurrent() {
        return Result.success(academicCalendarService.getCurrentSemester());
    }

    @Operation(summary = "创建学期")
    @PostMapping
    @PreAuthorize("hasAuthority('teaching:calendar:manage')")
    public Result<Long> create(@RequestBody CreateSemesterRequest request,
                               @AuthenticationPrincipal CustomUserDetails user) {
        CreateSemesterCommand command = CreateSemesterCommand.builder()
                .academicYearId(request.getAcademicYearId())
                .semesterCode(request.getSemesterCode())
                .semesterName(request.getSemesterName())
                .semesterType(request.getSemesterType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .teachingStartDate(request.getTeachingStartDate())
                .teachingEndDate(request.getTeachingEndDate())
                .examStartDate(request.getExamStartDate())
                .examEndDate(request.getExamEndDate())
                .totalTeachingWeeks(request.getTotalTeachingWeeks())
                .operatorId(user.getUserId())
                .build();
        Long id = academicCalendarService.createSemester(command);
        return Result.success(id);
    }

    @Operation(summary = "更新学期")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:calendar:manage')")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody UpdateSemesterRequest request,
                               @AuthenticationPrincipal CustomUserDetails user) {
        UpdateSemesterCommand command = UpdateSemesterCommand.builder()
                .id(id)
                .semesterName(request.getSemesterName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .teachingStartDate(request.getTeachingStartDate())
                .teachingEndDate(request.getTeachingEndDate())
                .examStartDate(request.getExamStartDate())
                .examEndDate(request.getExamEndDate())
                .totalTeachingWeeks(request.getTotalTeachingWeeks())
                .operatorId(user.getUserId())
                .build();
        academicCalendarService.updateSemester(command);
        return Result.success();
    }

    @Operation(summary = "设为当前学期")
    @PostMapping("/{id}/set-current")
    @PreAuthorize("hasAuthority('teaching:calendar:manage')")
    public Result<Void> setCurrent(@PathVariable Long id) {
        academicCalendarService.setCurrentSemester(id);
        return Result.success();
    }

    @Operation(summary = "删除学期")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:calendar:manage')")
    public Result<Void> delete(@PathVariable Long id) {
        academicCalendarService.deleteSemester(id);
        return Result.success();
    }

    // ==================== 教学周管理 ====================

    @Operation(summary = "获取学期的教学周列表")
    @GetMapping("/{id}/weeks")
    @PreAuthorize("hasAnyAuthority('teaching:calendar:view', 'teaching:calendar:manage')")
    public Result<List<TeachingWeekDTO>> getWeeks(@PathVariable Long id) {
        return Result.success(academicCalendarService.listTeachingWeeks(id));
    }

    @Operation(summary = "自动生成教学周")
    @PostMapping("/{id}/weeks/generate")
    @PreAuthorize("hasAuthority('teaching:calendar:manage')")
    public Result<List<TeachingWeekDTO>> generateWeeks(@PathVariable Long id) {
        return Result.success(academicCalendarService.generateTeachingWeeks(id));
    }
}
