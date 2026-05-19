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
 * 资产借用应用服务 (M2, 2026-05-20).
 * AssetBorrowController 11 处直 jdbc 下沉.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetBorrowApplicationService {

    private final JdbcTemplate jdbcTemplate;

    private static final String BORROW_COLUMNS =
        "id, borrow_no AS borrowNo, borrow_type AS borrowType, " +
        "asset_id AS assetId, asset_code AS assetCode, asset_name AS assetName, quantity, " +
        "borrower_id AS borrowerId, borrower_name AS borrowerName, " +
        "borrower_dept AS borrowerDept, borrower_phone AS borrowerPhone, " +
        "borrow_date AS borrowDate, expected_return_date AS expectedReturnDate, " +
        "actual_return_date AS actualReturnDate, " +
        "return_condition AS returnCondition, return_remark AS returnRemark, " +
        "returner_id AS returnerId, returner_name AS returnerName, " +
        "purpose, status, operator_id AS operatorId, operator_name AS operatorName, " +
        "created_at AS createdAt";

    @Transactional
    public long create(Map<String, Object> data) {
        long id = IdWorker.getId();
        String borrowNo = "BRW-" + System.currentTimeMillis();
        Long assetId = toLong(data.get("assetId"));

        // 查 asset 编码/名 — 缺失允许 null
        String assetCode = null;
        String assetName = null;
        try {
            Map<String, Object> asset = jdbcTemplate.queryForMap(
                "SELECT asset_code, asset_name FROM asset WHERE id = ? AND deleted = 0", assetId);
            assetCode = (String) asset.get("asset_code");
            assetName = (String) asset.get("asset_name");
        } catch (Exception ignored) {
            // 资产可能已删除/不存在 — 借用记录仍创建, 业务允许
        }

        jdbcTemplate.update(
            "INSERT INTO asset_borrow (id, borrow_no, borrow_type, asset_id, asset_code, asset_name, " +
            "quantity, borrower_id, borrower_name, borrower_dept, borrower_phone, " +
            "borrow_date, expected_return_date, purpose, status, operator_id, operator_name, deleted) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?, 1, ?, ?, 0)",
            id, borrowNo,
            data.get("borrowType"),
            assetId, assetCode, assetName,
            data.getOrDefault("quantity", 1),
            toLong(data.get("borrowerId")),
            data.get("borrowerName"),
            data.get("borrowerDept"),
            data.get("borrowerPhone"),
            toDate(data.get("expectedReturnDate")),
            data.get("purpose"),
            data.get("operatorId"),
            data.get("operatorName"));
        return id;
    }

    @Transactional
    public void markReturned(Long id, Object returnCondition, Object returnRemark) {
        jdbcTemplate.update(
            "UPDATE asset_borrow SET status = 2, actual_return_date = NOW(), " +
            "return_condition = ?, return_remark = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            returnCondition, returnRemark, id);
    }

    @Transactional
    public void cancel(Long id) {
        jdbcTemplate.update(
            "UPDATE asset_borrow SET status = 4, updated_at = NOW() WHERE id = ? AND deleted = 0", id);
    }

    public Map<String, Object> findById(Long id) {
        try {
            return jdbcTemplate.queryForMap(
                "SELECT " + BORROW_COLUMNS + " FROM asset_borrow WHERE id = ? AND deleted = 0", id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Map<String, Object> queryPaged(int pageNum, int pageSize,
                                          Integer borrowType, Integer status, Long borrowerId, String keyword) {
        StringBuilder where = new StringBuilder(" WHERE deleted = 0");
        List<Object> params = new ArrayList<>();
        if (borrowType != null) {
            where.append(" AND borrow_type = ?");
            params.add(borrowType);
        }
        if (status != null) {
            where.append(" AND status = ?");
            params.add(status);
        }
        if (borrowerId != null) {
            where.append(" AND borrower_id = ?");
            params.add(borrowerId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            where.append(" AND (borrow_no LIKE ? OR asset_name LIKE ? OR borrower_name LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
        }

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM asset_borrow" + where, Long.class, params.toArray());

        int offset = (pageNum - 1) * pageSize;
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(
            "SELECT " + BORROW_COLUMNS + " FROM asset_borrow" + where +
            " ORDER BY created_at DESC LIMIT ? OFFSET ?",
            dataParams.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    public List<Map<String, Object>> listMyActiveBorrows() {
        return jdbcTemplate.queryForList(
            "SELECT " + BORROW_COLUMNS + " FROM asset_borrow WHERE deleted = 0 AND status IN (1, 3) " +
            "ORDER BY created_at DESC");
    }

    public List<Map<String, Object>> listAssetHistory(Long assetId) {
        return jdbcTemplate.queryForList(
            "SELECT " + BORROW_COLUMNS + " FROM asset_borrow WHERE asset_id = ? AND deleted = 0 " +
            "ORDER BY created_at DESC", assetId);
    }

    public List<Map<String, Object>> listOverdue() {
        return jdbcTemplate.queryForList(
            "SELECT " + BORROW_COLUMNS + " FROM asset_borrow " +
            "WHERE deleted = 0 AND status = 1 AND expected_return_date < CURDATE() " +
            "ORDER BY expected_return_date");
    }

    public Map<String, Object> statistics() {
        return jdbcTemplate.queryForMap(
            "SELECT " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS borrowedCount, " +
            "SUM(CASE WHEN status = 1 AND expected_return_date < CURDATE() THEN 1 ELSE 0 END) AS overdueCount, " +
            "SUM(CASE WHEN borrow_type = 1 AND status = 1 THEN 1 ELSE 0 END) AS usedCount " +
            "FROM asset_borrow WHERE deleted = 0");
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }

    private LocalDate toDate(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate) return (LocalDate) val;
        try { return LocalDate.parse(val.toString()); } catch (Exception e) { return null; }
    }
}
