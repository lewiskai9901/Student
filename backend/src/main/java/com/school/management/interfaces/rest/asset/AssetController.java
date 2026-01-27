package com.school.management.interfaces.rest.asset;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.application.asset.FixedAssetApplicationService;
import com.school.management.application.asset.command.*;
import com.school.management.application.asset.query.*;
import com.school.management.application.asset.query.BatchCreateResult;
import com.school.management.application.asset.query.BatchTransferResult;
import com.school.management.common.ApiResponse;
import com.school.management.common.PageResult;
import com.school.management.interfaces.rest.asset.dto.*;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 固定资产管理接口
 */
@Tag(name = "固定资产管理", description = "资产的增删改查、调拨、报废、维修等操作")
@RestController
@RequestMapping("/v2/assets")
@RequiredArgsConstructor
public class AssetController {

    private final FixedAssetApplicationService assetService;
    private final JwtTokenService jwtTokenService;

    // ============ 资产CRUD ============

    @Operation(summary = "分页查询资产列表")
    @GetMapping
    @PreAuthorize("hasAuthority('asset:list')")
    public ApiResponse<PageResult<AssetDTO>> getAssetList(
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

        IPage<AssetDTO> page = assetService.queryAssets(criteria);

        return ApiResponse.success(PageResult.of(page.getRecords(), page.getTotal(), pageNum, pageSize));
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
    @PreAuthorize("hasAuthority('asset:manage')")
    public ApiResponse<Long> createAsset(@Valid @RequestBody CreateAssetRequest request) {
        Long userId = jwtTokenService.getCurrentUserId();
        String userName = jwtTokenService.getCurrentUserName();

        CreateAssetCommand command = CreateAssetCommand.builder()
                .assetName(request.getAssetName())
                .categoryId(request.getCategoryId())
                .brand(request.getBrand())
                .model(request.getModel())
                .unit(request.getUnit())
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
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
                .operatorId(userId)
                .operatorName(userName)
                .build();

        return ApiResponse.success(assetService.createAsset(command));
    }

    @Operation(summary = "批量入库资产", description = "一次性入库多件相同规格的资产，自动生成资产编号")
    @PostMapping("/batch")
    @PreAuthorize("hasAuthority('asset:manage')")
    public ApiResponse<BatchCreateResult> batchCreateAssets(@Valid @RequestBody BatchCreateAssetRequest request) {
        Long userId = jwtTokenService.getCurrentUserId();
        String userName = jwtTokenService.getCurrentUserName();

        BatchCreateAssetCommand command = BatchCreateAssetCommand.builder()
                .assetName(request.getAssetName())
                .categoryId(request.getCategoryId())
                .brand(request.getBrand())
                .model(request.getModel())
                .unit(request.getUnit())
                .quantity(request.getQuantity())
                .originalValue(request.getOriginalValue())
                .purchaseDate(request.getPurchaseDate())
                .warrantyDate(request.getWarrantyDate())
                .supplier(request.getSupplier())
                .locationType(request.getLocationType())
                .locationId(request.getLocationId())
                .locationName(request.getLocationName())
                .responsibleUserId(request.getResponsibleUserId())
                .responsibleUserName(request.getResponsibleUserName())
                .remark(request.getRemark())
                .operatorId(userId)
                .operatorName(userName)
                .build();

        return ApiResponse.success(assetService.batchCreateAssets(command));
    }

    @Operation(summary = "更新资产")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:manage')")
    public ApiResponse<Void> updateAsset(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAssetRequest request) {
        Long userId = jwtTokenService.getCurrentUserId();
        String userName = jwtTokenService.getCurrentUserName();

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
                .operatorId(userId)
                .operatorName(userName)
                .build();

        assetService.updateAsset(command);
        return ApiResponse.success();
    }

    @Operation(summary = "删除资产")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:manage')")
    public ApiResponse<Void> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return ApiResponse.success();
    }

    // ============ 资产操作 ============

    @Operation(summary = "调拨资产")
    @PostMapping("/{id}/transfer")
    @PreAuthorize("hasAuthority('asset:manage')")
    public ApiResponse<Void> transferAsset(
            @PathVariable Long id,
            @Valid @RequestBody TransferAssetRequest request) {
        Long userId = jwtTokenService.getCurrentUserId();
        String userName = jwtTokenService.getCurrentUserName();

        TransferAssetCommand command = TransferAssetCommand.builder()
                .assetId(id)
                .locationType(request.getLocationType())
                .locationId(request.getLocationId())
                .locationName(request.getLocationName())
                .responsibleUserId(request.getResponsibleUserId())
                .responsibleUserName(request.getResponsibleUserName())
                .remark(request.getRemark())
                .operatorId(userId)
                .operatorName(userName)
                .build();

        assetService.transferAsset(command);
        return ApiResponse.success();
    }

    @Operation(summary = "批量调拨资产", description = "将多件资产一次性调拨到新位置")
    @PostMapping("/batch-transfer")
    @PreAuthorize("hasAuthority('asset:manage')")
    public ApiResponse<BatchTransferResult> batchTransferAssets(@Valid @RequestBody BatchTransferAssetRequest request) {
        Long userId = jwtTokenService.getCurrentUserId();
        String userName = jwtTokenService.getCurrentUserName();

        BatchTransferAssetCommand command = BatchTransferAssetCommand.builder()
                .assetIds(request.getAssetIds())
                .locationType(request.getLocationType())
                .locationId(request.getLocationId())
                .locationName(request.getLocationName())
                .responsibleUserId(request.getResponsibleUserId())
                .responsibleUserName(request.getResponsibleUserName())
                .remark(request.getRemark())
                .operatorId(userId)
                .operatorName(userName)
                .build();

        return ApiResponse.success(assetService.batchTransferAssets(command));
    }

    @Operation(summary = "报废资产")
    @PostMapping("/{id}/scrap")
    @PreAuthorize("hasAuthority('asset:manage')")
    public ApiResponse<Void> scrapAsset(
            @PathVariable Long id,
            @RequestBody(required = false) ScrapAssetRequest request) {
        Long userId = jwtTokenService.getCurrentUserId();
        String userName = jwtTokenService.getCurrentUserName();

        ScrapAssetCommand command = ScrapAssetCommand.builder()
                .assetId(id)
                .reason(request != null ? request.getReason() : null)
                .operatorId(userId)
                .operatorName(userName)
                .build();

        assetService.scrapAsset(command);
        return ApiResponse.success();
    }

    // ============ 历史和维修 ============

    @Operation(summary = "获取资产变更历史")
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('asset:list')")
    public ApiResponse<List<AssetHistoryDTO>> getAssetHistory(@PathVariable Long id) {
        return ApiResponse.success(assetService.getAssetHistory(id));
    }

    @Operation(summary = "获取资产维修记录")
    @GetMapping("/{id}/maintenance")
    @PreAuthorize("hasAuthority('asset:list')")
    public ApiResponse<List<AssetMaintenanceDTO>> getAssetMaintenanceRecords(@PathVariable Long id) {
        return ApiResponse.success(assetService.getAssetMaintenanceRecords(id));
    }

    @Operation(summary = "创建维修记录")
    @PostMapping("/{assetId}/maintenance")
    @PreAuthorize("hasAuthority('asset:manage')")
    public ApiResponse<Long> createMaintenance(
            @PathVariable Long assetId,
            @Valid @RequestBody CreateMaintenanceRequest request) {
        Long userId = jwtTokenService.getCurrentUserId();
        String userName = jwtTokenService.getCurrentUserName();

        CreateMaintenanceCommand command = CreateMaintenanceCommand.builder()
                .assetId(assetId)
                .maintenanceType(request.getMaintenanceType())
                .faultDesc(request.getFaultDesc())
                .maintainer(request.getMaintainer())
                .operatorId(userId)
                .operatorName(userName)
                .build();

        return ApiResponse.success(assetService.createMaintenance(command));
    }

    @Operation(summary = "完成维修")
    @PostMapping("/maintenance/{maintenanceId}/complete")
    @PreAuthorize("hasAuthority('asset:manage')")
    public ApiResponse<Void> completeMaintenance(
            @PathVariable Long maintenanceId,
            @Valid @RequestBody CompleteMaintenanceRequest request) {

        CompleteMaintenanceCommand command = CompleteMaintenanceCommand.builder()
                .maintenanceId(maintenanceId)
                .result(request.getResult())
                .cost(request.getCost())
                .maintainer(request.getMaintainer())
                .build();

        assetService.completeMaintenance(command);
        return ApiResponse.success();
    }

    // ============ 统计 ============

    @Operation(summary = "获取资产统计")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('asset:list')")
    public ApiResponse<AssetStatisticsDTO> getStatistics() {
        return ApiResponse.success(assetService.getStatistics());
    }
}
