package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetAlertApplicationService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Asset Alert REST Controller.
 * <p>M2 (2026-05-20): 10 处直 jdbc 已下沉到 AssetAlertApplicationService.
 */
@Slf4j
@RestController
@RequestMapping("/asset-alerts")
@RequiredArgsConstructor
public class AssetAlertController {

    private final AssetAlertApplicationService alertService;

    @GetMapping("/{id}")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<Map<String, Object>> getAlert(@PathVariable Long id) {
        Map<String, Object> alert = alertService.findById(id);
        if (alert == null) return Result.error("告警不存在");
        enrichAlert(alert);
        return Result.success(alert);
    }

    @GetMapping("/unread")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<List<Map<String, Object>>> getUnreadAlerts() {
        List<Map<String, Object>> alerts = alertService.listUnread();
        for (Map<String, Object> a : alerts) enrichAlert(a);
        return Result.success(alerts);
    }

    @GetMapping("/unhandled")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<List<Map<String, Object>>> getUnhandledAlerts() {
        List<Map<String, Object>> alerts = alertService.listUnhandled();
        for (Map<String, Object> a : alerts) enrichAlert(a);
        return Result.success(alerts);
    }

    @PostMapping("/{id}/read")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Void> markAsRead(@PathVariable Long id) {
        alertService.markAsRead(id);
        return Result.success();
    }

    @PostMapping("/read-all")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Void> markAllAsRead() {
        alertService.markAllAsRead();
        return Result.success();
    }

    @PostMapping("/{id}/handle")
    @CasbinAccess(resource = "asset:manage", action = "edit")
    public Result<Void> handleAlert(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> data) {
        String remark = data != null ? (String) data.get("remark") : null;
        alertService.handleAlert(id, remark);
        return Result.success();
    }

    @GetMapping
    @CasbinAccess(resource = "asset:manage", action = "view")
    @SuppressWarnings("unchecked")
    public Result<Map<String, Object>> queryAlerts(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer alertType,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Boolean isHandled) {
        Map<String, Object> result = alertService.queryPaged(pageNum, pageSize, alertType, isRead, isHandled);
        List<Map<String, Object>> records = (List<Map<String, Object>>) result.get("records");
        for (Map<String, Object> r : records) enrichAlert(r);
        return Result.success(result);
    }

    @GetMapping("/statistics")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(alertService.statistics());
    }

    @GetMapping("/unread/count")
    @CasbinAccess(resource = "asset:manage", action = "view")
    public Result<Long> countUnread() {
        return Result.success(alertService.countUnread());
    }

    // ==================== Presentation Helpers ====================

    private void enrichAlert(Map<String, Object> alert) {
        alert.put("alertTypeDesc", getAlertTypeDesc(alert.get("alertType")));
        alert.put("alertLevelDesc", getAlertLevelDesc(alert.get("alertLevel")));
    }

    private String getAlertTypeDesc(Object type) {
        if (type == null) return null;
        int t = ((Number) type).intValue();
        switch (t) {
            case 1: return "借用逾期";
            case 2: return "即将逾期";
            case 3: return "保修到期";
            case 4: return "库存不足";
            default: return "未知";
        }
    }

    private String getAlertLevelDesc(Object level) {
        if (level == null) return null;
        int l = ((Number) level).intValue();
        switch (l) {
            case 1: return "普通";
            case 2: return "重要";
            case 3: return "紧急";
            default: return "未知";
        }
    }
}
