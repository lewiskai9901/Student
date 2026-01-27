package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.AcademicCalendarApplicationService;
import com.school.management.application.teaching.command.CreateAcademicYearCommand;
import com.school.management.application.teaching.command.UpdateAcademicYearCommand;
import com.school.management.application.teaching.query.AcademicYearDTO;
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
 * 学年管理控制器
 */
@Tag(name = "学年管理", description = "学年的增删改查")
@RestController
@RequestMapping("/api/v2/teaching/academic-years")
@RequiredArgsConstructor
public class AcademicYearController {

    private final AcademicCalendarApplicationService academicCalendarService;

    @Operation(summary = "获取学年列表")
    @GetMapping
    @PreAuthorize("hasAnyAuthority('teaching:calendar:view', 'teaching:calendar:manage')")
    public Result<List<AcademicYearDTO>> list() {
        return Result.success(academicCalendarService.listAcademicYears());
    }

    @Operation(summary = "获取学年详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('teaching:calendar:view', 'teaching:calendar:manage')")
    public Result<AcademicYearDTO> get(@PathVariable Long id) {
        return Result.success(academicCalendarService.getAcademicYear(id));
    }

    @Operation(summary = "获取当前学年")
    @GetMapping("/current")
    @PreAuthorize("hasAnyAuthority('teaching:calendar:view', 'teaching:calendar:manage')")
    public Result<AcademicYearDTO> getCurrent() {
        return Result.success(academicCalendarService.getCurrentAcademicYear());
    }

    @Operation(summary = "创建学年")
    @PostMapping
    @PreAuthorize("hasAuthority('teaching:calendar:manage')")
    public Result<Long> create(@RequestBody CreateAcademicYearRequest request,
                               @AuthenticationPrincipal CustomUserDetails user) {
        CreateAcademicYearCommand command = CreateAcademicYearCommand.builder()
                .yearCode(request.getYearCode())
                .yearName(request.getYearName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .operatorId(user.getUserId())
                .build();
        Long id = academicCalendarService.createAcademicYear(command);
        return Result.success(id);
    }

    @Operation(summary = "更新学年")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:calendar:manage')")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody UpdateAcademicYearRequest request,
                               @AuthenticationPrincipal CustomUserDetails user) {
        UpdateAcademicYearCommand command = UpdateAcademicYearCommand.builder()
                .id(id)
                .yearCode(request.getYearCode())
                .yearName(request.getYearName())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .operatorId(user.getUserId())
                .build();
        academicCalendarService.updateAcademicYear(command);
        return Result.success();
    }

    @Operation(summary = "设为当前学年")
    @PostMapping("/{id}/set-current")
    @PreAuthorize("hasAuthority('teaching:calendar:manage')")
    public Result<Void> setCurrent(@PathVariable Long id) {
        academicCalendarService.setCurrentAcademicYear(id);
        return Result.success();
    }

    @Operation(summary = "删除学年")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:calendar:manage')")
    public Result<Void> delete(@PathVariable Long id) {
        academicCalendarService.deleteAcademicYear(id);
        return Result.success();
    }
}
