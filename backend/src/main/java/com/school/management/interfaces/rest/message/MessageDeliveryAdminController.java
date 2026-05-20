package com.school.management.interfaces.rest.message;

import com.school.management.application.message.MessageDeliveryAdminApplicationService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
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

    private final MessageDeliveryAdminApplicationService service;

    /** 列失败消息 */
    @GetMapping("/failed")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<List<Map<String, Object>>> listFailed(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String channel) {
        return Result.success(service.listFailed(limit, channel));
    }

    /** 死信列表 (retry_count >= 3 且 FAILED) */
    @GetMapping("/dead-letter")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<List<Map<String, Object>>> listDeadLetter(@RequestParam(defaultValue = "100") int limit) {
        return Result.success(service.listDeadLetter(limit));
    }

    /** 单条重发 */
    @PostMapping("/{id}/retry")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<Void> retry(@PathVariable Long id) {
        service.retryNotification(id);
        return Result.success();
    }

    /** 批量重发 */
    @PostMapping("/retry-batch")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<Map<String, Integer>> retryBatch(@RequestBody RetryBatchRequest req) {
        int updated = service.retryBatch(req.ids());
        return Result.success(Map.of("affected", updated));
    }

    /** 标为永久死信 (retry_count = 99 阻止再重试) */
    @PostMapping("/{id}/dead")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<Void> markDead(@PathVariable Long id) {
        service.markDead(id);
        return Result.success();
    }

    /** 清空死信 (软删除, 保留审计) */
    @DeleteMapping("/dead-letter/{id}")
    @CasbinAccess(resource = "system:message", action = "manage")
    public Result<Void> clearDeadLetter(@PathVariable Long id) {
        service.clearDeadLetter(id);
        return Result.success();
    }

    public record RetryBatchRequest(List<Long> ids) {}
}
