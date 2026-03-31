package com.school.management.interfaces.rest.asset;

import com.school.management.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Asset Alert REST Controller
 *
 * Uses JdbcTemplate to operate on: asset_alert
 */
@Slf4j
@RestController
@RequestMapping("/asset-alerts")
@RequiredArgsConstructor
public class AssetAlertController {

    private final JdbcTemplate jdbc;

    private static final String ALERT_COLUMNS =
        "id, alert_type AS alertType, asset_id AS assetId, " +
        "asset_code AS assetCode, asset_name AS assetName, " +
        "borrow_id AS borrowId, alert_content AS alertContent, " +
        "alert_level AS alertLevel, is_read AS isRead, is_handled AS isHandled, " +
        "handle_remark AS handleRemark, handle_time AS handleTime, " +
        "handler_id AS handlerId, handler_name AS handlerName, " +
        "notify_user_id AS notifyUserId, notify_user_name AS notifyUserName, " +
        "alert_time AS alertTime, expire_time AS expireTime, " +
        "created_at AS createdAt";

    // ==================== Get Alert ====================

    @GetMapping("/{id}")
    public Result<Map<String, Object>> getAlert(@PathVariable Long id) {
        Map<String, Object> alert = jdbc.queryForMap(
            "SELECT " + ALERT_COLUMNS + " FROM asset_alert WHERE id = ?", id);
        enrichAlert(alert);
        return Result.success(alert);
    }

    // ==================== Unread Alerts ====================

    @GetMapping("/unread")
    public Result<List<Map<String, Object>>> getUnreadAlerts() {
        List<Map<String, Object>> alerts = jdbc.queryForList(
            "SELECT " + ALERT_COLUMNS + " FROM asset_alert WHERE is_read = 0 ORDER BY alert_time DESC"
        );
        for (Map<String, Object> a : alerts) enrichAlert(a);
        return Result.success(alerts);
    }

    // ==================== Unhandled Alerts ====================

    @GetMapping("/unhandled")
    public Result<List<Map<String, Object>>> getUnhandledAlerts() {
        List<Map<String, Object>> alerts = jdbc.queryForList(
            "SELECT " + ALERT_COLUMNS + " FROM asset_alert WHERE is_handled = 0 ORDER BY alert_time DESC"
        );
        for (Map<String, Object> a : alerts) enrichAlert(a);
        return Result.success(alerts);
    }

    // ==================== Mark as Read ====================

    @PostMapping("/{id}/read")
    @Transactional
    public Result<Void> markAsRead(@PathVariable Long id) {
        jdbc.update("UPDATE asset_alert SET is_read = 1, updated_at = NOW() WHERE id = ?", id);
        return Result.success();
    }

    // ==================== Mark All as Read ====================

    @PostMapping("/read-all")
    @Transactional
    public Result<Void> markAllAsRead() {
        jdbc.update("UPDATE asset_alert SET is_read = 1, updated_at = NOW() WHERE is_read = 0");
        return Result.success();
    }

    // ==================== Handle Alert ====================

    @PostMapping("/{id}/handle")
    @Transactional
    public Result<Void> handleAlert(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> data) {
        String remark = data != null ? (String) data.get("remark") : null;
        jdbc.update(
            "UPDATE asset_alert SET is_handled = 1, is_read = 1, handle_remark = ?, " +
            "handle_time = NOW(), updated_at = NOW() WHERE id = ?",
            remark, id
        );
        return Result.success();
    }

    // ==================== Query Alerts (paginated) ====================

    @GetMapping
    public Result<Map<String, Object>> queryAlerts(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) Integer alertType,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Boolean isHandled) {

        StringBuilder where = new StringBuilder(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (alertType != null) {
            where.append(" AND alert_type = ?");
            params.add(alertType);
        }
        if (isRead != null) {
            where.append(" AND is_read = ?");
            params.add(isRead ? 1 : 0);
        }
        if (isHandled != null) {
            where.append(" AND is_handled = ?");
            params.add(isHandled ? 1 : 0);
        }

        Long total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM asset_alert" + where, Long.class, params.toArray());

        int offset = (pageNum - 1) * pageSize;
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbc.queryForList(
            "SELECT " + ALERT_COLUMNS + " FROM asset_alert" + where +
            " ORDER BY alert_time DESC LIMIT ? OFFSET ?",
            dataParams.toArray()
        );

        for (Map<String, Object> r : records) enrichAlert(r);

        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return Result.success(result);
    }

    // ==================== Statistics ====================

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = jdbc.queryForMap(
            "SELECT " +
            "COUNT(*) AS totalCount, " +
            "SUM(CASE WHEN is_read = 0 THEN 1 ELSE 0 END) AS unreadCount, " +
            "SUM(CASE WHEN is_handled = 0 THEN 1 ELSE 0 END) AS unhandledCount, " +
            "SUM(CASE WHEN alert_type = 1 THEN 1 ELSE 0 END) AS overdueCount, " +
            "SUM(CASE WHEN alert_type = 2 THEN 1 ELSE 0 END) AS nearOverdueCount, " +
            "SUM(CASE WHEN alert_type = 3 THEN 1 ELSE 0 END) AS warrantyExpireCount, " +
            "SUM(CASE WHEN alert_type = 4 THEN 1 ELSE 0 END) AS lowStockCount " +
            "FROM asset_alert"
        );
        return Result.success(stats);
    }

    // ==================== Unread Count ====================

    @GetMapping("/unread/count")
    public Result<Long> countUnread() {
        Long count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM asset_alert WHERE is_read = 0", Long.class);
        return Result.success(count);
    }

    // ==================== Helpers ====================

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
