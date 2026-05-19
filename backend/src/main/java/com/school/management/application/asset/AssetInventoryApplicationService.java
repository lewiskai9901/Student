package com.school.management.application.asset;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产盘点应用服务 (M2, 2026-05-20).
 * AssetInventoryController 15 处直 jdbc 下沉.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetInventoryApplicationService {

    private final JdbcTemplate jdbcTemplate;

    private static final String INV_COLUMNS =
        "id, inventory_code AS inventoryCode, inventory_name AS inventoryName, " +
        "scope_type AS scopeType, scope_value AS scopeValue, " +
        "start_date AS startDate, end_date AS endDate, status, " +
        "total_count AS totalCount, checked_count AS checkedCount, " +
        "profit_count AS profitCount, loss_count AS lossCount, " +
        "created_by AS createdBy, created_at AS createdAt";

    @Transactional
    public long create(Map<String, Object> data) {
        long id = IdWorker.getId();
        String code = "INV-" + System.currentTimeMillis();
        String scopeType = (String) data.get("scopeType");
        String scopeValue = (String) data.get("scopeValue");

        jdbcTemplate.update(
            "INSERT INTO asset_inventory (id, inventory_code, inventory_name, scope_type, scope_value, " +
            "start_date, end_date, status, total_count, checked_count, profit_count, loss_count, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, 1, 0, 0, 0, 0, ?)",
            id, code,
            data.get("inventoryName"), scopeType, scopeValue,
            toDate(data.get("startDate")),
            toDate(data.get("endDate")),
            data.get("createdBy"));

        // 按 scope 自动生成 details
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
        List<Map<String, Object>> assets = jdbcTemplate.queryForList(assetQuery.toString(), params.toArray());

        for (Map<String, Object> asset : assets) {
            jdbcTemplate.update(
                "INSERT INTO asset_inventory_detail (id, inventory_id, asset_id, expected_quantity) " +
                "VALUES (?, ?, ?, ?)",
                IdWorker.getId(), id, asset.get("id"), asset.get("quantity"));
        }

        jdbcTemplate.update("UPDATE asset_inventory SET total_count = ? WHERE id = ?",
            assets.size(), id);

        return id;
    }

    public Map<String, Object> listPaged(int pageNum, int pageSize, Integer status, String keyword) {
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

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM asset_inventory" + where, Long.class, params.toArray());

        int offset = (pageNum - 1) * pageSize;
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(
            "SELECT " + INV_COLUMNS + " FROM asset_inventory" + where +
            " ORDER BY created_at DESC LIMIT ? OFFSET ?",
            dataParams.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    public Map<String, Object> findInventoryWithDetails(Long id) {
        Map<String, Object> inv;
        try {
            inv = jdbcTemplate.queryForMap(
                "SELECT " + INV_COLUMNS + " FROM asset_inventory WHERE id = ?", id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }

        List<Map<String, Object>> details = jdbcTemplate.queryForList(
            "SELECT d.id, d.inventory_id AS inventoryId, d.asset_id AS assetId, " +
            "a.asset_code AS assetCode, a.asset_name AS assetName, a.location_name AS locationName, " +
            "d.expected_quantity AS expectedQuantity, d.actual_quantity AS actualQuantity, " +
            "d.difference, d.result_type AS resultType, " +
            "d.check_time AS checkTime, d.checker_id AS checkerId, d.checker_name AS checkerName, " +
            "d.remark " +
            "FROM asset_inventory_detail d LEFT JOIN asset a ON d.asset_id = a.id " +
            "WHERE d.inventory_id = ? ORDER BY a.asset_code", id);

        inv.put("details", details);
        return inv;
    }

    @Transactional
    public void updateDetail(Long inventoryId, Long detailId, int actualQuantity, Object remark) {
        Map<String, Object> detail = jdbcTemplate.queryForMap(
            "SELECT expected_quantity FROM asset_inventory_detail WHERE id = ?", detailId);
        int expected = ((Number) detail.get("expected_quantity")).intValue();

        int difference = actualQuantity - expected;
        int resultType = difference == 0 ? 1 : (difference > 0 ? 2 : 3);

        jdbcTemplate.update(
            "UPDATE asset_inventory_detail SET actual_quantity = ?, difference = ?, " +
            "result_type = ?, check_time = NOW(), remark = ? WHERE id = ?",
            actualQuantity, difference, resultType, remark, detailId);

        recalculateInventoryCounts(inventoryId);
    }

    @Transactional
    public void complete(Long id) {
        jdbcTemplate.update(
            "UPDATE asset_inventory SET status = 2, updated_at = NOW() WHERE id = ?", id);
    }

    @Transactional
    public void cancel(Long id) {
        jdbcTemplate.update(
            "UPDATE asset_inventory SET status = 3, updated_at = NOW() WHERE id = ?", id);
    }

    public Map<String, Object> statistics() {
        return jdbcTemplate.queryForMap(
            "SELECT " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS inProgressCount, " +
            "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS completedCount " +
            "FROM asset_inventory");
    }

    private void recalculateInventoryCounts(Long inventoryId) {
        Map<String, Object> counts = jdbcTemplate.queryForMap(
            "SELECT " +
            "COUNT(CASE WHEN actual_quantity IS NOT NULL THEN 1 END) AS checkedCount, " +
            "COUNT(CASE WHEN result_type = 2 THEN 1 END) AS profitCount, " +
            "COUNT(CASE WHEN result_type = 3 THEN 1 END) AS lossCount " +
            "FROM asset_inventory_detail WHERE inventory_id = ?", inventoryId);

        jdbcTemplate.update(
            "UPDATE asset_inventory SET checked_count = ?, profit_count = ?, loss_count = ?, " +
            "updated_at = NOW() WHERE id = ?",
            counts.get("checkedCount"), counts.get("profitCount"), counts.get("lossCount"), inventoryId);
    }

    private LocalDate toDate(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate) return (LocalDate) val;
        try { return LocalDate.parse(val.toString()); } catch (Exception e) { return null; }
    }
}
