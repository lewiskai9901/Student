package com.school.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.Result;
import com.school.management.dto.inspector.InspectorCreateRequest;
import com.school.management.dto.inspector.InspectorDTO;
import com.school.management.dto.inspector.InspectorUpdateRequest;
import com.school.management.entity.CheckTaskAssignment;
import com.school.management.service.CheckPlanInspectorService;
import com.school.management.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 检查计划打分人员Controller
 */
@RestController
@RequestMapping("/check-plans/{planId}/inspectors")
@RequiredArgsConstructor
@Tag(name = "检查计划打分人员管理", description = "检查计划打分人员的增删改查")
public class CheckPlanInspectorController {

    private final CheckPlanInspectorService inspectorService;

    @GetMapping
    @Operation(summary = "获取打分人员列表")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    public Result<List<InspectorDTO>> getInspectors(
            @Parameter(description = "计划ID") @PathVariable Long planId) {
        return Result.success(inspectorService.getInspectorsByPlanId(planId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取打分人员详情")
    @PreAuthorize("hasAuthority('quantification:plan:view')")
    public Result<InspectorDTO> getInspectorDetail(
            @Parameter(description = "计划ID") @PathVariable Long planId,
            @Parameter(description = "打分人员ID") @PathVariable Long id) {
        return Result.success(inspectorService.getInspectorById(id));
    }

    @PostMapping
    @Operation(summary = "添加打分人员")
    @PreAuthorize("hasAuthority('quantification:plan:edit')")
    public Result<Long> addInspector(
            @Parameter(description = "计划ID") @PathVariable Long planId,
            @Valid @RequestBody InspectorCreateRequest request) {
        return Result.success(inspectorService.addInspector(planId, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新打分人员配置")
    @PreAuthorize("hasAuthority('quantification:plan:edit')")
    public Result<Void> updateInspector(
            @Parameter(description = "计划ID") @PathVariable Long planId,
            @Parameter(description = "打分人员ID") @PathVariable Long id,
            @Valid @RequestBody InspectorUpdateRequest request) {
        request.setId(id);
        inspectorService.updateInspector(planId, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除打分人员")
    @PreAuthorize("hasAuthority('quantification:plan:edit')")
    public Result<Void> deleteInspector(
            @Parameter(description = "计划ID") @PathVariable Long planId,
            @Parameter(description = "打分人员ID") @PathVariable Long id) {
        inspectorService.deleteInspector(planId, id);
        return Result.success();
    }
}
