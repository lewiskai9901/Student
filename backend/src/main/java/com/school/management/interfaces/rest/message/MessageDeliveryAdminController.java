package com.school.management.interfaces.rest.message;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * S-6: 失败重发 / 死信队列管理 — 平台管理员视角.
 *
 * - GET /admin/messages/failed   列出 sendStatus=FAILED 的消息
 * - POST /admin/messages/{id}/retry  重置 sendStatus=PENDING + retryCount+1, 等下一轮 dispatcher 重发
 * - POST /admin/messages/retry-batch  批量重发
 * - DELETE /admin/messages/{id}/dead   把消息标为 DEAD (彻底死信, 不再重试)
 */
@RestController
@RequestMapping("/admin/messages")
@RequiredArgsConstructor
public class MessageDeliveryAdminController {

    private final JdbcTemplate jdbc;

    /** 列失败消息 */
    @GetMapping("/failed")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<List<Map<String, Object>>> listFailed(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String channel) {
        StringBuilder sql = new StringBuilder(
                "SELECT id, receiver_type, receiver_id, title, source_event_type, "
                + "send_status, retry_count, last_error, created_at, sent_at "
                + "FROM msg_notifications "
                + "WHERE deleted = 0 AND send_status IN ('FAILED','PENDING') ");
        if (channel != null && !channel.isBlank()) {
            sql.append("AND msg_type = ? ");
        }
        sql.append("ORDER BY created_at DESC LIMIT ?");
        return Result.success(channel != null && !channel.isBlank()
                ? jdbc.queryForList(sql.toString(), channel, limit)
                : jdbc.queryForList(sql.toString(), limit));
    }

    /** 死信列表 (retry_count >= 3 且 FAILED) */
    @GetMapping("/dead-letter")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<List<Map<String, Object>>> listDeadLetter(@RequestParam(defaultValue = "100") int limit) {
        return Result.success(jdbc.queryForList(
                "SELECT id, receiver_type, receiver_id, title, source_event_type, "
                + "retry_count, last_error, created_at "
                + "FROM msg_notifications "
                + "WHERE deleted = 0 AND send_status = 'FAILED' AND retry_count >= 3 "
                + "ORDER BY created_at DESC LIMIT ?", limit));
    }

    /** 单条重发 */
    @PostMapping("/{id}/retry")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<Void> retry(@PathVariable Long id) {
        jdbc.update(
                "UPDATE msg_notifications SET send_status = 'PENDING', last_error = NULL "
                + "WHERE id = ? AND send_status = 'FAILED'", id);
        return Result.success();
    }

    /** 批量重发 */
    @PostMapping("/retry-batch")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<Map<String, Integer>> retryBatch(@RequestBody RetryBatchRequest req) {
        if (req.ids() == null || req.ids().isEmpty()) {
            return Result.success(Map.of("affected", 0));
        }
        StringBuilder sql = new StringBuilder(
                "UPDATE msg_notifications SET send_status = 'PENDING', last_error = NULL "
                + "WHERE send_status = 'FAILED' AND id IN (");
        for (int i = 0; i < req.ids().size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");
        int updated = jdbc.update(sql.toString(), req.ids().toArray());
        return Result.success(Map.of("affected", updated));
    }

    /** 标为永久死信 (retry_count = 99 阻止再重试) */
    @PostMapping("/{id}/dead")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<Void> markDead(@PathVariable Long id) {
        jdbc.update(
                "UPDATE msg_notifications SET retry_count = 99 WHERE id = ?", id);
        return Result.success();
    }

    /** 清空死信 (软删除, 保留审计) */
    @DeleteMapping("/dead-letter/{id}")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<Void> clearDeadLetter(@PathVariable Long id) {
        jdbc.update("UPDATE msg_notifications SET deleted = 1 WHERE id = ?", id);
        return Result.success();
    }

    public record RetryBatchRequest(List<Long> ids) {}
}
