package com.school.management.application.asset;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产主体应用服务 (M2, 2026-05-20).
 * AssetController 27 处直 jdbc 下沉. 含 asset CRUD / transfer / scrap /
 * history / statistics / maintenance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetApplicationService {

    private final JdbcTemplate jdbcTemplate;

    private static final String ASSET_COLUMNS =
        "a.id, a.asset_code AS assetCode, a.asset_name AS assetName, " +
        "a.category_id AS categoryId, c.category_name AS categoryName, c.category_code AS categoryCode, " +
        "a.brand, a.model, a.unit, a.quantity, " +
        "a.original_value AS originalValue, a.net_value AS netValue, " +
        "a.purchase_date AS purchaseDate, a.warranty_date AS warrantyDate, " +
        "a.supplier, a.status, a.management_mode AS managementMode, " +
        "a.location_type AS locationType, a.location_id AS locationId, " +
        "a.location_name AS locationName, " +
        "a.responsible_user_id AS responsibleUserId, a.responsible_user_name AS responsibleUserName, " +
        "a.remark, a.created_by AS createdBy, a.created_at AS createdAt, a.updated_at AS updatedAt, " +
        "a.category_type AS categoryType, a.depreciation_method AS depreciationMethod, " +
        "a.residual_value AS residualValue, a.accumulated_depreciation AS accumulatedDepreciation, " +
        "a.useful_life AS usefulLife, a.stock_warning_threshold AS stockWarningThreshold";

    // ==================== Query ====================

    public Map<String, Object> listPaged(int pageNum, int pageSize, Long categoryId, Integer status,
                                          String locationType, Long locationId, String keyword) {
        StringBuilder where = new StringBuilder(" WHERE a.deleted = 0");
        List<Object> params = new ArrayList<>();
        if (categoryId != null) {
            where.append(" AND a.category_id = ?");
            params.add(categoryId);
        }
        if (status != null) {
            where.append(" AND a.status = ?");
            params.add(status);
        }
        if (locationType != null && !locationType.isEmpty()) {
            where.append(" AND a.location_type = ?");
            params.add(locationType);
        }
        if (locationId != null) {
            where.append(" AND a.location_id = ?");
            params.add(locationId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (a.asset_code LIKE ? OR a.asset_name LIKE ? OR a.brand LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM asset a" + where, Long.class, params.toArray());

        int offset = (pageNum - 1) * pageSize;
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(
            "SELECT " + ASSET_COLUMNS +
            " FROM asset a LEFT JOIN asset_category c ON a.category_id = c.id" + where +
            " ORDER BY a.created_at DESC LIMIT ? OFFSET ?",
            dataParams.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    public Map<String, Object> findById(Long id) {
        try {
            return jdbcTemplate.queryForMap(
                "SELECT " + ASSET_COLUMNS +
                " FROM asset a LEFT JOIN asset_category c ON a.category_id = c.id" +
                " WHERE a.id = ? AND a.deleted = 0", id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Map<String, Object>> findByLocation(String locationType, Long locationId) {
        return jdbcTemplate.queryForList(
            "SELECT " + ASSET_COLUMNS +
            " FROM asset a LEFT JOIN asset_category c ON a.category_id = c.id" +
            " WHERE a.location_type = ? AND a.location_id = ? AND a.deleted = 0" +
            " ORDER BY a.asset_code", locationType, locationId);
    }

    // ==================== Create ====================

    @Transactional
    public long create(Map<String, Object> data) {
        long id = IdWorker.getId();
        String assetCode = generateAssetCode(data);
        jdbcTemplate.update(
            "INSERT INTO asset (id, asset_code, asset_name, category_id, brand, model, unit, quantity, " +
            "original_value, net_value, purchase_date, warranty_date, supplier, status, management_mode, " +
            "location_type, location_id, location_name, responsible_user_id, responsible_user_name, " +
            "remark, created_by, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, ?, ?, ?, 0)",
            id, assetCode,
            data.get("assetName"),
            toLong(data.get("categoryId")),
            data.get("brand"), data.get("model"),
            data.getOrDefault("unit", "个"),
            data.getOrDefault("quantity", 1),
            toBigDecimal(data.get("originalValue")),
            toBigDecimal(data.get("netValue")),
            toDate(data.get("purchaseDate")),
            toDate(data.get("warrantyDate")),
            data.get("supplier"),
            data.get("managementMode"),
            data.get("locationType"),
            toLong(data.get("locationId")),
            data.get("locationName"),
            toLong(data.get("responsibleUserId")),
            data.get("responsibleUserName"),
            data.get("remark"),
            data.get("createdBy"));

        recordHistory(id, "CREATE", "资产入库", null, null, null,
            (String) data.get("locationType"), toLong(data.get("locationId")),
            (String) data.get("locationName"), null);

        return id;
    }

    @Transactional
    public Map<String, Object> batchCreate(Map<String, Object> data, int quantity) {
        Long categoryId = toLong(data.get("categoryId"));
        String categoryCode = "AST";
        if (categoryId != null) {
            try {
                Map<String, Object> cat = jdbcTemplate.queryForMap(
                    "SELECT category_code FROM asset_category WHERE id = ?", categoryId);
                categoryCode = (String) cat.get("category_code");
            } catch (Exception ignored) {}
        }

        BigDecimal unitPrice = toBigDecimal(data.get("originalValue"));
        List<Long> assetIds = new ArrayList<>();
        String firstCode = null;
        String lastCode = null;

        for (int i = 0; i < quantity; i++) {
            long id = IdWorker.getId();
            String code = categoryCode + "-" + String.format("%04d", getNextSeq(categoryCode));
            if (firstCode == null) firstCode = code;
            lastCode = code;
            assetIds.add(id);

            jdbcTemplate.update(
                "INSERT INTO asset (id, asset_code, asset_name, category_id, brand, model, unit, quantity, " +
                "original_value, net_value, purchase_date, warranty_date, supplier, status, management_mode, " +
                "location_type, location_id, location_name, responsible_user_id, responsible_user_name, " +
                "remark, created_by, deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?, ?, ?, ?, 1, 1, ?, ?, ?, ?, ?, ?, ?, 0)",
                id, code,
                data.get("assetName"), categoryId,
                data.get("brand"), data.get("model"),
                data.getOrDefault("unit", "个"),
                unitPrice, unitPrice,
                toDate(data.get("purchaseDate")),
                toDate(data.get("warrantyDate")),
                data.get("supplier"),
                data.get("locationType"),
                toLong(data.get("locationId")),
                data.get("locationName"),
                toLong(data.get("responsibleUserId")),
                data.get("responsibleUserName"),
                data.get("remark"),
                data.get("createdBy"));
        }

        BigDecimal totalValue = unitPrice != null ? unitPrice.multiply(BigDecimal.valueOf(quantity)) : null;

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", quantity);
        result.put("successCount", quantity);
        result.put("firstAssetCode", firstCode);
        result.put("lastAssetCode", lastCode);
        result.put("assetIds", assetIds);
        result.put("totalValue", totalValue);
        return result;
    }

    @Transactional
    public void update(Long id, Map<String, Object> data) {
        StringBuilder sql = new StringBuilder("UPDATE asset SET updated_at = NOW()");
        List<Object> params = new ArrayList<>();
        setIfPresent(sql, params, data, "assetName", "asset_name");
        setIfPresent(sql, params, data, "categoryId", "category_id");
        setIfPresent(sql, params, data, "brand", "brand");
        setIfPresent(sql, params, data, "model", "model");
        setIfPresent(sql, params, data, "unit", "unit");
        setIfPresent(sql, params, data, "quantity", "quantity");
        setIfPresent(sql, params, data, "originalValue", "original_value");
        setIfPresent(sql, params, data, "netValue", "net_value");
        setIfPresentDate(sql, params, data, "purchaseDate", "purchase_date");
        setIfPresentDate(sql, params, data, "warrantyDate", "warranty_date");
        setIfPresent(sql, params, data, "supplier", "supplier");
        setIfPresent(sql, params, data, "responsibleUserId", "responsible_user_id");
        setIfPresent(sql, params, data, "responsibleUserName", "responsible_user_name");
        setIfPresent(sql, params, data, "remark", "remark");
        sql.append(" WHERE id = ? AND deleted = 0");
        params.add(id);
        jdbcTemplate.update(sql.toString(), params.toArray());
        recordHistory(id, "UPDATE", "资产信息更新", null, null, null, null, null, null, null);
    }

    @Transactional
    public void softDelete(Long id) {
        jdbcTemplate.update(
            "UPDATE asset SET deleted = 1, updated_at = NOW() WHERE id = ? AND deleted = 0", id);
    }

    // ==================== Transfer / Scrap ====================

    @Transactional
    public void transfer(Long id, Map<String, Object> data) {
        Map<String, Object> old = jdbcTemplate.queryForMap(
            "SELECT location_type, location_id, location_name FROM asset WHERE id = ? AND deleted = 0", id);
        jdbcTemplate.update(
            "UPDATE asset SET location_type = ?, location_id = ?, location_name = ?, " +
            "responsible_user_id = ?, responsible_user_name = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            data.get("locationType"),
            toLong(data.get("locationId")),
            data.get("locationName"),
            toLong(data.get("responsibleUserId")),
            data.get("responsibleUserName"),
            id);
        recordHistory(id, "TRANSFER", "资产调拨",
            (String) old.get("location_type"), toLong(old.get("location_id")), (String) old.get("location_name"),
            (String) data.get("locationType"), toLong(data.get("locationId")), (String) data.get("locationName"),
            (String) data.get("remark"));
    }

    @Transactional
    public Map<String, Object> batchTransfer(Map<String, Object> data) {
        @SuppressWarnings("unchecked")
        List<Object> assetIds = (List<Object>) data.get("assetIds");
        String locationType = (String) data.get("locationType");
        Long locationId = toLong(data.get("locationId"));
        String locationName = (String) data.get("locationName");
        Long responsibleUserId = toLong(data.get("responsibleUserId"));
        String responsibleUserName = (String) data.get("responsibleUserName");
        String remark = (String) data.get("remark");

        List<Long> successIds = new ArrayList<>();
        List<Map<String, Object>> failedAssets = new ArrayList<>();

        for (Object rawId : assetIds) {
            Long assetId = toLong(rawId);
            try {
                Map<String, Object> old = jdbcTemplate.queryForMap(
                    "SELECT location_type, location_id, location_name, status FROM asset WHERE id = ? AND deleted = 0",
                    assetId);
                Integer assetStatus = (Integer) old.get("status");
                if (assetStatus != null && assetStatus == 4) {
                    Map<String, Object> fail = new HashMap<>();
                    fail.put("assetId", assetId);
                    fail.put("reason", "已报废资产不能调拨");
                    failedAssets.add(fail);
                    continue;
                }
                jdbcTemplate.update(
                    "UPDATE asset SET location_type = ?, location_id = ?, location_name = ?, " +
                    "responsible_user_id = ?, responsible_user_name = ?, updated_at = NOW() " +
                    "WHERE id = ? AND deleted = 0",
                    locationType, locationId, locationName,
                    responsibleUserId, responsibleUserName, assetId);
                recordHistory(assetId, "TRANSFER", "批量调拨",
                    (String) old.get("location_type"), toLong(old.get("location_id")), (String) old.get("location_name"),
                    locationType, locationId, locationName, remark);
                successIds.add(assetId);
            } catch (Exception e) {
                Map<String, Object> fail = new HashMap<>();
                fail.put("assetId", assetId);
                fail.put("reason", e.getMessage());
                failedAssets.add(fail);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", assetIds.size());
        result.put("successCount", successIds.size());
        result.put("failedCount", failedAssets.size());
        result.put("successAssetIds", successIds);
        result.put("failedAssets", failedAssets);
        result.put("targetLocationName", locationName);
        return result;
    }

    @Transactional
    public void scrap(Long id, String reason) {
        jdbcTemplate.update(
            "UPDATE asset SET status = 4, updated_at = NOW() WHERE id = ? AND deleted = 0", id);
        recordHistory(id, "SCRAP", "资产报废", null, null, null, null, null, null, reason);
    }

    // ==================== History / Statistics ====================

    public List<Map<String, Object>> listHistory(Long assetId) {
        return jdbcTemplate.queryForList(
            "SELECT id, asset_id AS assetId, change_type AS changeType, " +
            "change_content AS changeContent, " +
            "old_location_type AS oldLocationType, old_location_id AS oldLocationId, " +
            "old_location_name AS oldLocationName, " +
            "new_location_type AS newLocationType, new_location_id AS newLocationId, " +
            "new_location_name AS newLocationName, " +
            "operator_id AS operatorId, operator_name AS operatorName, " +
            "operate_time AS operateTime, remark " +
            "FROM asset_history WHERE asset_id = ? ORDER BY operate_time DESC", assetId);
    }

    public Map<String, Object> statistics() {
        Map<String, Object> stats = new HashMap<>();
        Map<String, Object> counts = jdbcTemplate.queryForMap(
            "SELECT " +
            "COUNT(*) AS totalCount, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS inUseCount, " +
            "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS idleCount, " +
            "SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) AS repairingCount, " +
            "SUM(CASE WHEN status = 4 THEN 1 ELSE 0 END) AS scrappedCount, " +
            "COALESCE(SUM(original_value), 0) AS totalOriginalValue, " +
            "COALESCE(SUM(net_value), 0) AS totalNetValue " +
            "FROM asset WHERE deleted = 0");
        stats.putAll(counts);

        List<Map<String, Object>> catStats = jdbcTemplate.queryForList(
            "SELECT a.category_id AS categoryId, c.category_name AS categoryName, " +
            "COUNT(*) AS count, COALESCE(SUM(a.original_value), 0) AS totalValue " +
            "FROM asset a LEFT JOIN asset_category c ON a.category_id = c.id " +
            "WHERE a.deleted = 0 GROUP BY a.category_id, c.category_name");
        stats.put("categoryStatistics", catStats);

        List<Map<String, Object>> locStats = jdbcTemplate.queryForList(
            "SELECT location_type AS locationType, COUNT(*) AS count " +
            "FROM asset WHERE deleted = 0 AND location_type IS NOT NULL " +
            "GROUP BY location_type");
        stats.put("locationStatistics", locStats);

        return stats;
    }

    // ==================== Maintenance ====================

    public List<Map<String, Object>> listMaintenance(Long assetId) {
        return jdbcTemplate.queryForList(
            "SELECT m.id, m.asset_id AS assetId, a.asset_code AS assetCode, a.asset_name AS assetName, " +
            "m.maintenance_type AS maintenanceType, m.fault_desc AS faultDesc, " +
            "m.start_date AS startDate, m.end_date AS endDate, m.cost, m.maintainer, " +
            "m.result, m.status, m.created_by AS createdBy, m.created_at AS createdAt " +
            "FROM asset_maintenance m LEFT JOIN asset a ON m.asset_id = a.id " +
            "WHERE m.asset_id = ? ORDER BY m.created_at DESC", assetId);
    }

    @Transactional
    public long createMaintenance(Long assetId, Map<String, Object> data) {
        long id = IdWorker.getId();
        jdbcTemplate.update(
            "INSERT INTO asset_maintenance (id, asset_id, maintenance_type, fault_desc, start_date, " +
            "maintainer, status, created_by) VALUES (?, ?, ?, ?, NOW(), ?, 1, ?)",
            id, assetId,
            data.get("maintenanceType"), data.get("faultDesc"),
            data.get("maintainer"), data.get("createdBy"));
        jdbcTemplate.update(
            "UPDATE asset SET status = 3, updated_at = NOW() WHERE id = ? AND deleted = 0", assetId);
        recordHistory(assetId, "MAINTENANCE", "送修", null, null, null, null, null, null,
            (String) data.get("faultDesc"));
        return id;
    }

    @Transactional
    public void completeMaintenance(Long maintenanceId, Map<String, Object> data) {
        Map<String, Object> m = jdbcTemplate.queryForMap(
            "SELECT asset_id FROM asset_maintenance WHERE id = ?", maintenanceId);
        Long assetId = toLong(m.get("asset_id"));

        jdbcTemplate.update(
            "UPDATE asset_maintenance SET status = 2, end_date = NOW(), result = ?, cost = ?, " +
            "maintainer = COALESCE(?, maintainer), updated_at = NOW() WHERE id = ?",
            data.get("result"), toBigDecimal(data.get("cost")),
            data.get("maintainer"), maintenanceId);

        jdbcTemplate.update(
            "UPDATE asset SET status = 1, updated_at = NOW() WHERE id = ? AND deleted = 0", assetId);

        recordHistory(assetId, "MAINTENANCE_COMPLETE", "维修完成", null, null, null, null, null, null,
            (String) data.get("result"));
    }

    // ==================== Helpers ====================

    private void recordHistory(Long assetId, String changeType, String content,
                               String oldLocType, Long oldLocId, String oldLocName,
                               String newLocType, Long newLocId, String newLocName,
                               String remark) {
        jdbcTemplate.update(
            "INSERT INTO asset_history (id, asset_id, change_type, change_content, " +
            "old_location_type, old_location_id, old_location_name, " +
            "new_location_type, new_location_id, new_location_name, " +
            "operator_id, operator_name, operate_time, remark) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NOW(), ?)",
            IdWorker.getId(), assetId, changeType, content,
            oldLocType, oldLocId, oldLocName,
            newLocType, newLocId, newLocName,
            remark);
    }

    private String generateAssetCode(Map<String, Object> data) {
        Long categoryId = toLong(data.get("categoryId"));
        String prefix = "AST";
        if (categoryId != null) {
            try {
                Map<String, Object> cat = jdbcTemplate.queryForMap(
                    "SELECT category_code FROM asset_category WHERE id = ?", categoryId);
                prefix = (String) cat.get("category_code");
            } catch (Exception ignored) {}
        }
        return prefix + "-" + String.format("%04d", getNextSeq(prefix));
    }

    private int getNextSeq(String prefix) {
        try {
            Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM asset WHERE asset_code LIKE ? AND deleted = 0",
                Long.class, prefix + "-%");
            return (count != null ? count.intValue() : 0) + 1;
        } catch (Exception e) {
            return 1;
        }
    }

    private void setIfPresent(StringBuilder sql, List<Object> params,
                              Map<String, Object> data, String key, String column) {
        if (data.containsKey(key)) {
            sql.append(", ").append(column).append(" = ?");
            params.add(data.get(key));
        }
    }

    private void setIfPresentDate(StringBuilder sql, List<Object> params,
                                  Map<String, Object> data, String key, String column) {
        if (data.containsKey(key) && data.get(key) != null) {
            sql.append(", ").append(column).append(" = ?");
            params.add(LocalDate.parse((String) data.get(key)));
        }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        try { return new BigDecimal(val.toString()); } catch (Exception e) { return null; }
    }

    private LocalDate toDate(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate) return (LocalDate) val;
        try { return LocalDate.parse(val.toString()); } catch (Exception e) { return null; }
    }
}
