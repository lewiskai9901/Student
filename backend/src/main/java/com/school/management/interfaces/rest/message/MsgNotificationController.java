package com.school.management.interfaces.rest.message;

import com.school.management.application.message.MsgNotificationService;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.message.model.MsgNotification;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 站内消息 API（当前用户操作）
 */
@RestController
@RequestMapping("/msg/notifications")
@Tag(name = "站内消息", description = "用户站内消息查看与已读管理")
@RequiredArgsConstructor
public class MsgNotificationController {

    private final MsgNotificationService notificationService;

    @GetMapping
    @Operation(summary = "获取当前用户消息列表")
    @CasbinAccess(resource = "msg-notification", action = "view")
    public Result<PageResult<MsgNotification>> getMyNotifications(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String msgType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.success(notificationService.getMyNotifications(userId, isRead, msgType, page, size));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读消息数")
    @CasbinAccess(resource = "msg-notification", action = "view")
    public Result<Map<String, Long>> getUnreadCount() {
        Long userId = SecurityUtils.requireCurrentUserId();
        long count = notificationService.getUnreadCount(userId);
        return Result.success(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条消息已读")
    @CasbinAccess(resource = "msg-notification", action = "edit")
    public Result<Void> markRead(@PathVariable Long id) {
        Long userId = SecurityUtils.requireCurrentUserId();
        notificationService.markRead(id, userId);
        return Result.success();
    }

    @PutMapping("/read-all")
    @Operation(summary = "全部标记已读")
    @CasbinAccess(resource = "msg-notification", action = "edit")
    public Result<Void> markAllRead() {
        Long userId = SecurityUtils.requireCurrentUserId();
        notificationService.markAllRead(userId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除消息（软删除）")
    @CasbinAccess(resource = "msg-notification", action = "delete")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        Long userId = SecurityUtils.requireCurrentUserId();
        notificationService.deleteMessage(id, userId);
        return Result.success();
    }
}
