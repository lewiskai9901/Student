package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetBorrowApplicationService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Asset Borrow REST Controller.
 * <p>M2 (2026-05-20): 11 处直 jdbc 已下沉到 AssetBorrowApplicationService.
 */
@Slf4j
@RestController
@RequestMapping("/asset-borrows")
@RequiredArgsConstructor
public class AssetBorrowController {

    private final AssetBorrowApplicationService borrowService;

    @PostMapping
    @CasbinAccess(resource = "asset:borrow", action = "edit")
    public Result<Long> createBorrow(@RequestBody Map<String, Object> data) {
        return Result.success(borrowService.create(data));
    }

    @PostMapping("/{id}/return")
    @CasbinAccess(resource = "asset:borrow", action = "edit")
    public Result<Void> returnBorrow(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        borrowService.markReturned(id, data.get("returnCondition"), data.get("returnRemark"));
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @CasbinAccess(resource = "asset:borrow", action = "edit")
    public Result<Void> cancelBorrow(@PathVariable Long id) {
        borrowService.cancel(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "asset:borrow", action = "view")
    public Result<Map<String, Object>> getBorrow(@PathVariable Long id) {
        Map<String, Object> borrow = borrowService.findById(id);
        if (borrow == null) return Result.error("借用单不存在");
        enrichBorrow(borrow);
        return Result.success(borrow);
    }

    @GetMapping
    @CasbinAccess(resource = "asset:borrow", action = "view")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> listBorrows(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer borrowType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long borrowerId,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> result = borrowService.queryPaged(pageNum, pageSize, borrowType, status, borrowerId, keyword);
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.get("records");
        for (Map<String, Object> r : records) enrichBorrow(r);
        return Result.success(result);
    }

    @GetMapping("/my")
    @CasbinAccess(resource = "asset:borrow", action = "view")
    public Result<List<Map<String, Object>>> getMyBorrows() {
        Long userId = com.school.management.common.util.SecurityUtils.getCurrentUserId();
        if (userId == null) return Result.error("未登录");
        List<Map<String, Object>> borrows = borrowService.listMyActiveBorrows(userId);
        for (Map<String, Object> r : borrows) enrichBorrow(r);
        return Result.success(borrows);
    }

    @GetMapping("/asset/{assetId}")
    @CasbinAccess(resource = "asset:borrow", action = "view")
    public Result<List<Map<String, Object>>> getAssetBorrowHistory(@PathVariable Long assetId) {
        List<Map<String, Object>> borrows = borrowService.listAssetHistory(assetId);
        for (Map<String, Object> r : borrows) enrichBorrow(r);
        return Result.success(borrows);
    }

    @GetMapping("/overdue")
    @CasbinAccess(resource = "asset:borrow", action = "view")
    public Result<List<Map<String, Object>>> getOverdueBorrows() {
        List<Map<String, Object>> borrows = borrowService.listOverdue();
        for (Map<String, Object> r : borrows) enrichBorrow(r);
        return Result.success(borrows);
    }

    @GetMapping("/statistics")
    @CasbinAccess(resource = "asset:borrow", action = "view")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(borrowService.statistics());
    }

    // ==================== Presentation Helpers ====================

    private void enrichBorrow(Map<String, Object> borrow) {
        borrow.put("borrowTypeDesc", getBorrowTypeDesc(borrow.get("borrowType")));
        borrow.put("statusDesc", getBorrowStatusDesc(borrow.get("status")));
        borrow.put("returnConditionDesc", getReturnConditionDesc(borrow.get("returnCondition")));

        Object expectedReturn = borrow.get("expectedReturnDate");
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
