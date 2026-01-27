package com.school.management.interfaces.rest.asset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.application.asset.AssetInventoryApplicationService;
import com.school.management.application.asset.command.CreateInventoryCommand;
import com.school.management.application.asset.query.AssetInventoryDTO;
import com.school.management.common.result.Result;
import com.school.management.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;

/**
 * 资产盘点控制器
 */
@Tag(name = "资产盘点", description = "资产盘点管理接口")
@RestController
@RequestMapping("/v2/asset-inventories")
@RequiredArgsConstructor
public class AssetInventoryController {

    private final AssetInventoryApplicationService inventoryService;

    @Operation(summary = "创建盘点任务")
    @PostMapping
    @PreAuthorize("hasAuthority('asset:inventory:create')")
    public Result<Long> createInventory(@Valid @RequestBody CreateInventoryRequest request) {
        Long operatorId = SecurityUtils.getCurrentUserId();

        CreateInventoryCommand command = CreateInventoryCommand.builder()
                .inventoryName(request.getInventoryName())
                .scopeType(request.getScopeType())
                .scopeValue(request.getScopeValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .operatorId(operatorId)
                .build();

        Long id = inventoryService.createInventory(command);
        return Result.success(id);
    }

    @Operation(summary = "分页查询盘点任务")
    @GetMapping
    @PreAuthorize("hasAuthority('asset:inventory:view')")
    public Result<IPage<AssetInventoryDTO>> listInventories(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        IPage<AssetInventoryDTO> page = inventoryService.queryInventories(status, keyword, pageNum, pageSize);
        return Result.success(page);
    }

    @Operation(summary = "获取盘点任务详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:inventory:view')")
    public Result<AssetInventoryDTO> getInventory(@PathVariable Long id) {
        AssetInventoryDTO dto = inventoryService.getInventory(id);
        return Result.success(dto);
    }

    @Operation(summary = "更新盘点明细")
    @PutMapping("/{id}/details/{detailId}")
    @PreAuthorize("hasAuthority('asset:inventory:edit')")
    public Result<Void> updateDetail(
            @PathVariable Long id,
            @PathVariable Long detailId,
            @Valid @RequestBody UpdateDetailRequest request) {

        Long checkerId = SecurityUtils.getCurrentUserId();
        String checkerName = SecurityUtils.getCurrentUsername();

        inventoryService.updateInventoryDetail(
                detailId,
                request.getActualQuantity(),
                request.getRemark(),
                checkerId,
                checkerName
        );
        return Result.success();
    }

    @Operation(summary = "完成盘点")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('asset:inventory:edit')")
    public Result<Void> completeInventory(@PathVariable Long id) {
        inventoryService.completeInventory(id);
        return Result.success();
    }

    @Operation(summary = "取消盘点")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('asset:inventory:edit')")
    public Result<Void> cancelInventory(@PathVariable Long id) {
        inventoryService.cancelInventory(id);
        return Result.success();
    }

    @Operation(summary = "获取盘点统计")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('asset:inventory:view')")
    public Result<AssetInventoryApplicationService.InventoryStatistics> getStatistics() {
        return Result.success(inventoryService.getStatistics());
    }

    // ============ 请求DTO ============

    @lombok.Data
    public static class CreateInventoryRequest {
        private String inventoryName;
        private String scopeType;
        private String scopeValue;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @lombok.Data
    public static class UpdateDetailRequest {
        private Integer actualQuantity;
        private String remark;
    }
}
