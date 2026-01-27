package com.school.management.interfaces.rest.asset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.application.asset.FixedAssetApplicationService;
import com.school.management.application.asset.command.*;
import com.school.management.application.asset.query.*;
import com.school.management.common.ApiResponse;
import com.school.management.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 固定资产管理接口
 */
@Tag(name = "固定资产管理", description = "设备、家具等固定资产的全生命周期管理")
@RestController
@RequestMapping("/api/v2/assets")
@RequiredArgsConstructor
public class FixedAssetController {

    private final FixedAssetApplicationService assetService;

    @Operation(summary = "获取资产列表")
    @GetMapping
    @PreAuthorize("hasAuthority('asset:list')")
    public ApiResponse<IPage<AssetDTO>> listAssets(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String locationType,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        AssetQueryCriteria criteria = AssetQueryCriteria.builder()
                .categoryId(categoryId)
                .status(status)
                .locationType(locationType)
                .locationId(locationId)
                .keyword(keyword)
                .pageNum(pageNum)
                .pageSize(pageSize)
                .build();
        return ApiResponse.success(assetService.queryAssets(criteria));
    }

    @Operation(summary = "获取资产详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:list')")
    public ApiResponse<AssetDTO> getAsset(@PathVariable Long id) {
        return ApiResponse.success(assetService.getAsset(id));
    }

    @Operation(summary = "根据位置查询资产")
    @GetMapping("/by-location")
    @PreAuthorize("hasAuthority('asset:list')")
    public ApiResponse<List<AssetDTO>> getAssetsByLocation(
            @RequestParam String locationType,
            @RequestParam Long locationId) {
        return ApiResponse.success(assetService.getAssetsByLocation(locationType, locationId));
    }

    @Operation(summary = "创建资产")
    @PostMapping
    @PreAuthorize("hasAuthority('asset:create')")
    public ApiResponse<Long> createAsset(
            @Valid @RequestBody CreateAssetRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        CreateAssetCommand command = CreateAssetCommand.builder()
                .assetName(request.getAssetName())
                .categoryId(request.getCategoryId())
                .brand(request.getBrand())
                .model(request.getModel())
                .unit(request.getUnit())
                .quantity(request.getQuantity())
                .originalValue(request.getOriginalValue())
                .netValue(request.getNetValue())
                .purchaseDate(request.getPurchaseDate())
                .warrantyDate(request.getWarrantyDate())
                .supplier(request.getSupplier())
                .locationType(request.getLocationType())
                .locationId(request.getLocationId())
                .locationName(request.getLocationName())
                .responsibleUserId(request.getResponsibleUserId())
                .responsibleUserName(request.getResponsibleUserName())
                .remark(request.getRemark())
                .operatorId(user.getId())
                .operatorName(user.getRealName())
                .build();
        return ApiResponse.success(assetService.createAsset(command));
    }

    @Operation(summary = "更新资产")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:update')")
    public ApiResponse<Void> updateAsset(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssetRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        UpdateAssetCommand command = UpdateAssetCommand.builder()
                .id(id)
                .assetName(request.getAssetName())
                .categoryId(request.getCategoryId())
                .brand(request.getBrand())
                .model(request.getModel())
                .unit(request.getUnit())
                .quantity(request.getQuantity())
                .originalValue(request.getOriginalValue())
                .netValue(request.getNetValue())
                .purchaseDate(request.getPurchaseDate())
                .warrantyDate(request.getWarrantyDate())
                .supplier(request.getSupplier())
                .responsibleUserId(request.getResponsibleUserId())
                .responsibleUserName(request.getResponsibleUserName())
                .remark(request.getRemark())
                .operatorId(user.getId())
                .operatorName(user.getRealName())
                .build();
        assetService.updateAsset(command);
        return ApiResponse.success();
    }

    @Operation(summary = "删除资产")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:delete')")
    public ApiResponse<Void> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return ApiResponse.success();
    }

    @Operation(summary = "调拨资产")
    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAuthority('asset:transfer')")
    public ApiResponse<Void> transferAsset(
            @PathVariable Long id,
            @Valid @RequestBody TransferAssetRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        TransferAssetCommand command = TransferAssetCommand.builder()
                .assetId(id)
                .locationType(request.getLocationType())
                .locationId(request.getLocationId())
                .locationName(request.getLocationName())
                .responsibleUserId(request.getResponsibleUserId())
                .responsibleUserName(request.getResponsibleUserName())
                .remark(request.getRemark())
                .operatorId(user.getId())
                .operatorName(user.getRealName())
                .build();
        assetService.transferAsset(command);
        return ApiResponse.success();
    }

    @Operation(summary = "报废资产")
    @PostMapping("/{id}/scrap")
    @PreAuthorize("hasAuthority('asset:scrap')")
    public ApiResponse<Void> scrapAsset(
            @PathVariable Long id,
            @RequestBody(required = false) ScrapAssetRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        ScrapAssetCommand command = ScrapAssetCommand.builder()
                .assetId(id)
                .reason(request != null ? request.getReason() : null)
                .operatorId(user.getId())
                .operatorName(user.getRealName())
                .build();
        assetService.scrapAsset(command);
        return ApiResponse.success();
    }

    @Operation(summary = "创建维修记录")
    @PostMapping("/{id}/maintenance")
    @PreAuthorize("hasAuthority('asset:maintenance:create')")
    public ApiResponse<Long> createMaintenance(
            @PathVariable Long id,
            @Valid @RequestBody CreateMaintenanceRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        CreateMaintenanceCommand command = CreateMaintenanceCommand.builder()
                .assetId(id)
                .maintenanceType(request.getMaintenanceType())
                .faultDesc(request.getFaultDesc())
                .maintainer(request.getMaintainer())
                .operatorId(user.getId())
                .operatorName(user.getRealName())
                .build();
        return ApiResponse.success(assetService.createMaintenance(command));
    }

    @Operation(summary = "完成维修")
    @PostMapping("/maintenance/{maintenanceId}/complete")
    @PreAuthorize("hasAuthority('asset:maintenance:create')")
    public ApiResponse<Void> completeMaintenance(
            @PathVariable Long maintenanceId,
            @Valid @RequestBody CompleteMaintenanceRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        CompleteMaintenanceCommand command = CompleteMaintenanceCommand.builder()
                .maintenanceId(maintenanceId)
                .result(request.getResult())
                .cost(request.getCost())
                .maintainer(request.getMaintainer())
                .operatorId(user.getId())
                .operatorName(user.getRealName())
                .build();
        assetService.completeMaintenance(command);
        return ApiResponse.success();
    }

    @Operation(summary = "获取资产变更历史")
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('asset:list')")
    public ApiResponse<List<AssetHistoryDTO>> getAssetHistory(@PathVariable Long id) {
        return ApiResponse.success(assetService.getAssetHistory(id));
    }

    @Operation(summary = "获取资产维修记录")
    @GetMapping("/{id}/maintenance")
    @PreAuthorize("hasAuthority('asset:maintenance:list')")
    public ApiResponse<List<AssetMaintenanceDTO>> getAssetMaintenanceRecords(@PathVariable Long id) {
        return ApiResponse.success(assetService.getAssetMaintenanceRecords(id));
    }

    @Operation(summary = "获取资产统计")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('asset:list')")
    public ApiResponse<AssetStatisticsDTO> getStatistics() {
        return ApiResponse.success(assetService.getStatistics());
    }
}
