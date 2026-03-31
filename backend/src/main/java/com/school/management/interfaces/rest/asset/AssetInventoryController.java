package com.school.management.interfaces.rest.asset;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Asset Inventory (stocktaking) REST Controller
 *
 * Uses JdbcTemplate to operate on:
 * - asset_inventory
 * - asset_inventory_detail
 */
@Slf4j
@RestController
@RequestMapping("/asset-inventories")
@RequiredArgsConstructor
public class AssetInventoryController {

    private final JdbcTemplate jdbc;

    // ==================== Create Inventory ====================

    @PostMapping
    @Transactional
    public Result<Long> createInventory(@RequestBody Map<String, Object> data) {
        long id = IdWorker.getId();
        String code = "INV-" + System.currentTimeMillis();

        String scopeType = (String) data.get("scopeType");
        String scopeValue = (String) data.get("scopeValue");

        jdbc.update(
            "INSERT INTO asset_inventory (id, inventory_code, inventory_name, scope_type, scope_value, " +
            "start_date, end_date, status, total_count, checked_count, profit_count, loss_count, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, 1, 0, 0, 0, 0, ?)",
            id, code,
            data.get("inventoryName"),
            scopeType,
            scopeValue,
            toDate(data.get("startDate")),
            toDate(data.get("endDate")),
            data.get("createdBy")
        );

        // Auto-generate inventory details based on scope
        StringBuilder assetQuery = new StringBuilder(
            "SELECT id, quantity FROM asset WHERE deleted = 0 AND status != 4");
        List<Object> params = new ArrayList<>();

        if ("category".equals(scopeType) && scopeValue != null) {
            assetQuery.append(" AND category_id = ?");
            params.add(Long.parseLong(scopeValue));
        } else if ("location".equals(scopeType) && scopeValue != null) {
            assetQuery.append(" AND location_type = ?");
            params.add(scopeValue);
        }

        List<Map<String, Object>> assets = jdbc.queryForList(assetQuery.toString(), params.toArray());

        for (Map<String, Object> asset : assets) {
            jdbc.update(
                "INSERT INTO asset_inventory_detail (id, inventory_id, asset_id, expected_quantity) " +
                "VALUES (?, ?, ?, ?)",
                IdWorker.getId(), id, asset.get("id"), asset.get("quantity")
            );
        }

        // Update total count
        jdbc.update("UPDATE asset_inventory SET total_count = ? WHERE id = ?", assets.size(), id);

        return Result.success(id);
    }

    // ==================== List Inventories (paginated) ====================

    @GetMapping
    public Result<Map<String, Object>> listInventories(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {

        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (status != null) {
            where.append(" AND status = ?");
            params.add(status);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (inventory_code LIKE ? OR inventory_name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
        }

        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM asset_inventory" + where, Long.class, params.toArray());

        int offset = (pageNum - 1) * pageSize;
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbc.queryForList(
            "SELECT id, inventory_code AS inventoryCode, inventory_name AS inventoryName, " +
            "scope_type AS scopeType, scope_value AS scopeValue, " +
            "start_date AS startDate, end_date AS endDate, status, " +
            "total_count AS totalCount, checked_count AS checkedCount, " +
            "profit_count AS profitCount, loss_count AS lossCount, " +
            "created_by AS createdBy, created_at AS createdAt " +
            "FROM asset_inventory" + where +
            " ORDER BY created_at DESC LIMIT ? OFFSET ?",
            dataParams.toArray()
        );

        for (Map<String, Object> r : records) {
            r.put("statusDesc", getInventoryStatusDesc(r.get("status")));
            r.put("scopeTypeDesc", getScopeTypeDesc(r.get("scopeType")));
            int totalCount = r.get("totalCount") != null ? ((Number) r.get("totalCount")).intValue() : 0;
            int checkedCount = r.get("checkedCount") != null ? ((Number) r.get("checkedCount")).intValue() : 0;
            r.put("progress", totalCount > 0 ? (checkedCount * 100 / totalCount) : 0);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return Result.success(result);
    }

    // ==================== Get Inventory Detail ====================

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getInventory(@PathVariable Long id) {
        Map<String, Object> inv = jdbc.queryForMap(
            "SELECT id, inventory_code AS inventoryCode, inventory_name AS inventoryName, " +
            "scope_type AS scopeType, scope_value AS scopeValue, " +
            "start_date AS startDate, end_date AS endDate, status, " +
            "total_count AS totalCount, checked_count AS checkedCount, " +
            "profit_count AS profitCount, loss_count AS lossCount, " +
            "created_by AS createdBy, created_at AS createdAt " +
            "FROM asset_inventory WHERE id = ?", id
        );
        inv.put("statusDesc", getInventoryStatusDesc(inv.get("status")));
        inv.put("scopeTypeDesc", getScopeTypeDesc(inv.get("scopeType")));

        int totalCount = inv.get("totalCount") != null ? ((Number) inv.get("totalCount")).intValue() : 0;
        int checkedCount = inv.get("checkedCount") != null ? ((Number) inv.get("checkedCount")).intValue() : 0;
        inv.put("progress", totalCount > 0 ? (checkedCount * 100 / totalCount) : 0);

        // Load details
        List<Map<String, Object>> details = jdbc.queryForList(
            "SELECT d.id, d.inventory_id AS inventoryId, d.asset_id AS assetId, " +
            "a.asset_code AS assetCode, a.asset_name AS assetName, a.location_name AS locationName, " +
            "d.expected_quantity AS expectedQuantity, d.actual_quantity AS actualQuantity, " +
            "d.difference, d.result_type AS resultType, " +
            "d.check_time AS checkTime, d.checker_id AS checkerId, d.checker_name AS checkerName, " +
            "d.remark " +
            "FROM asset_inventory_detail d LEFT JOIN asset a ON d.asset_id = a.id " +
            "WHERE d.inventory_id = ? ORDER BY a.asset_code", id
        );

        for (Map<String, Object> d : details) {
            d.put("resultTypeDesc", getResultTypeDesc(d.get("resultType")));
        }

        inv.put("details", details);

        return Result.success(inv);
    }

    // ==================== Update Inventory Detail ====================

    @PutMapping("/{inventoryId}/details/{detailId}")
    @Transactional
    public Result<Void> updateInventoryDetail(
            @PathVariable Long inventoryId,
            @PathVariable Long detailId,
            @RequestBody Map<String, Object> data) {

        int actualQuantity = ((Number) data.get("actualQuantity")).intValue();

        // Get expected quantity
        Map<String, Object> detail = jdbc.queryForMap(
            "SELECT expected_quantity FROM asset_inventory_detail WHERE id = ?", detailId);
        int expected = ((Number) detail.get("expected_quantity")).intValue();

        int difference = actualQuantity - expected;
        int resultType = difference == 0 ? 1 : (difference > 0 ? 2 : 3); // 1=normal, 2=profit, 3=loss

        jdbc.update(
            "UPDATE asset_inventory_detail SET actual_quantity = ?, difference = ?, " +
            "result_type = ?, check_time = NOW(), remark = ? WHERE id = ?",
            actualQuantity, difference, resultType, data.get("remark"), detailId
        );

        // Update inventory counters
        recalculateInventoryCounts(inventoryId);

        return Result.success();
    }

    // ==================== Complete Inventory ====================

    @PostMapping("/{id}/complete")
    @Transactional
    public Result<Void> completeInventory(@PathVariable Long id) {
        jdbc.update("UPDATE asset_inventory SET status = 2, updated_at = NOW() WHERE id = ?", id);
        return Result.success();
    }

    // ==================== Cancel Inventory ====================

    @PostMapping("/{id}/cancel")
    @Transactional
    public Result<Void> cancelInventory(@PathVariable Long id) {
        jdbc.update("UPDATE asset_inventory SET status = 3, updated_at = NOW() WHERE id = ?", id);
        return Result.success();
    }

    // ==================== Statistics ====================

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = jdbc.queryForMap(
            "SELECT " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS inProgressCount, " +
            "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS completedCount " +
            "FROM asset_inventory"
        );
        return Result.success(stats);
    }

    // ==================== Helpers ====================

    private void recalculateInventoryCounts(Long inventoryId) {
        Map<String, Object> counts = jdbc.queryForMap(
            "SELECT " +
            "COUNT(CASE WHEN actual_quantity IS NOT NULL THEN 1 END) AS checkedCount, " +
            "COUNT(CASE WHEN result_type = 2 THEN 1 END) AS profitCount, " +
            "COUNT(CASE WHEN result_type = 3 THEN 1 END) AS lossCount " +
            "FROM asset_inventory_detail WHERE inventory_id = ?", inventoryId
        );

        jdbc.update(
            "UPDATE asset_inventory SET checked_count = ?, profit_count = ?, loss_count = ?, " +
            "updated_at = NOW() WHERE id = ?",
            counts.get("checkedCount"), counts.get("profitCount"), counts.get("lossCount"), inventoryId
        );
    }

    private LocalDate toDate(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate) return (LocalDate) val;
        try { return LocalDate.parse(val.toString()); } catch (Exception e) { return null; }
    }

    private String getInventoryStatusDesc(Object status) {
        if (status == null) return null;
        int s = ((Number) status).intValue();
        switch (s) {
            case 1: return "进行中";
            case 2: return "已完成";
            case 3: return "已取消";
            default: return "未知";
        }
    }

    private String getScopeTypeDesc(Object type) {
        if (type == null) return null;
        switch (type.toString()) {
            case "all": return "全部资产";
            case "category": return "按分类";
            case "location": return "按位置";
            default: return type.toString();
        }
    }

    private String getResultTypeDesc(Object type) {
        if (type == null) return null;
        int t = ((Number) type).intValue();
        switch (t) {
            case 1: return "正常";
            case 2: return "盘盈";
            case 3: return "盘亏";
            default: return "未知";
        }
    }
}
