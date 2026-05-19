package com.school.management.application.asset;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资产告警应用服务 (M2, 2026-05-20).
 * AssetAlertController 10 处直 jdbc 全部下沉.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetAlertApplicationService {

    private final JdbcTemplate jdbcTemplate;

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

    public Map<String, Object> findById(Long id) {
        try {
            return jdbcTemplate.queryForMap(
                "SELECT " + ALERT_COLUMNS + " FROM asset_alert WHERE id = ?", id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Map<String, Object>> listUnread() {
        return jdbcTemplate.queryForList(
            "SELECT " + ALERT_COLUMNS + " FROM asset_alert WHERE is_read = 0 ORDER BY alert_time DESC");
    }

    public List<Map<String, Object>> listUnhandled() {
        return jdbcTemplate.queryForList(
            "SELECT " + ALERT_COLUMNS + " FROM asset_alert WHERE is_handled = 0 ORDER BY alert_time DESC");
    }

    @Transactional
    public void markAsRead(Long id) {
        jdbcTemplate.update(
            "UPDATE asset_alert SET is_read = 1, updated_at = NOW() WHERE id = ?", id);
    }

    @Transactional
    public void markAllAsRead() {
        jdbcTemplate.update(
            "UPDATE asset_alert SET is_read = 1, updated_at = NOW() WHERE is_read = 0");
    }

    @Transactional
    public void handleAlert(Long id, String remark) {
        jdbcTemplate.update(
            "UPDATE asset_alert SET is_handled = 1, is_read = 1, handle_remark = ?, " +
            "handle_time = NOW(), updated_at = NOW() WHERE id = ?",
            remark, id);
    }

    /**
     * 分页查询. 返回 {records, total, pageNum, pageSize}.
     */
    public Map<String, Object> queryPaged(int pageNum, int pageSize,
                                          Integer alertType, Boolean isRead, Boolean isHandled) {
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

        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM asset_alert" + where, Long.class, params.toArray());

        int offset = (pageNum - 1) * pageSize;
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(pageSize);
        dataParams.add(offset);

        List<Map<String, Object>> records = jdbcTemplate.queryForList(
            "SELECT " + ALERT_COLUMNS + " FROM asset_alert" + where +
            " ORDER BY alert_time DESC LIMIT ? OFFSET ?",
            dataParams.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    public Map<String, Object> statistics() {
        return jdbcTemplate.queryForMap(
            "SELECT " +
            "COUNT(*) AS totalCount, " +
            "SUM(CASE WHEN is_read = 0 THEN 1 ELSE 0 END) AS unreadCount, " +
            "SUM(CASE WHEN is_handled = 0 THEN 1 ELSE 0 END) AS unhandledCount, " +
            "SUM(CASE WHEN alert_type = 1 THEN 1 ELSE 0 END) AS overdueCount, " +
            "SUM(CASE WHEN alert_type = 2 THEN 1 ELSE 0 END) AS nearOverdueCount, " +
            "SUM(CASE WHEN alert_type = 3 THEN 1 ELSE 0 END) AS warrantyExpireCount, " +
            "SUM(CASE WHEN alert_type = 4 THEN 1 ELSE 0 END) AS lowStockCount " +
            "FROM asset_alert");
    }

    public long countUnread() {
        Long c = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM asset_alert WHERE is_read = 0", Long.class);
        return c == null ? 0 : c;
    }
}
