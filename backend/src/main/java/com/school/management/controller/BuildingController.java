package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.dto.BuildingRequest;
import com.school.management.entity.Building;
import com.school.management.service.BuildingService;
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
 * 楼宇管理控制器
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/teaching/buildings")
@RequiredArgsConstructor
@Tag(name = "楼宇管理", description = "楼宇管理相关接口")
public class BuildingController {

    private final BuildingService buildingService;

    /**
     * 分页查询楼宇
     */
    @GetMapping
    @Operation(summary = "分页查询楼宇", description = "根据条件分页查询楼宇")
    @PreAuthorize("hasAuthority('system:building:view')")
    public Result<PageResult<Building>> page(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "楼宇类型") @RequestParam(required = false) Integer buildingType,
            @Parameter(description = "楼号") @RequestParam(required = false) String buildingNo,
            @Parameter(description = "楼宇名称") @RequestParam(required = false) String buildingName,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        IPage<Building> page = buildingService.page(pageNum, pageSize, buildingType, buildingNo, buildingName, status);
        return Result.success(PageResult.from(page));
    }

    /**
     * 获取楼宇详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取楼宇详情", description = "根据ID获取楼宇详细信息")
    @PreAuthorize("hasAuthority('system:building:view')")
    public Result<Building> getById(@Parameter(description = "楼宇ID") @PathVariable Long id) {
        return Result.success(buildingService.getById(id));
    }

    /**
     * 创建楼宇
     */
    @PostMapping
    @Operation(summary = "创建楼宇", description = "创建新的楼宇")
    @PreAuthorize("hasAuthority('system:building:add')")
    public Result<Building> create(@Valid @RequestBody BuildingRequest request) {
        log.info("创建楼宇请求: {}", request.getBuildingNo());
        return Result.success(buildingService.create(request));
    }

    /**
     * 更新楼宇
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新楼宇", description = "更新指定楼宇信息")
    @PreAuthorize("hasAuthority('system:building:edit')")
    public Result<Building> update(
            @Parameter(description = "楼宇ID") @PathVariable Long id,
            @Valid @RequestBody BuildingRequest request) {
        log.info("更新楼宇: {}", id);
        return Result.success(buildingService.update(id, request));
    }

    /**
     * 删除楼宇
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除楼宇", description = "删除指定的楼宇")
    @PreAuthorize("hasAuthority('system:building:delete')")
    public Result<Void> delete(@Parameter(description = "楼宇ID") @PathVariable Long id) {
        log.info("删除楼宇: {}", id);
        buildingService.delete(id);
        return Result.success();
    }

    /**
     * 获取所有启用的楼宇
     */
    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的楼宇", description = "获取所有状态为启用的楼宇")
    @PreAuthorize("hasAuthority('system:building:view')")
    public Result<List<Building>> getAllEnabled(
            @Parameter(description = "楼宇类型") @RequestParam(required = false) Integer buildingType) {
        return Result.success(buildingService.getAllEnabled(buildingType));
    }

    /**
     * 检查楼号是否存在
     */
    @GetMapping("/exists")
    @Operation(summary = "检查楼号", description = "检查楼号是否已存在")
    @PreAuthorize("hasAuthority('system:building:view')")
    public Result<Boolean> existsBuildingNo(
            @Parameter(description = "楼号") @RequestParam String buildingNo,
            @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = buildingService.existsBuildingNo(buildingNo, excludeId);
        return Result.success(exists);
    }
}
