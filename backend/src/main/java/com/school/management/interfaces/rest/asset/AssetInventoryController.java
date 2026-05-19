package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetInventoryApplicationService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Asset Inventory (stocktaking) REST Controller.
 * <p>M2 (2026-05-20): 15 处直 jdbc 已下沉到 AssetInventoryApplicationService.
 */
@Slf4j
@RestController
@RequestMapping("/asset-inventories")
@RequiredArgsConstructor
public class AssetInventoryController {

    private final AssetInventoryApplicationService inventoryService;

    @PostMapping
    @CasbinAccess(resource = "asset:inventory", action = "edit")
    public Result<Long> createInventory(@RequestBody Map<String, Object> data) {
        return Result.success(inventoryService.create(data));
    }

    @GetMapping
    @CasbinAccess(resource = "asset:inventory", action = "view")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> listInventories(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> result = inventoryService.listPaged(pageNum, pageSize, status, keyword);
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.get("records");
        for (Map<String, Object> r : records) enrichInventory(r);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "asset:inventory", action = "view")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> getInventory(@PathVariable Long id) {
        Map<String, Object> inv = inventoryService.findInventoryWithDetails(id);
        if (inv == null) return Result.error("盘点单不存在");
        enrichInventory(inv);
        List<Map<String, Object>> details = (List<Map<String, Object>>) inv.get("details");
        for (Map<String, Object> d : details) {
            d.put("resultTypeDesc", getResultTypeDesc(d.get("resultType")));
        }
        return Result.success(inv);
    }

    @PutMapping("/{inventoryId}/details/{detailId}")
    @CasbinAccess(resource = "asset:inventory", action = "edit")
    public Result<Void> updateInventoryDetail(
            @PathVariable Long inventoryId,
            @PathVariable Long detailId,
            @RequestBody Map<String, Object> data) {
        int actualQuantity = ((Number) data.get("actualQuantity")).intValue();
        inventoryService.updateDetail(inventoryId, detailId, actualQuantity, data.get("remark"));
        return Result.success();
    }

    @PostMapping("/{id}/complete")
    @CasbinAccess(resource = "asset:inventory", action = "edit")
    public Result<Void> completeInventory(@PathVariable Long id) {
        inventoryService.complete(id);
        return Result.success();
    }

    @PostMapping("/{id}/cancel")
    @CasbinAccess(resource = "asset:inventory", action = "edit")
    public Result<Void> cancelInventory(@PathVariable Long id) {
        inventoryService.cancel(id);
        return Result.success();
    }

    @GetMapping("/statistics")
    @CasbinAccess(resource = "asset:inventory", action = "view")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(inventoryService.statistics());
    }

    // ==================== Presentation Helpers ====================

    private void enrichInventory(Map<String, Object> inv) {
        inv.put("statusDesc", getInventoryStatusDesc(inv.get("status")));
        inv.put("scopeTypeDesc", getScopeTypeDesc(inv.get("scopeType")));
        int totalCount = inv.get("totalCount") != null ? ((Number) inv.get("totalCount")).intValue() : 0;
        int checkedCount = inv.get("checkedCount") != null ? ((Number) inv.get("checkedCount")).intValue() : 0;
        inv.put("progress", totalCount > 0 ? (checkedCount * 100 / totalCount) : 0);
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
