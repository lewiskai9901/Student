package com.school.management.interfaces.rest.academic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.application.academic.CurriculumPlanApplicationService;
import com.school.management.application.academic.command.CreateCurriculumPlanCommand;
import com.school.management.application.academic.command.CreatePlanCourseCommand;
import com.school.management.application.academic.command.UpdateCurriculumPlanCommand;
import com.school.management.application.academic.command.UpdatePlanCourseCommand;
import com.school.management.application.academic.query.CurriculumPlanDTO;
import com.school.management.application.academic.query.PlanCourseDTO;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 培养方案管理 REST 控制器
 */
@Slf4j
@RequiredArgsConstructor
@Tag(name = "CurriculumPlans", description = "Curriculum plan management API")
@RestController
@RequestMapping("/academic/curriculum-plans")
public class CurriculumPlanController {

    private final CurriculumPlanApplicationService planService;

    // ======================== 方案 CRUD ========================

    @Operation(summary = "Get plan list (paginated)")
    @GetMapping
    @CasbinAccess(resource = "teaching:curriculum", action = "view")
    public Result<Page<CurriculumPlanDTO>> listPlans(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer gradeYear,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long majorId) {
        return Result.success(planService.getPlanList(gradeYear, status, majorId, pageNum, pageSize));
    }

    @Operation(summary = "Get plan by ID")
    @GetMapping("/{id}")
    @CasbinAccess(resource = "teaching:curriculum", action = "view")
    public Result<CurriculumPlanDTO> getPlan(@PathVariable Long id) {
        return Result.success(planService.getPlan(id));
    }

    @Operation(summary = "Create plan")
    @PostMapping
    @CasbinAccess(resource = "teaching:curriculum", action = "edit")
    public Result<CurriculumPlanDTO> createPlan(@RequestBody CreateCurriculumPlanCommand command) {
        command.setCreatedBy(SecurityUtils.requireCurrentUserId());
        return Result.success(planService.createPlan(command));
    }

    @Operation(summary = "Update plan")
    @PutMapping("/{id}")
    @CasbinAccess(resource = "teaching:curriculum", action = "edit")
    public Result<CurriculumPlanDTO> updatePlan(@PathVariable Long id,
                                                  @RequestBody UpdateCurriculumPlanCommand command) {
        command.setUpdatedBy(SecurityUtils.requireCurrentUserId());
        return Result.success(planService.updatePlan(id, command));
    }

    @Operation(summary = "Delete plan")
    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "teaching:curriculum", action = "edit")
    public Result<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return Result.success();
    }

    @Operation(summary = "Publish plan")
    @PostMapping("/{id}/publish")
    @CasbinAccess(resource = "teaching:curriculum", action = "edit")
    public Result<Void> publishPlan(@PathVariable Long id) {
        planService.publishPlan(id, SecurityUtils.requireCurrentUserId());
        return Result.success();
    }

    @Operation(summary = "Deprecate plan")
    @PostMapping("/{id}/deprecate")
    @CasbinAccess(resource = "teaching:curriculum", action = "edit")
    public Result<Void> deprecatePlan(@PathVariable Long id) {
        planService.deprecatePlan(id);
        return Result.success();
    }

    @Operation(summary = "Copy plan")
    @PostMapping("/{id}/copy")
    @CasbinAccess(resource = "teaching:curriculum", action = "edit")
    public Result<Map<String, Object>> copyPlan(@PathVariable Long id) {
        return Result.success(planService.copyPlan(id, SecurityUtils.requireCurrentUserId()));
    }

    // ======================== 方案课程 ========================

    @Operation(summary = "Get plan courses")
    @GetMapping("/{planId}/courses")
    @CasbinAccess(resource = "teaching:curriculum", action = "view")
    public Result<List<PlanCourseDTO>> listPlanCourses(@PathVariable Long planId) {
        return Result.success(planService.getPlanCourses(planId));
    }

    @Operation(summary = "Add plan course")
    @PostMapping("/{planId}/courses")
    @CasbinAccess(resource = "teaching:curriculum", action = "edit")
    public Result<PlanCourseDTO> addPlanCourse(@PathVariable Long planId,
                                                @RequestBody CreatePlanCourseCommand command) {
        return Result.success(planService.addPlanCourse(planId, command));
    }

    @Operation(summary = "Update plan course")
    @PutMapping("/{planId}/courses/{courseRecordId}")
    @CasbinAccess(resource = "teaching:curriculum", action = "edit")
    public Result<Void> updatePlanCourse(@PathVariable Long planId,
                                          @PathVariable Long courseRecordId,
                                          @RequestBody UpdatePlanCourseCommand command) {
        planService.updatePlanCourse(planId, courseRecordId, command);
        return Result.success();
    }

    @Operation(summary = "Remove plan course")
    @DeleteMapping("/{planId}/courses/{courseRecordId}")
    @CasbinAccess(resource = "teaching:curriculum", action = "edit")
    public Result<Void> removePlanCourse(@PathVariable Long planId,
                                          @PathVariable Long courseRecordId) {
        planService.removePlanCourse(planId, courseRecordId);
        return Result.success();
    }
}
