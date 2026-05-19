package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetDepreciationApplicationService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * Asset Depreciation REST Controller.
 * <p>M2 (2026-05-20): 13 处直 jdbc + 折旧计算引擎已下沉到 AssetDepreciationApplicationService.
 */
@Slf4j
@RestController
@RequestMapping("/asset-depreciation")
@RequiredArgsConstructor
public class AssetDepreciationController {

    private final AssetDepreciationApplicationService depService;

    @PostMapping("/{assetId}/calculate")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Map<String, Object>> calculateDepreciation(
            @PathVariable Long assetId,
            @RequestParam(required = false) String period) {
        String p = period != null ? period : AssetDepreciationApplicationService.currentPeriod();
        try {
            Map<String, Object> dep = depService.calculateAndSave(assetId, p);
            if (dep == null) return Result.error("该资产不需要计提折旧");
            dep.put("depreciationMethodName", getMethodName(dep.get("depreciationMethod")));
            return Result.success(dep);
        } catch (IllegalStateException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/calculate-all")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Map<String, Object>> calculateAllDepreciation(
            @RequestParam(required = false) String period) {
        String p = period != null ? period : AssetDepreciationApplicationService.currentPeriod();
        int processed = depService.calculateAllAndSave(p);
        Map<String, Object> result = new HashMap<>();
        result.put("period", p);
        result.put("processedCount", processed);
        return Result.success(result);
    }

    @PostMapping("/trigger")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Void> triggerDepreciation(@RequestParam String period) {
        depService.calculateAllAndSave(period);
        return Result.success();
    }

    @GetMapping("/{assetId}/preview")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<Map<String, Object>> previewDepreciation(
            @PathVariable Long assetId,
            @RequestParam(required = false) String period) {
        String p = period != null ? period : AssetDepreciationApplicationService.currentPeriod();
        Map<String, Object> dep = depService.preview(assetId, p);
        if (dep == null) return Result.error("该资产不需要计提折旧");
        dep.put("depreciationMethodName", getMethodName(dep.get("depreciationMethod")));
        return Result.success(dep);
    }

    @GetMapping("/{assetId}/history")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<List<Map<String, Object>>> getHistory(@PathVariable Long assetId) {
        List<Map<String, Object>> history = depService.listHistory(assetId);
        addMethodNames(history);
        return Result.success(history);
    }

    @GetMapping("/{assetId}/history-page")
    @CasbinAccess(resource = "asset:manage", action = "view")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> getHistoryPage(
            @PathVariable Long assetId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        Map<String, Object> result = depService.listHistoryPaged(assetId, pageNum, pageSize);
        addMethodNames((List<Map<String, Object>>) result.get("records"));
        return Result.success(result);
    }

    @GetMapping("/period/{period}")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<Map<String, Object>> getPeriodSummary(@PathVariable String period) {
        List<Map<String, Object>> records = depService.listByPeriod(period);
        addMethodNames(records);

        BigDecimal totalDep = BigDecimal.ZERO;
        for (Map<String, Object> r : records) {
            BigDecimal amount = toBigDecimal(r.get("depreciationAmount"));
            if (amount != null) totalDep = totalDep.add(amount);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("period", period);
        result.put("assetCount", records.size());
        result.put("totalDepreciation", totalDep);
        result.put("records", records);
        return Result.success(result);
    }

    @GetMapping("/methods")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<List<Map<String, Object>>> getMethods() {
        List<Map<String, Object>> methods = new ArrayList<>();
        methods.add(Map.of("code", 0, "name", "不计提折旧"));
        methods.add(Map.of("code", 1, "name", "直线法"));
        methods.add(Map.of("code", 2, "name", "双倍余额递减法"));
        methods.add(Map.of("code", 3, "name", "年数总和法"));
        methods.add(Map.of("code", 4, "name", "工作量法"));
        return Result.success(methods);
    }

    // ==================== Presentation Helpers ====================

    private void addMethodNames(List<Map<String, Object>> records) {
        for (Map<String, Object> r : records) {
            r.put("depreciationMethodName", getMethodName(r.get("depreciationMethod")));
        }
    }

    private String getMethodName(Object method) {
        if (method == null) return null;
        int m = ((Number) method).intValue();
        switch (m) {
            case 0: return "不计提折旧";
            case 1: return "直线法";
            case 2: return "双倍余额递减法";
            case 3: return "年数总和法";
            case 4: return "工作量法";
            default: return "未知";
        }
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return null; }
    }
}
