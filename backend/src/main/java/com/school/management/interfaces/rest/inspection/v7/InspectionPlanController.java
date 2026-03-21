package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.InspectionPlanApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.execution.InspTask;
import com.school.management.domain.inspection.model.v7.execution.InspectionPlan;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 检查计划控制器
 * 管理项目下的检查计划（排期），支持定期调度和手动触发。
 */
@Slf4j
@RestController
@RequestMapping("/v7/insp/plans")
@RequiredArgsConstructor
public class InspectionPlanController {

    private final InspectionPlanApplicationService planService;

    @PostMapping
    @CasbinAccess(resource = "insp:plan", action = "create")
    public Result<InspectionPlan> createPlan(@RequestBody CreatePlanRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(planService.createPlan(
                request.projectId(), request.planName(), request.rootSectionId(),
                request.sectionIds(),
                request.scheduleMode(), request.cycleType(), request.frequency(),
                request.scheduleDays(), request.timeSlots(), request.skipHolidays(),
                userId));
    }

    @GetMapping
    @CasbinAccess(resource = "insp:plan", action = "view")
    public Result<List<InspectionPlan>> listPlans(@RequestParam Long projectId) {
        return Result.success(planService.listPlans(projectId));
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:plan", action = "view")
    public Result<InspectionPlan> getPlan(@PathVariable Long id) {
        return Result.success(planService.getPlan(id)
                .orElseThrow(() -> new IllegalArgumentException("检查计划不存在: " + id)));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:plan", action = "edit")
    public Result<InspectionPlan> updatePlan(@PathVariable Long id,
                                              @RequestBody UpdatePlanRequest request) {
        return Result.success(planService.updatePlan(id,
                request.planName(), request.rootSectionId(), request.sectionIds(),
                request.scheduleMode(), request.cycleType(), request.frequency(),
                request.scheduleDays(), request.timeSlots(), request.skipHolidays()));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:plan", action = "delete")
    public Result<Void> deletePlan(@PathVariable Long id) {
        planService.deletePlan(id);
        return Result.success();
    }

    @PostMapping("/{id}/enable")
    @CasbinAccess(resource = "insp:plan", action = "edit")
    public Result<InspectionPlan> enablePlan(@PathVariable Long id) {
        return Result.success(planService.enablePlan(id));
    }

    @PostMapping("/{id}/disable")
    @CasbinAccess(resource = "insp:plan", action = "edit")
    public Result<InspectionPlan> disablePlan(@PathVariable Long id) {
        return Result.success(planService.disablePlan(id));
    }

    @PostMapping("/{id}/trigger")
    @CasbinAccess(resource = "insp:plan", action = "execute")
    public Result<InspTask> triggerOnDemandPlan(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(planService.triggerOnDemandPlan(id, userId));
    }

    // --- Request DTOs ---

    public record CreatePlanRequest(
            Long projectId,
            String planName,
            Long rootSectionId,    // V66: 该计划使用的模板（可选，null 则从项目继承）
            String sectionIds,
            String scheduleMode,
            String cycleType,
            Integer frequency,
            String scheduleDays,
            String timeSlots,
            Boolean skipHolidays
    ) {}

    public record UpdatePlanRequest(
            String planName,
            Long rootSectionId,    // V66: 可选，变更该计划绑定的模板
            String sectionIds,
            String scheduleMode,
            String cycleType,
            Integer frequency,
            String scheduleDays,
            String timeSlots,
            Boolean skipHolidays
    ) {}
}
