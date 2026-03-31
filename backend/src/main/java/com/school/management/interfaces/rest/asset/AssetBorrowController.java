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
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Asset Borrow REST Controller
 *
 * Uses JdbcTemplate to operate on: asset_borrow
 */
@Slf4j
@RestController
@RequestMapping("/asset-borrows")
@RequiredArgsConstructor
public class AssetBorrowController {

    private final JdbcTemplate jdbc;

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

    // ==================== Create Borrow ====================

    @PostMapping
    @Transactional
    public Result<Long> createBorrow(@RequestBody Map<String, Object> data) {
        long id = IdWorker.getId();
        String borrowNo = "BRW-" + System.currentTimeMillis();

        Long assetId = toLong(data.get("assetId"));

        // Get asset info
        String assetCode = null;
        String assetName = null;
        try {
            Map<String, Object> asset = jdbc.queryForMap(
                "SELECT asset_code, asset_name FROM asset WHERE id = ? AND deleted = 0", assetId);
            assetCode = (String) asset.get("asset_code");
            assetName = (String) asset.get("asset_name");
        } catch (Exception ignored) {}

        jdbc.update(
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
            data.get("operatorName")
        );

        return Result.success(id);
    }

    // ==================== Return Borrow ====================

    @PostMapping("/{id}/return")
    @Transactional
    public Result<Void> returnBorrow(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        jdbc.update(
            "UPDATE asset_borrow SET status = 2, actual_return_date = NOW(), " +
            "return_condition = ?, return_remark = ?, updated_at = NOW() " +
            "WHERE id = ? AND deleted = 0",
            data.get("returnCondition"),
            data.get("returnRemark"),
            id
        );
        return Result.success();
    }

    // ==================== Cancel Borrow ====================

    @PostMapping("/{id}/cancel")
    @Transactional
    public Result<Void> cancelBorrow(@PathVariable Long id) {
        jdbc.update(
            "UPDATE asset_borrow SET status = 4, updated_at = NOW() WHERE id = ? AND deleted = 0", id);
        return Result.success();
    }

    // ==================== Get Borrow Detail ====================

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getBorrow(@PathVariable Long id) {
        Map<String, Object> borrow = jdbc.queryForMap(
            "SELECT " + BORROW_COLUMNS + " FROM asset_borrow WHERE id = ? AND deleted = 0", id);
        enrichBorrow(borrow);
        return Result.success(borrow);
    }

    // ==================== List Borrows (paginated) ====================

    @GetMapping
    public Result<Map<String, Object>> listBorrows(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer borrowType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long borrowerId,
            @RequestParam(required = false) String keyword) {

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

        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM asset_borrow" + where, Long.class, params.toArray());

        int offset = (pageNum - 1) * pageSize;
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbc.queryForList(
            "SELECT " + BORROW_COLUMNS + " FROM asset_borrow" + where +
            " ORDER BY created_at DESC LIMIT ? OFFSET ?",
            dataParams.toArray()
        );

        for (Map<String, Object> r : records) {
            enrichBorrow(r);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return Result.success(result);
    }

    // ==================== My Borrows ====================

    @GetMapping("/my")
    public Result<List<Map<String, Object>>> getMyBorrows() {
        // In a real system, get current user from SecurityContext.
        // For now, return all active borrows.
        List<Map<String, Object>> borrows = jdbc.queryForList(
            "SELECT " + BORROW_COLUMNS + " FROM asset_borrow WHERE deleted = 0 AND status IN (1, 3) " +
            "ORDER BY created_at DESC"
        );
        for (Map<String, Object> r : borrows) {
            enrichBorrow(r);
        }
        return Result.success(borrows);
    }

    // ==================== Asset Borrow History ====================

    @GetMapping("/asset/{assetId}")
    public Result<List<Map<String, Object>>> getAssetBorrowHistory(@PathVariable Long assetId) {
        List<Map<String, Object>> borrows = jdbc.queryForList(
            "SELECT " + BORROW_COLUMNS + " FROM asset_borrow WHERE asset_id = ? AND deleted = 0 " +
            "ORDER BY created_at DESC", assetId
        );
        for (Map<String, Object> r : borrows) {
            enrichBorrow(r);
        }
        return Result.success(borrows);
    }

    // ==================== Overdue Borrows ====================

    @GetMapping("/overdue")
    public Result<List<Map<String, Object>>> getOverdueBorrows() {
        List<Map<String, Object>> borrows = jdbc.queryForList(
            "SELECT " + BORROW_COLUMNS + " FROM asset_borrow " +
            "WHERE deleted = 0 AND status = 1 AND expected_return_date < CURDATE() " +
            "ORDER BY expected_return_date"
        );
        for (Map<String, Object> r : borrows) {
            enrichBorrow(r);
        }
        return Result.success(borrows);
    }

    // ==================== Statistics ====================

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = jdbc.queryForMap(
            "SELECT " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS borrowedCount, " +
            "SUM(CASE WHEN status = 1 AND expected_return_date < CURDATE() THEN 1 ELSE 0 END) AS overdueCount, " +
            "SUM(CASE WHEN borrow_type = 1 AND status = 1 THEN 1 ELSE 0 END) AS usedCount " +
            "FROM asset_borrow WHERE deleted = 0"
        );
        return Result.success(stats);
    }

    // ==================== Helpers ====================

    private void enrichBorrow(Map<String, Object> borrow) {
        Object borrowType = borrow.get("borrowType");
        borrow.put("borrowTypeDesc", getBorrowTypeDesc(borrowType));
        borrow.put("statusDesc", getBorrowStatusDesc(borrow.get("status")));
        borrow.put("returnConditionDesc", getReturnConditionDesc(borrow.get("returnCondition")));

        // Calculate overdue info
        Object expectedReturn = borrow.get("expectedReturnDate");
        Object actualReturn = borrow.get("actualReturnDate");
        Object status = borrow.get("status");

        if (status != null && ((Number) status).intValue() == 1 && expectedReturn != null) {
            LocalDate expected = toLocalDate(expectedReturn);
            if (expected != null) {
                long days = ChronoUnit.DAYS.between(LocalDate.now(), expected);
                borrow.put("overdue", days < 0);
                borrow.put("overdueDays", days < 0 ? Math.abs(days) : 0);
                borrow.put("remainingDays", days >= 0 ? days : 0);
            }
        } else {
            borrow.put("overdue", false);
            borrow.put("overdueDays", 0);
            borrow.put("remainingDays", null);
        }
    }

    private LocalDate toLocalDate(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate) return (LocalDate) val;
        if (val instanceof java.sql.Date) return ((java.sql.Date) val).toLocalDate();
        try { return LocalDate.parse(val.toString().substring(0, 10)); } catch (Exception e) { return null; }
    }

    private LocalDate toDate(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate) return (LocalDate) val;
        try { return LocalDate.parse(val.toString()); } catch (Exception e) { return null; }
    }

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).longValue();
        try { return Long.parseLong(val.toString()); } catch (Exception e) { return null; }
    }

    private String getBorrowTypeDesc(Object type) {
        if (type == null) return null;
        int t = ((Number) type).intValue();
        switch (t) {
            case 1: return "领用";
            case 2: return "借用";
            default: return "未知";
        }
    }

    private String getBorrowStatusDesc(Object status) {
        if (status == null) return null;
        int s = ((Number) status).intValue();
        switch (s) {
            case 1: return "借出中";
            case 2: return "已归还";
            case 3: return "已逾期";
            case 4: return "已取消";
            default: return "未知";
        }
    }

    private String getReturnConditionDesc(Object condition) {
        if (condition == null) return null;
        switch (condition.toString()) {
            case "good": return "完好";
            case "damaged": return "损坏";
            case "lost": return "丢失";
            default: return condition.toString();
        }
    }
}
