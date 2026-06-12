package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetApplicationService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Asset Management REST Controller.
 * <p>M2 (2026-05-20): 27 处直 jdbc 已下沉到 AssetApplicationService.
 * Controller 只保留 HTTP 绑定 + 各种 enum desc 表现层 helpers.
 */
@Slf4j
@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetApplicationService assetService;

    // ==================== Query ====================

    @GetMapping
    @CasbinAccess(resource = "asset:manage", action = "view")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> listAssets(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String locationType,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String keyword) {
        Map<String, Object> result = assetService.listPaged(pageNum, pageSize, categoryId, status,
            locationType, locationId, keyword);
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.get("records");
        Map<String, String> placeTypes = assetService.placeTypeNames();
        for (Map<String, Object> r : records) enrichAsset(r, placeTypes);
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<Map<String, Object>> getAsset(@PathVariable Long id) {
        Map<String, Object> asset = assetService.findById(id);
        if (asset == null) return Result.error("资产不存在");
        enrichAsset(asset, assetService.placeTypeNames());
        return Result.success(asset);
    }

    @GetMapping("/by-location")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<List<Map<String, Object>>> getAssetsByLocation(
            @RequestParam String locationType,
            @RequestParam Long locationId) {
        List<Map<String, Object>> assets = assetService.findByLocation(locationType, locationId);
        for (Map<String, Object> a : assets) {
            a.put("statusDesc", getStatusDesc(a.get("status")));
        }
        return Result.success(assets);
    }

    // ==================== Create ====================

    @PostMapping
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Long> createAsset(@RequestBody Map<String, Object> data) {
        return Result.success(assetService.create(data));
    }

    @PostMapping("/batch")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Map<String, Object>> batchCreateAssets(@RequestBody Map<String, Object> data) {
        int quantity = ((Number) data.getOrDefault("quantity", 1)).intValue();
        if (quantity < 1 || quantity > 1000) {
            return Result.error("数量必须在1到1000之间");
        }
        return Result.success(assetService.batchCreate(data, quantity));
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Void> updateAsset(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        assetService.update(id, data);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Void> deleteAsset(@PathVariable Long id) {
        assetService.softDelete(id);
        return Result.success();
    }

    // ==================== Transfer / Scrap ====================

    @PostMapping("/{id}/transfer")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Void> transferAsset(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        assetService.transfer(id, data);
        return Result.success();
    }

    @PostMapping("/batch-transfer")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Map<String, Object>> batchTransferAssets(@RequestBody Map<String, Object> data) {
        return Result.success(assetService.batchTransfer(data));
    }

    @PostMapping("/{id}/scrap")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Void> scrapAsset(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> data) {
        String reason = data != null ? (String) data.get("reason") : null;
        assetService.scrap(id, reason);
        return Result.success();
    }

    // ==================== History / Statistics ====================

    @GetMapping("/{id}/history")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<List<Map<String, Object>>> getAssetHistory(@PathVariable Long id) {
        List<Map<String, Object>> history = assetService.listHistory(id);
        for (Map<String, Object> h : history) {
            h.put("changeTypeDesc", getChangeTypeDesc(h.get("changeType")));
        }
        return Result.success(history);
    }

    @GetMapping("/statistics")
    @CasbinAccess(resource = "asset:manage", action = "view")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> getAssetStatistics() {
        Map<String, Object> stats = assetService.statistics();
        List<Map<String, Object>> locStats = (List<Map<String, Object>>) stats.get("locationStatistics");
        Map<String, String> placeTypes = assetService.placeTypeNames();
        for (Map<String, Object> ls : locStats) {
            ls.put("locationTypeDesc", getLocationTypeDesc(ls.get("locationType"), placeTypes));
        }
        return Result.success(stats);
    }

    // ==================== Maintenance ====================

    @GetMapping("/{id}/maintenance")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<List<Map<String, Object>>> getMaintenanceRecords(@PathVariable Long id) {
        List<Map<String, Object>> records = assetService.listMaintenance(id);
        for (Map<String, Object> r : records) {
            r.put("maintenanceTypeDesc", getMaintenanceTypeDesc(r.get("maintenanceType")));
            r.put("statusDesc", getMaintenanceStatusDesc(r.get("status")));
        }
        return Result.success(records);
    }

    @PostMapping("/{assetId}/maintenance")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Long> createMaintenance(@PathVariable Long assetId, @RequestBody Map<String, Object> data) {
        return Result.success(assetService.createMaintenance(assetId, data));
    }

    @PostMapping("/maintenance/{maintenanceId}/complete")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Void> completeMaintenance(@PathVariable Long maintenanceId, @RequestBody Map<String, Object> data) {
        assetService.completeMaintenance(maintenanceId, data);
        return Result.success();
    }

    // ==================== Presentation Helpers ====================

    private void enrichAsset(Map<String, Object> r, Map<String, String> placeTypes) {
        r.put("statusDesc", getStatusDesc(r.get("status")));
        r.put("managementModeDesc", getManagementModeDesc(r.get("managementMode")));
        r.put("locationTypeDesc", getLocationTypeDesc(r.get("locationType"), placeTypes));
    }

    private String getStatusDesc(Object status) {
        if (status == null) return null;
        int s = ((Number) status).intValue();
        switch (s) {
            case 1: return "在用";
            case 2: return "闲置";
            case 3: return "维修中";
            case 4: return "已报废";
            default: return "未知";
        }
    }

    private String getManagementModeDesc(Object mode) {
        if (mode == null) return null;
        int m = ((Number) mode).intValue();
        switch (m) {
            case 1: return "单品管理";
            case 2: return "批量管理";
            default: return "未知";
        }
    }

    /**
     * 位置类型显示名: 先查 entity_type_configs 的 PLACE 类型字典 (行业类型由对应插件
     * 贡献, 如教育的 CLASSROOM/DORMITORY — 核心不持有行业标签), 字典未命中再走核心
     * 中性兜底, 最后原样返回。
     */
    private String getLocationTypeDesc(Object type, Map<String, String> placeTypes) {
        if (type == null) return null;
        String code = type.toString();
        String fromDict = placeTypes.get(code.toUpperCase());
        if (fromDict != null) return fromDict;
        switch (code) {
            case "office": return "办公室";
            case "warehouse": return "仓库";
            case "other": return "其他";
            default: return code;
        }
    }

    private String getChangeTypeDesc(Object type) {
        if (type == null) return null;
        switch (type.toString()) {
            case "CREATE": return "入库";
            case "UPDATE": return "信息更新";
            case "TRANSFER": return "调拨";
            case "SCRAP": return "报废";
            case "MAINTENANCE": return "送修";
            case "MAINTENANCE_COMPLETE": return "维修完成";
            case "BORROW": return "借出";
            case "RETURN": return "归还";
            default: return type.toString();
        }
    }

    private String getMaintenanceTypeDesc(Object type) {
        if (type == null) return null;
        int t = ((Number) type).intValue();
        switch (t) {
            case 1: return "维修";
            case 2: return "保养";
            default: return "未知";
        }
    }

    private String getMaintenanceStatusDesc(Object status) {
        if (status == null) return null;
        int s = ((Number) status).intValue();
        switch (s) {
            case 1: return "进行中";
            case 2: return "已完成";
            default: return "未知";
        }
    }
}
