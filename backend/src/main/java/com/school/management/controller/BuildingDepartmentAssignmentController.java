package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.dto.request.BuildingDepartmentAssignmentCreateRequest;
import com.school.management.dto.request.BuildingDepartmentAssignmentUpdateRequest;
import com.school.management.entity.BuildingDepartmentAssignment;
import com.school.management.service.BuildingDepartmentAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 宿舍楼-院系分配控制器
 *
 * @author system
 * @version 3.1.0
 * @since 2024-12-07
 */
@Slf4j
@RestController
@RequestMapping("/dormitory/building-assignments")
@RequiredArgsConstructor
@Tag(name = "宿舍楼-院系分配管理", description = "宿舍楼与院系的分配关系管理接口")
public class BuildingDepartmentAssignmentController {

    private final BuildingDepartmentAssignmentService assignmentService;

    /**
     * 分页查询分配列表
     */
    @GetMapping
    @Operation(summary = "分页查询分配列表", description = "根据条件分页查询宿舍楼-院系分配列表")
    @PreAuthorize("hasAnyAuthority('system:dormitory_building:view', 'student:dormitory:view')")
    public Result<PageResult<BuildingDepartmentAssignment>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "宿舍楼ID") @RequestParam(required = false) Long buildingId,
            @Parameter(description = "院系ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        IPage<BuildingDepartmentAssignment> page = assignmentService.page(pageNum, pageSize, buildingId, departmentId, status);
        return Result.success(PageResult.from(page));
    }

    /**
     * 获取分配详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取分配详情", description = "根据ID获取分配详细信息")
    @PreAuthorize("hasAuthority('system:dormitory_building:view')")
    public Result<BuildingDepartmentAssignment> getById(
            @Parameter(description = "分配ID") @PathVariable Long id) {
        return Result.success(assignmentService.getById(id));
    }

    /**
     * 根据宿舍楼ID查询分配的院系
     */
    @GetMapping("/building/{buildingId}")
    @Operation(summary = "查询宿舍楼分配的院系", description = "根据宿舍楼ID查询所有分配的院系")
    @PreAuthorize("hasAnyAuthority('system:dormitory_building:view', 'student:dormitory:view')")
    public Result<List<BuildingDepartmentAssignment>> getByBuildingId(
            @Parameter(description = "宿舍楼ID") @PathVariable Long buildingId) {
        return Result.success(assignmentService.getByBuildingId(buildingId));
    }

    /**
     * 根据院系ID查询分配的宿舍楼
     */
    @GetMapping("/department/{departmentId}")
    @Operation(summary = "查询院系分配的宿舍楼", description = "根据院系ID查询所有分配的宿舍楼")
    @PreAuthorize("hasAnyAuthority('system:dormitory_building:view', 'student:dormitory:view')")
    public Result<List<BuildingDepartmentAssignment>> getByDepartmentId(
            @Parameter(description = "院系ID") @PathVariable Long departmentId) {
        return Result.success(assignmentService.getByDepartmentId(departmentId));
    }

    /**
     * 根据宿舍楼ID和楼层查询分配的院系
     */
    @GetMapping("/building/{buildingId}/floor/{floor}")
    @Operation(summary = "查询楼层分配的院系", description = "根据宿舍楼ID和楼层查询分配的院系")
    @PreAuthorize("hasAnyAuthority('system:dormitory_building:view', 'student:dormitory:view')")
    public Result<List<BuildingDepartmentAssignment>> getByBuildingAndFloor(
            @Parameter(description = "宿舍楼ID") @PathVariable Long buildingId,
            @Parameter(description = "楼层") @PathVariable Integer floor) {
        return Result.success(assignmentService.getByBuildingAndFloor(buildingId, floor));
    }

    /**
     * 创建分配
     */
    @PostMapping
    @Operation(summary = "创建分配", description = "创建宿舍楼-院系分配关系")
    @PreAuthorize("hasAuthority('system:dormitory_building:edit')")
    public Result<BuildingDepartmentAssignment> create(
            @Valid @RequestBody BuildingDepartmentAssignmentCreateRequest request) {
        return Result.success(assignmentService.create(request));
    }

    /**
     * 更新分配
     */
    @PutMapping
    @Operation(summary = "更新分配", description = "更新宿舍楼-院系分配信息")
    @PreAuthorize("hasAuthority('system:dormitory_building:edit')")
    public Result<BuildingDepartmentAssignment> update(
            @Valid @RequestBody BuildingDepartmentAssignmentUpdateRequest request) {
        return Result.success(assignmentService.update(request));
    }

    /**
     * 删除分配
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除分配", description = "删除宿舍楼-院系分配")
    @PreAuthorize("hasAuthority('system:dormitory_building:edit')")
    public Result<Void> delete(@Parameter(description = "分配ID") @PathVariable Long id) {
        assignmentService.delete(id);
        return Result.success();
    }

    /**
     * 批量删除分配
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除分配", description = "批量删除宿舍楼-院系分配")
    @PreAuthorize("hasAuthority('system:dormitory_building:edit')")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        assignmentService.batchDelete(ids);
        return Result.success();
    }

    /**
     * 启用分配
     */
    @PutMapping("/{id}/enable")
    @Operation(summary = "启用分配", description = "启用宿舍楼-院系分配")
    @PreAuthorize("hasAuthority('system:dormitory_building:edit')")
    public Result<Void> enable(@Parameter(description = "分配ID") @PathVariable Long id) {
        assignmentService.enable(id);
        return Result.success();
    }

    /**
     * 禁用分配
     */
    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用分配", description = "禁用宿舍楼-院系分配")
    @PreAuthorize("hasAuthority('system:dormitory_building:edit')")
    public Result<Void> disable(@Parameter(description = "分配ID") @PathVariable Long id) {
        assignmentService.disable(id);
        return Result.success();
    }

    /**
     * 检查楼层冲突
     */
    @GetMapping("/check-conflict")
    @Operation(summary = "检查楼层冲突", description = "检查指定楼层范围是否与现有分配冲突")
    @PreAuthorize("hasAuthority('system:dormitory_building:view')")
    public Result<Boolean> checkConflict(
            @Parameter(description = "宿舍楼ID") @RequestParam Long buildingId,
            @Parameter(description = "院系ID") @RequestParam Long departmentId,
            @Parameter(description = "起始楼层") @RequestParam(required = false) Integer floorStart,
            @Parameter(description = "结束楼层") @RequestParam(required = false) Integer floorEnd,
            @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        boolean hasConflict = assignmentService.hasFloorConflict(buildingId, departmentId, floorStart, floorEnd, excludeId);
        return Result.success(hasConflict);
    }
}
