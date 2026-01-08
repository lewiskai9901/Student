package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.dto.BuildingDormitoryDTO;
import com.school.management.entity.BuildingDormitory;
import com.school.management.service.BuildingDormitoryService;
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
 * 宿舍楼管理控制器
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/dormitory/buildings")
@RequiredArgsConstructor
@Tag(name = "宿舍楼管理", description = "宿舍楼管理相关接口")
public class BuildingDormitoryController {

    private final BuildingDormitoryService buildingDormitoryService;

    /**
     * 分页查询宿舍楼
     */
    @GetMapping
    @Operation(summary = "分页查询宿舍楼", description = "根据条件分页查询宿舍楼列表（带数据权限过滤）")
    @PreAuthorize("hasAnyAuthority('system:dormitory_building:view', 'student:dormitory:view')")
    public Result<PageResult<BuildingDormitory>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "楼宇名称") @RequestParam(required = false) String buildingName,
            @Parameter(description = "宿舍类型") @RequestParam(required = false) Integer dormitoryType) {
        IPage<BuildingDormitory> page = buildingDormitoryService.page(pageNum, pageSize, buildingName, dormitoryType);
        return Result.success(PageResult.from(page));
    }

    /**
     * 获取宿舍楼详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取宿舍楼详情", description = "根据ID获取宿舍楼详细信息")
    @PreAuthorize("hasAuthority('system:dormitory_building:view')")
    public Result<BuildingDormitory> getById(@Parameter(description = "宿舍楼ID") @PathVariable Long id) {
        return Result.success(buildingDormitoryService.getById(id));
    }

    /**
     * 根据楼宇ID获取宿舍楼详情
     */
    @GetMapping("/building/{buildingId}")
    @Operation(summary = "根据楼宇ID获取宿舍楼", description = "根据楼宇ID获取宿舍楼详细信息")
    @PreAuthorize("hasAuthority('system:dormitory_building:view')")
    public Result<BuildingDormitory> getByBuildingId(@Parameter(description = "楼宇ID") @PathVariable Long buildingId) {
        return Result.success(buildingDormitoryService.getByBuildingId(buildingId));
    }

    /**
     * 更新宿舍楼信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新宿舍楼", description = "更新宿舍楼基本信息（仅管理员可操作）")
    @PreAuthorize("hasAuthority('system:dormitory_building:edit')")
    public Result<BuildingDormitory> update(
            @Parameter(description = "宿舍楼ID") @PathVariable Long id,
            @Valid @RequestBody BuildingDormitoryDTO dto) {
        return Result.success(buildingDormitoryService.update(id, dto));
    }

    /**
     * 分配管理员
     */
    @PostMapping("/{buildingId}/managers")
    @Operation(summary = "分配管理员", description = "为宿舍楼分配管理员（最多5个）")
    @PreAuthorize("hasAuthority('system:dormitory_building:assign_manager')")
    public Result<Void> assignManagers(
            @Parameter(description = "楼宇ID") @PathVariable Long buildingId,
            @RequestBody List<Long> managerIds) {
        buildingDormitoryService.assignManagers(buildingId, managerIds);
        return Result.success();
    }

    /**
     * 移除管理员
     */
    @DeleteMapping("/{buildingId}/managers/{userId}")
    @Operation(summary = "移除管理员", description = "移除宿舍楼的指定管理员")
    @PreAuthorize("hasAuthority('system:dormitory_building:assign_manager')")
    public Result<Void> removeManager(
            @Parameter(description = "楼宇ID") @PathVariable Long buildingId,
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        buildingDormitoryService.removeManager(buildingId, userId);
        return Result.success();
    }

    /**
     * 检查用户是否有宿舍管理权限
     */
    @GetMapping("/check-permission/{userId}")
    @Operation(summary = "检查宿舍管理权限", description = "检查用户是否具备宿舍管理权限")
    @PreAuthorize("hasAuthority('system:dormitory_building:assign_manager')")
    public Result<Boolean> hasDormitoryPermission(@Parameter(description = "用户ID") @PathVariable Long userId) {
        return Result.success(buildingDormitoryService.hasDormitoryPermission(userId));
    }
}
