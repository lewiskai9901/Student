package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetDepreciationApplicationService;
import com.school.management.application.asset.AssetDepreciationScheduler;
import com.school.management.application.asset.query.AssetDepreciationDTO;
import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 资产折旧控制器
 */
@Tag(name = "资产折旧", description = "资产折旧管理接口")
@RestController
@RequestMapping("/asset-depreciation")
@RequiredArgsConstructor
public class AssetDepreciationController {

    private final AssetDepreciationApplicationService depreciationService;
    private final AssetDepreciationScheduler depreciationScheduler;

    private static final DateTimeFormatter PERIOD_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    @Operation(summary = "计算单个资产的折旧")
    @PostMapping("/{assetId}/calculate")
    @PreAuthorize("hasAuthority('asset:depreciation:manage')")
    public Result<AssetDepreciationDTO> calculateDepreciation(
            @PathVariable Long assetId,
            @RequestParam(required = false) String period
    ) {
        if (period == null || period.isEmpty()) {
            period = YearMonth.now().format(PERIOD_FORMATTER);
        }
        return Result.success(depreciationService.calculateDepreciation(assetId, period));
    }

    @Operation(summary = "批量计提所有资产折旧")
    @PostMapping("/calculate-all")
    @PreAuthorize("hasAuthority('asset:depreciation:manage')")
    public Result<Map<String, Object>> calculateAllDepreciation(
            @RequestParam(required = false) String period
    ) {
        if (period == null || period.isEmpty()) {
            period = YearMonth.now().format(PERIOD_FORMATTER);
        }
        int count = depreciationService.calculateAllDepreciation(period);

        return Result.success(Map.of(
                "period", period,
                "processedCount", count
        ));
    }

    @Operation(summary = "手动触发折旧任务（管理员）")
    @PostMapping("/trigger")
    @PreAuthorize("hasAuthority('asset:depreciation:manage')")
    public Result<Void> triggerDepreciation(@RequestParam String period) {
        depreciationScheduler.manualCalculate(period);
        return Result.success();
    }

    @Operation(summary = "预览折旧计算结果")
    @GetMapping("/{assetId}/preview")
    public Result<AssetDepreciationDTO> previewDepreciation(
            @PathVariable Long assetId,
            @RequestParam(required = false) String period
    ) {
        if (period == null || period.isEmpty()) {
            period = YearMonth.now().format(PERIOD_FORMATTER);
        }
        return Result.success(depreciationService.previewDepreciation(assetId, period));
    }

    @Operation(summary = "获取资产折旧历史")
    @GetMapping("/{assetId}/history")
    public Result<List<AssetDepreciationDTO>> getDepreciationHistory(@PathVariable Long assetId) {
        return Result.success(depreciationService.getDepreciationHistory(assetId));
    }

    @Operation(summary = "获取资产折旧历史（分页）")
    @GetMapping("/{assetId}/history-page")
    public Result<Map<String, Object>> getDepreciationHistoryPage(
            @PathVariable Long assetId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return Result.success(depreciationService.getDepreciationHistoryPage(assetId, pageNum, pageSize));
    }

    @Operation(summary = "获取某期间的折旧汇总")
    @GetMapping("/period/{period}")
    public Result<Map<String, Object>> getPeriodSummary(@PathVariable String period) {
        return Result.success(depreciationService.getPeriodSummary(period));
    }

    @Operation(summary = "获取折旧方法列表")
    @GetMapping("/methods")
    public Result<List<Map<String, Object>>> getDepreciationMethods() {
        return Result.success(depreciationService.getDepreciationMethods());
    }
}
