package com.school.management.application.message.preference;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;

/**
 * S-3: 用户消息偏好服务 — 决定某条事件给某用户用哪些通道发.
 *
 * 优先级:
 *   1. 用户对该 eventType 的特定偏好 (event_type_code = 具体类型)
 *   2. 用户的全局偏好 (event_type_code = NULL)
 *   3. 系统默认 ["IN_APP"]
 *
 * 静默时段: 在 quiet_hours 内一律返回空 channels (不发).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMsgPreferenceService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    /**
     * 解析用户在某事件类型下的有效通道列表.
     *
     * @return 通道码列表 (例: ["IN_APP","EMAIL"]); 静默时段返回空表
     */
    public List<String> resolveChannels(Long tenantId, Long userId, String eventTypeCode) {
        if (userId == null) return List.of("IN_APP");
        // 先查特定 eventType 的偏好, 没有再查全局
        Map<String, Object> pref = findPreference(tenantId, userId, eventTypeCode);
        if (pref == null) {
            pref = findPreference(tenantId, userId, null);
        }
        if (pref == null) {
            return List.of("IN_APP"); // 系统默认
        }
        // 静默时段判断
        if (isInQuietHours((String) pref.get("quiet_hours_start"), (String) pref.get("quiet_hours_end"))) {
            return List.of();
        }
        // 解析 channels JSON
        try {
            String channels = (String) pref.get("channels");
            if (channels == null || channels.isBlank()) return List.of("IN_APP");
            return objectMapper.readValue(channels, List.class);
        } catch (Exception e) {
            log.warn("[preference] 解析 channels 失败 userId={}, eventType={}: {}",
                    userId, eventTypeCode, e.getMessage());
            return List.of("IN_APP");
        }
    }

    private Map<String, Object> findPreference(Long tenantId, Long userId, String eventTypeCode) {
        String sql = "SELECT channels, quiet_hours_start, quiet_hours_end FROM user_msg_preferences "
                + "WHERE tenant_id = ? AND user_id = ? AND deleted = 0 AND enabled = 1 AND "
                + (eventTypeCode == null ? "event_type_code IS NULL" : "event_type_code = ?")
                + " LIMIT 1";
        try {
            return eventTypeCode == null
                    ? jdbc.queryForMap(sql, tenantId == null ? 0L : tenantId, userId)
                    : jdbc.queryForMap(sql, tenantId == null ? 0L : tenantId, userId, eventTypeCode);
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isInQuietHours(String start, String end) {
        if (start == null || end == null || start.isBlank() || end.isBlank()) return false;
        try {
            LocalTime now = LocalTime.now();
            LocalTime s = LocalTime.parse(start);
            LocalTime e = LocalTime.parse(end);
            // 跨天 (例如 22:00 - 08:00)
            if (s.isAfter(e)) {
                return !now.isBefore(s) || !now.isAfter(e);
            }
            return !now.isBefore(s) && !now.isAfter(e);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * 列出某用户的所有偏好 (UI 显示用)
     */
    public List<Map<String, Object>> listForUser(Long tenantId, Long userId) {
        return jdbc.queryForList(
                "SELECT id, event_type_code, channels, quiet_hours_start, quiet_hours_end, enabled "
                        + "FROM user_msg_preferences "
                        + "WHERE tenant_id = ? AND user_id = ? AND deleted = 0 "
                        + "ORDER BY event_type_code IS NULL DESC, event_type_code",
                tenantId == null ? 0L : tenantId, userId);
    }

    /**
     * Upsert 用户偏好 (按 user_id + event_type_code 唯一, NULL 表示全局).
     */
    @Transactional
    public void upsert(Long tenantId, Long userId, String eventTypeCode,
                        List<String> channels, String quietStart, String quietEnd, boolean enabled) {
        if (userId == null) throw new IllegalArgumentException("userId 不能为空");
        String channelsJson;
        try {
            channelsJson = objectMapper.writeValueAsString(channels == null ? List.of("IN_APP") : channels);
        } catch (Exception e) {
            channelsJson = "[\"IN_APP\"]";
        }
        // 先尝试 update
        int updated;
        if (eventTypeCode == null) {
            updated = jdbc.update(
                    "UPDATE user_msg_preferences SET channels = ?, quiet_hours_start = ?, quiet_hours_end = ?, enabled = ? "
                            + "WHERE tenant_id = ? AND user_id = ? AND event_type_code IS NULL AND deleted = 0",
                    channelsJson, quietStart, quietEnd, enabled ? 1 : 0,
                    tenantId == null ? 0L : tenantId, userId);
        } else {
            updated = jdbc.update(
                    "UPDATE user_msg_preferences SET channels = ?, quiet_hours_start = ?, quiet_hours_end = ?, enabled = ? "
                            + "WHERE tenant_id = ? AND user_id = ? AND event_type_code = ? AND deleted = 0",
                    channelsJson, quietStart, quietEnd, enabled ? 1 : 0,
                    tenantId == null ? 0L : tenantId, userId, eventTypeCode);
        }
        if (updated == 0) {
            jdbc.update(
                    "INSERT INTO user_msg_preferences (tenant_id, user_id, event_type_code, channels, quiet_hours_start, quiet_hours_end, enabled) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    tenantId == null ? 0L : tenantId, userId, eventTypeCode,
                    channelsJson, quietStart, quietEnd, enabled ? 1 : 0);
        }
    }

    /**
     * 删除 (soft delete) 一条偏好
     */
    @Transactional
    public void delete(Long tenantId, Long userId, Long prefId) {
        jdbc.update("UPDATE user_msg_preferences SET deleted = 1 WHERE id = ? AND user_id = ? AND tenant_id = ?",
                prefId, userId, tenantId == null ? 0L : tenantId);
    }
}
