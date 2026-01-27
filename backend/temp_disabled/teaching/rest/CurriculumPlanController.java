package com.school.management.interfaces.rest.teaching;

import com.school.management.application.teaching.CurriculumPlanApplicationService;
import com.school.management.application.teaching.command.CreateCurriculumPlanCommand;
import com.school.management.application.teaching.command.UpdateCurriculumPlanCommand;
import com.school.management.application.teaching.query.CurriculumPlanDTO;
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
 * 培养方案管理控制器
 */
@Tag(name = "培养方案管理", description = "培养方案的增删改查及版本管理")
@RestController
@RequestMapping("/api/v2/teaching/curriculum-plans")
@RequiredArgsConstructor
public class CurriculumPlanController {

    private final CurriculumPlanApplicationService planService;

    @Operation(summary = "创建培养方案")
    @PostMapping
    @PreAuthorize("hasAuthority('teaching:curriculum:create')")
    public Result<Long> createPlan(
            @Valid @RequestBody CreateCurriculumPlanRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        CreateCurriculumPlanCommand command = CreateCurriculumPlanCommand.builder()
                .planCode(request.getPlanCode())
                .planName(request.getPlanName())
                .majorId(request.getMajorId())
                .enrollYear(request.getEnrollYear())
                .duration(request.getDuration())
                .totalCredits(request.getTotalCredits())
                .requiredCredits(request.getRequiredCredits())
                .electiveCredits(request.getElectiveCredits())
                .practiceCredits(request.getPracticeCredits())
                .objectives(request.getObjectives())
                .requirements(request.getRequirements())
                .remark(request.getRemark())
                .courses(request.getCourses() != null ? request.getCourses().stream()
                        .map(c -> CreateCurriculumPlanCommand.PlanCourseItem.builder()
                                .courseId(c.getCourseId())
                                .semester(c.getSemester())
                                .weeklyHours(c.getWeeklyHours())
                                .examType(c.getExamType())
                                .isRequired(c.getIsRequired())
                                .build())
                        .collect(Collectors.toList()) : null)
                .operatorId(user.getId())
                .build();

        Long id = planService.createPlan(command);
        return Result.success(id);
    }

    @Operation(summary = "更新培养方案")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:curriculum:update')")
    public Result<Void> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCurriculumPlanRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        UpdateCurriculumPlanCommand command = UpdateCurriculumPlanCommand.builder()
                .id(id)
                .planName(request.getPlanName())
                .duration(request.getDuration())
                .totalCredits(request.getTotalCredits())
                .requiredCredits(request.getRequiredCredits())
                .electiveCredits(request.getElectiveCredits())
                .practiceCredits(request.getPracticeCredits())
                .objectives(request.getObjectives())
                .requirements(request.getRequirements())
                .remark(request.getRemark())
                .courses(request.getCourses() != null ? request.getCourses().stream()
                        .map(c -> UpdateCurriculumPlanCommand.PlanCourseItem.builder()
                                .courseId(c.getCourseId())
                                .semester(c.getSemester())
                                .weeklyHours(c.getWeeklyHours())
                                .examType(c.getExamType())
                                .isRequired(c.getIsRequired())
                                .build())
                        .collect(Collectors.toList()) : null)
                .status(request.getStatus())
                .operatorId(user.getId())
                .build();

        planService.updatePlan(command);
        return Result.success();
    }

    @Operation(summary = "发布培养方案")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('teaching:curriculum:publish')")
    public Result<Void> publishPlan(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        planService.publishPlan(id, user.getId());
        return Result.success();
    }

    @Operation(summary = "归档培养方案")
    @PostMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('teaching:curriculum:archive')")
    public Result<Void> archivePlan(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        planService.archivePlan(id, user.getId());
        return Result.success();
    }

    @Operation(summary = "创建新版本")
    @PostMapping("/{id}/new-version")
    @PreAuthorize("hasAuthority('teaching:curriculum:create')")
    public Result<Long> createNewVersion(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {
        Long newId = planService.createNewVersion(id, user.getId());
        return Result.success(newId);
    }

    @Operation(summary = "删除培养方案")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:curriculum:delete')")
    public Result<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return Result.success();
    }

    @Operation(summary = "获取培养方案详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('teaching:curriculum:view')")
    public Result<CurriculumPlanDTO> getPlan(@PathVariable Long id) {
        return Result.success(planService.getPlan(id));
    }

    @Operation(summary = "根据专业和年份获取方案")
    @GetMapping("/major/{majorId}/year/{enrollYear}")
    @PreAuthorize("hasAuthority('teaching:curriculum:view')")
    public Result<CurriculumPlanDTO> getPlanByMajorAndYear(
            @PathVariable Long majorId,
            @PathVariable Integer enrollYear) {
        return Result.success(planService.getPlanByMajorAndYear(majorId, enrollYear));
    }

    @Operation(summary = "获取所有培养方案")
    @GetMapping
    @PreAuthorize("hasAuthority('teaching:curriculum:view')")
    public Result<List<CurriculumPlanDTO>> getAllPlans() {
        return Result.success(planService.getAllPlans());
    }

    @Operation(summary = "根据专业获取方案列表")
    @GetMapping("/major/{majorId}")
    @PreAuthorize("hasAuthority('teaching:curriculum:view')")
    public Result<List<CurriculumPlanDTO>> getPlansByMajor(@PathVariable Long majorId) {
        return Result.success(planService.getPlansByMajor(majorId));
    }

    @Operation(summary = "分页查询培养方案")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('teaching:curriculum:view')")
    public Result<PageResult<CurriculumPlanDTO>> getPlansPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long majorId,
            @RequestParam(required = false) Integer enrollYear,
            @RequestParam(required = false) Integer status) {

        List<CurriculumPlanDTO> list = planService.getPlansPage(page, size, majorId, enrollYear, status);
        long total = planService.countPlans(majorId, enrollYear, status);

        return Result.success(new PageResult<>(list, total, page, size));
    }
}
