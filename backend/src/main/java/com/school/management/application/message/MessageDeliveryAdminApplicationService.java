package com.school.management.application.message;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * S-6: 失败重发 / 死信队列管理应用服务 — 平台管理员视角.
 *
 * 承载 {@code MessageDeliveryAdminController} 的全部数据访问逻辑,
 * 控制器只保留 HTTP 映射与权限注解.
 */
@Service
@RequiredArgsConstructor
public class MessageDeliveryAdminApplicationService {

    private final JdbcTemplate jdbc;

    /** 列失败消息 (sendStatus IN FAILED/PENDING) */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listFailed(int limit, String channel) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, receiver_type, receiver_id, title, source_event_type, "
                + "send_status, retry_count, last_error, created_at, sent_at "
                + "FROM msg_notifications "
                + "WHERE deleted = 0 AND send_status IN ('FAILED','PENDING') ");
        if (channel != null && !channel.isBlank()) {
            sql.append("AND msg_type = ? ");
        }
        sql.append("ORDER BY created_at DESC LIMIT ?");
        return channel != null && !channel.isBlank()
                ? jdbc.queryForList(sql.toString(), channel, limit)
                : jdbc.queryForList(sql.toString(), limit);
    }

    /** 死信列表 (retry_count >= 3 且 FAILED) */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listDeadLetter(int limit) {
        return jdbc.queryForList(
                "SELECT id, receiver_type, receiver_id, title, source_event_type, "
                + "retry_count, last_error, created_at "
                + "FROM msg_notifications "
                + "WHERE deleted = 0 AND send_status = 'FAILED' AND retry_count >= 3 "
                + "ORDER BY created_at DESC LIMIT ?", limit);
    }

    /** 单条重发 — 重置 sendStatus=PENDING, 清空 last_error */
    @Transactional
    public void retryNotification(Long id) {
        jdbc.update(
                "UPDATE msg_notifications SET send_status = 'PENDING', last_error = NULL "
                + "WHERE id = ? AND send_status = 'FAILED'", id);
    }

    /** 批量重发 — 返回受影响行数 */
    @Transactional
    public int retryBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        StringBuilder sql = new StringBuilder(
                "UPDATE msg_notifications SET send_status = 'PENDING', last_error = NULL "
                + "WHERE send_status = 'FAILED' AND id IN (");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");
        return jdbc.update(sql.toString(), ids.toArray());
    }

    /** 标为永久死信 (retry_count = 99 阻止再重试) */
    @Transactional
    public void markDead(Long id) {
        jdbc.update(
                "UPDATE msg_notifications SET retry_count = 99 WHERE id = ?", id);
    }

    /** 清空死信 (软删除, 保留审计) */
    @Transactional
    public void clearDeadLetter(Long id) {
        jdbc.update("UPDATE msg_notifications SET deleted = 1 WHERE id = ?", id);
    }
}
