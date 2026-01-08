package com.school.management.controller.task;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.result.Result;
import com.school.management.dto.task.SystemMessageDTO;
import com.school.management.util.SecurityUtils;
import com.school.management.service.task.SystemMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 站内消息控制器
 */
@Tag(name = "站内消息", description = "站内消息的查询、标记已读等操作")
@RestController
@RequestMapping("/task/messages")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SystemMessageController {

    private final SystemMessageService systemMessageService;

    @Operation(summary = "分页查询消息")
    @GetMapping
    public Result<IPage<SystemMessageDTO>> pageQuery(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) Integer isRead) {
        Long userId = SecurityUtils.getCurrentUserId();
        IPage<SystemMessageDTO> page = systemMessageService.pageQuery(pageNum, pageSize, userId, messageType, isRead);
        return Result.success(page);
    }

    @Operation(summary = "获取未读消息数量")
    @GetMapping("/unread-count")
    public Result<Map<String, Long>> getUnreadCount() {
        Long userId = SecurityUtils.getCurrentUserId();
        Long count = systemMessageService.countUnread(userId);
        Map<String, Long> result = new HashMap<>();
        result.put("count", count);
        return Result.success(result);
    }

    @Operation(summary = "标记消息为已读")
    @PostMapping("/{id}/read")
    public Result<Boolean> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean success = systemMessageService.markAsRead(id, userId);
        return Result.success(success);
    }

    @Operation(summary = "标记所有消息为已读")
    @PostMapping("/read-all")
    public Result<Integer> markAllAsRead() {
        Long userId = SecurityUtils.getCurrentUserId();
        int count = systemMessageService.markAllAsRead(userId);
        return Result.success(count);
    }

    @Operation(summary = "删除消息")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteMessage(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean success = systemMessageService.deleteMessage(id, userId);
        return Result.success(success);
    }
}
