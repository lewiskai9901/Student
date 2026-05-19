package com.school.management.application.asset;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产折旧应用服务 (M2, 2026-05-20).
 * AssetDepreciationController 13 处直 jdbc + 折旧计算引擎下沉.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetDepreciationApplicationService {

    private final JdbcTemplate jdbcTemplate;

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

    public static String currentPeriod() {
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
    }

    /**
     * 单资产计提折旧.
     * @return 计提结果 dep map; null=不需要计提 / 资产无效 / 已折旧到残值
     * @throws IllegalStateException 本期已计提 (上层应转 Result.error)
     */
    @Transactional
    public Map<String, Object> calculateAndSave(Long assetId, String period) {
        Long existing = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ? AND depreciation_period = ?",
            Long.class, assetId, period);
        if (existing != null && existing > 0) {
            throw new IllegalStateException("该资产本期已计提折旧");
        }

        Map<String, Object> dep = doCalculate(assetId, period);
        if (dep == null) return null;
        persistDepreciation(assetId, period, dep);
        dep.put("depreciationPeriod", period);
        dep.put("depreciationDate", LocalDate.now().toString());
        dep.put("createdAt", LocalDate.now().toString());
        return dep;
    }

    /** 批量计提 — 返回处理数. */
    @Transactional
    public int calculateAllAndSave(String period) {
        List<Map<String, Object>> assets = jdbcTemplate.queryForList(
            "SELECT id FROM asset WHERE deleted = 0 AND status != 4 " +
            "AND depreciation_method IS NOT NULL AND depreciation_method != 0 " +
            "AND useful_life IS NOT NULL AND useful_life > 0");

        int processed = 0;
        for (Map<String, Object> asset : assets) {
            Long assetId = ((Number) asset.get("id")).longValue();
            Long existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ? AND depreciation_period = ?",
                Long.class, assetId, period);
            if (existing != null && existing > 0) continue;

            Map<String, Object> dep = doCalculate(assetId, period);
            if (dep == null) continue;
            persistDepreciation(assetId, period, dep);
            processed++;
        }
        return processed;
    }

    /** 仅预览, 不写库. */
    public Map<String, Object> preview(Long assetId, String period) {
        Map<String, Object> dep = doCalculate(assetId, period);
        if (dep == null) return null;
        dep.put("depreciationPeriod", period);
        return dep;
    }

    public List<Map<String, Object>> listHistory(Long assetId) {
        return jdbcTemplate.queryForList(
            "SELECT " + DEP_COLUMNS + " FROM asset_depreciation " +
            "WHERE asset_id = ? ORDER BY depreciation_period DESC", assetId);
    }

    public Map<String, Object> listHistoryPaged(Long assetId, int pageNum, int pageSize) {
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ?", Long.class, assetId);

        int offset = (pageNum - 1) * pageSize;
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
            "SELECT " + DEP_COLUMNS + " FROM asset_depreciation " +
            "WHERE asset_id = ? ORDER BY depreciation_period DESC LIMIT ? OFFSET ?",
            assetId, pageSize, offset);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    public List<Map<String, Object>> listByPeriod(String period) {
        return jdbcTemplate.queryForList(
            "SELECT " + DEP_COLUMNS + " FROM asset_depreciation " +
            "WHERE depreciation_period = ? ORDER BY asset_code", period);
    }

    // ==================== Calculation Engine ====================

    private Map<String, Object> doCalculate(Long assetId, String period) {
        Map<String, Object> asset;
        try {
            asset = jdbcTemplate.queryForMap(
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
        if (netValue.compareTo(residualValue) <= 0) return null;

        Long usedMonthsCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ?", Long.class, assetId);
        int usedMonths = usedMonthsCount != null ? usedMonthsCount.intValue() : 0;
        int remainingMonths = usefulLife - usedMonths;
        if (remainingMonths <= 0) return null;

        BigDecimal depAmount;
        switch (method) {
            case 1:
                depAmount = originalValue.subtract(residualValue)
                    .divide(BigDecimal.valueOf(usefulLife), 2, RoundingMode.HALF_UP);
                break;
            case 2:
                BigDecimal rate = BigDecimal.valueOf(2).divide(BigDecimal.valueOf(usefulLife), 6, RoundingMode.HALF_UP);
                depAmount = netValue.multiply(rate).setScale(2, RoundingMode.HALF_UP);
                if (netValue.subtract(depAmount).compareTo(residualValue) < 0) {
                    depAmount = netValue.subtract(residualValue);
                }
                break;
            case 3:
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
        return result;
    }

    private void persistDepreciation(Long assetId, String period, Map<String, Object> dep) {
        long id = IdWorker.getId();
        jdbcTemplate.update(
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
            dep.get("depreciationMethod"), LocalDate.now());

        jdbcTemplate.update(
            "UPDATE asset SET accumulated_depreciation = ?, net_value = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            dep.get("endingAccumulatedDepreciation"), dep.get("endingNetValue"), assetId);

        dep.put("id", id);
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return null; }
    }
}
