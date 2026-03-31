package com.school.management.interfaces.rest.asset;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Asset Depreciation REST Controller
 *
 * Uses JdbcTemplate to operate on:
 * - asset_depreciation
 * - asset (for reading depreciation config)
 */
@Slf4j
@RestController
@RequestMapping("/asset-depreciation")
@RequiredArgsConstructor
public class AssetDepreciationController {

    private final JdbcTemplate jdbc;

    private static final String DEP_COLUMNS =
        "id, asset_id AS assetId, asset_code AS assetCode, " +
        "depreciation_period AS depreciationPeriod, " +
        "beginning_value AS beginningValue, " +
        "beginning_accumulated_depreciation AS beginningAccumulatedDepreciation, " +
        "beginning_net_value AS beginningNetValue, " +
        "depreciation_amount AS depreciationAmount, " +
        "ending_accumulated_depreciation AS endingAccumulatedDepreciation, " +
        "ending_net_value AS endingNetValue, " +
        "used_months AS usedMonths, remaining_months AS remainingMonths, " +
        "depreciation_method AS depreciationMethod, " +
        "depreciation_date AS depreciationDate, " +
        "created_at AS createdAt, remark";

    // ==================== Calculate Depreciation ====================

    @PostMapping("/{assetId}/calculate")
    @Transactional
    public Result<Map<String, Object>> calculateDepreciation(
            @PathVariable Long assetId,
            @RequestParam(required = false) String period) {

        if (period == null) {
            period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        }

        // Check if already calculated for this period
        Long existing = jdbc.queryForObject(
            "SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ? AND depreciation_period = ?",
            Long.class, assetId, period);
        if (existing != null && existing > 0) {
            return Result.error("该资产本期已计提折旧");
        }

        Map<String, Object> dep = doCalculate(assetId, period);
        if (dep == null) {
            return Result.error("该资产不需要计提折旧");
        }

        // Save
        long id = IdWorker.getId();
        jdbc.update(
            "INSERT INTO asset_depreciation (id, asset_id, asset_code, depreciation_period, " +
            "beginning_value, beginning_accumulated_depreciation, beginning_net_value, " +
            "depreciation_amount, ending_accumulated_depreciation, ending_net_value, " +
            "used_months, remaining_months, depreciation_method, depreciation_date) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            id, assetId, dep.get("assetCode"), period,
            dep.get("beginningValue"), dep.get("beginningAccumulatedDepreciation"),
            dep.get("beginningNetValue"), dep.get("depreciationAmount"),
            dep.get("endingAccumulatedDepreciation"), dep.get("endingNetValue"),
            dep.get("usedMonths"), dep.get("remainingMonths"),
            dep.get("depreciationMethod"), LocalDate.now()
        );

        // Update asset accumulated depreciation and net value
        jdbc.update(
            "UPDATE asset SET accumulated_depreciation = ?, net_value = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            dep.get("endingAccumulatedDepreciation"), dep.get("endingNetValue"), assetId
        );

        dep.put("id", id);
        dep.put("depreciationPeriod", period);
        dep.put("depreciationDate", LocalDate.now().toString());
        dep.put("createdAt", LocalDate.now().toString());

        return Result.success(dep);
    }

    // ==================== Batch Calculate All ====================

    @PostMapping("/calculate-all")
    @Transactional
    public Result<Map<String, Object>> calculateAllDepreciation(
            @RequestParam(required = false) String period) {

        if (period == null) {
            period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        }

        // Find assets that need depreciation (method != 0, not scrapped)
        List<Map<String, Object>> assets = jdbc.queryForList(
            "SELECT id FROM asset WHERE deleted = 0 AND status != 4 " +
            "AND depreciation_method IS NOT NULL AND depreciation_method != 0 " +
            "AND useful_life IS NOT NULL AND useful_life > 0"
        );

        int processed = 0;
        for (Map<String, Object> asset : assets) {
            Long assetId = ((Number) asset.get("id")).longValue();

            // Skip if already calculated
            Long existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ? AND depreciation_period = ?",
                Long.class, assetId, period);
            if (existing != null && existing > 0) continue;

            Map<String, Object> dep = doCalculate(assetId, period);
            if (dep == null) continue;

            long id = IdWorker.getId();
            jdbc.update(
                "INSERT INTO asset_depreciation (id, asset_id, asset_code, depreciation_period, " +
                "beginning_value, beginning_accumulated_depreciation, beginning_net_value, " +
                "depreciation_amount, ending_accumulated_depreciation, ending_net_value, " +
                "used_months, remaining_months, depreciation_method, depreciation_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, assetId, dep.get("assetCode"), period,
                dep.get("beginningValue"), dep.get("beginningAccumulatedDepreciation"),
                dep.get("beginningNetValue"), dep.get("depreciationAmount"),
                dep.get("endingAccumulatedDepreciation"), dep.get("endingNetValue"),
                dep.get("usedMonths"), dep.get("remainingMonths"),
                dep.get("depreciationMethod"), LocalDate.now()
            );

            jdbc.update(
                "UPDATE asset SET accumulated_depreciation = ?, net_value = ?, updated_at = NOW() " +
                "WHERE id = ? AND deleted = 0",
                dep.get("endingAccumulatedDepreciation"), dep.get("endingNetValue"), assetId
            );

            processed++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("period", period);
        result.put("processedCount", processed);
        return Result.success(result);
    }

    // ==================== Trigger Depreciation ====================

    @PostMapping("/trigger")
    @Transactional
    public Result<Void> triggerDepreciation(@RequestParam String period) {
        // Delegate to calculate-all
        calculateAllDepreciation(period);
        return Result.success();
    }

    // ==================== Preview Depreciation ====================

    @GetMapping("/{assetId}/preview")
    public Result<Map<String, Object>> previewDepreciation(
            @PathVariable Long assetId,
            @RequestParam(required = false) String period) {

        if (period == null) {
            period = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        }

        Map<String, Object> dep = doCalculate(assetId, period);
        if (dep == null) {
            return Result.error("该资产不需要计提折旧");
        }
        dep.put("depreciationPeriod", period);
        return Result.success(dep);
    }

    // ==================== Depreciation History ====================

    @GetMapping("/{assetId}/history")
    public Result<List<Map<String, Object>>> getHistory(@PathVariable Long assetId) {
        List<Map<String, Object>> history = jdbc.queryForList(
            "SELECT " + DEP_COLUMNS + " FROM asset_depreciation " +
            "WHERE asset_id = ? ORDER BY depreciation_period DESC", assetId
        );
        addMethodNames(history);
        return Result.success(history);
    }

    // ==================== Depreciation History (paginated) ====================

    @GetMapping("/{assetId}/history-page")
    public Result<Map<String, Object>> getHistoryPage(
            @PathVariable Long assetId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {

        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ?", Long.class, assetId);

        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> records = jdbc.queryForList(
            "SELECT " + DEP_COLUMNS + " FROM asset_depreciation " +
            "WHERE asset_id = ? ORDER BY depreciation_period DESC LIMIT ? OFFSET ?",
            assetId, pageSize, offset
        );
        addMethodNames(records);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return Result.success(result);
    }

    // ==================== Period Summary ====================

    @GetMapping("/period/{period}")
    public Result<Map<String, Object>> getPeriodSummary(@PathVariable String period) {
        List<Map<String, Object>> records = jdbc.queryForList(
            "SELECT " + DEP_COLUMNS + " FROM asset_depreciation " +
            "WHERE depreciation_period = ? ORDER BY asset_code", period
        );
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

    // ==================== Depreciation Methods ====================

    @GetMapping("/methods")
    public Result<List<Map<String, Object>>> getMethods() {
        List<Map<String, Object>> methods = new ArrayList<>();
        methods.add(Map.of("code", 0, "name", "不计提折旧"));
        methods.add(Map.of("code", 1, "name", "直线法"));
        methods.add(Map.of("code", 2, "name", "双倍余额递减法"));
        methods.add(Map.of("code", 3, "name", "年数总和法"));
        methods.add(Map.of("code", 4, "name", "工作量法"));
        return Result.success(methods);
    }

    // ==================== Calculation Engine ====================

    private Map<String, Object> doCalculate(Long assetId, String period) {
        Map<String, Object> asset;
        try {
            asset = jdbc.queryForMap(
                "SELECT id, asset_code, original_value, residual_value, accumulated_depreciation, " +
                "useful_life, depreciation_method, purchase_date " +
                "FROM asset WHERE id = ? AND deleted = 0", assetId);
        } catch (Exception e) {
            return null;
        }

        Integer method = asset.get("depreciation_method") != null ?
            ((Number) asset.get("depreciation_method")).intValue() : null;
        if (method == null || method == 0) return null;

        BigDecimal originalValue = toBigDecimal(asset.get("original_value"));
        BigDecimal residualValue = toBigDecimal(asset.get("residual_value"));
        BigDecimal accDep = toBigDecimal(asset.get("accumulated_depreciation"));
        Integer usefulLife = asset.get("useful_life") != null ?
            ((Number) asset.get("useful_life")).intValue() : null;

        if (originalValue == null || usefulLife == null || usefulLife <= 0) return null;

        if (residualValue == null) residualValue = BigDecimal.ZERO;
        if (accDep == null) accDep = BigDecimal.ZERO;

        BigDecimal netValue = originalValue.subtract(accDep);
        if (netValue.compareTo(residualValue) <= 0) return null; // fully depreciated

        // Count used months
        Long usedMonthsCount = jdbc.queryForObject(
            "SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ?", Long.class, assetId);
        int usedMonths = usedMonthsCount != null ? usedMonthsCount.intValue() : 0;
        int remainingMonths = usefulLife - usedMonths;
        if (remainingMonths <= 0) return null;

        BigDecimal depAmount;

        switch (method) {
            case 1: // Straight-line
                depAmount = originalValue.subtract(residualValue)
                    .divide(BigDecimal.valueOf(usefulLife), 2, RoundingMode.HALF_UP);
                break;
            case 2: // Double declining balance
                BigDecimal rate = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(usefulLife), 6, RoundingMode.HALF_UP);
                depAmount = netValue.multiply(rate).setScale(2, RoundingMode.HALF_UP);
                // Ensure we don't go below residual value
                if (netValue.subtract(depAmount).compareTo(residualValue) < 0) {
                    depAmount = netValue.subtract(residualValue);
                }
                break;
            case 3: // Sum-of-years-digits
                int totalSum = usefulLife * (usefulLife + 1) / 2;
                depAmount = originalValue.subtract(residualValue)
                    .multiply(BigDecimal.valueOf(remainingMonths))
                    .divide(BigDecimal.valueOf(totalSum), 2, RoundingMode.HALF_UP);
                break;
            default:
                return null;
        }

        if (depAmount.compareTo(BigDecimal.ZERO) <= 0) return null;

        BigDecimal endingAccDep = accDep.add(depAmount);
        BigDecimal endingNetValue = originalValue.subtract(endingAccDep);

        Map<String, Object> result = new HashMap<>();
        result.put("assetId", assetId);
        result.put("assetCode", asset.get("asset_code"));
        result.put("beginningValue", originalValue);
        result.put("beginningAccumulatedDepreciation", accDep);
        result.put("beginningNetValue", netValue);
        result.put("depreciationAmount", depAmount);
        result.put("endingAccumulatedDepreciation", endingAccDep);
        result.put("endingNetValue", endingNetValue);
        result.put("usedMonths", usedMonths + 1);
        result.put("remainingMonths", remainingMonths - 1);
        result.put("depreciationMethod", method);
        result.put("depreciationMethodName", getMethodName(method));

        return result;
    }

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
