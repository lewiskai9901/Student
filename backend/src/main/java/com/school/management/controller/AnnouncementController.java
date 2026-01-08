package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.annotation.OperationLog;
import com.school.management.common.result.Result;
import com.school.management.entity.Announcement;
import com.school.management.service.AnnouncementService;
import com.school.management.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 公告控制器
 *
 * @author Claude Code
 * @date 2025-11-18
 */
@Tag(name = "公告管理")
@RestController
@RequestMapping("/system/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    /**
     * 分页查询公告
     */
    @Operation(summary = "分页查询公告")
    @GetMapping
    @PreAuthorize("hasAuthority('system:announcement:view')")
    public Result<IPage<Announcement>> queryPage(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String announcementType,
            @RequestParam(required = false) Integer isPublished,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        IPage<Announcement> page = announcementService.queryPage(title, announcementType, isPublished, pageNum, pageSize);
        return Result.success(page);
    }

    /**
     * 获取当前用户的公告列表
     */
    @Operation(summary = "获取用户公告列表")
    @GetMapping("/user")
    public Result<Map<String, Object>> getUserAnnouncements() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录或用户信息无效");
        }

        List<Announcement> announcements = announcementService.getUserAnnouncements(userId);
        long unreadCount = announcementService.getUnreadCount(userId);

        Map<String, Object> data = new HashMap<>();
        data.put("announcements", announcements);
        data.put("unreadCount", unreadCount);

        return Result.success(data);
    }

    /**
     * 获取公告详情
     */
    @Operation(summary = "获取公告详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:announcement:view')")
    public Result<Announcement> getById(@PathVariable Long id) {
        Announcement announcement = announcementService.getById(id);
        if (announcement == null) {
            return Result.error("公告不存在");
        }
        return Result.success(announcement);
    }

    /**
     * 创建公告
     */
    @Operation(summary = "创建公告")
    @PostMapping
    @PreAuthorize("hasAuthority('system:announcement:add')")
    @OperationLog(module = "system", type = "create", name = "创建公告")
    public Result<Long> create(@RequestBody Announcement announcement) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录或用户信息无效");
        }

        String username = SecurityUtils.getCurrentUsername();

        announcement.setPublisherId(userId);
        announcement.setPublisherName(username != null ? username : "Unknown");
        announcement.setIsPublished(0); // 默认草稿
        announcement.setIsPinned(0);
        announcement.setViewCount(0);

        boolean success = announcementService.save(announcement);
        if (success) {
            return Result.success(announcement.getId());
        }
        return Result.error("创建失败");
    }

    /**
     * 更新公告
     */
    @Operation(summary = "更新公告")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:announcement:edit')")
    @OperationLog(module = "system", type = "update", name = "更新公告")
    public Result<Void> update(@PathVariable Long id, @RequestBody Announcement announcement) {
        announcement.setId(id);
        boolean success = announcementService.updateById(announcement);
        if (success) {
            return Result.success(null);
        }
        return Result.error("更新失败");
    }

    /**
     * 发布公告
     */
    @Operation(summary = "发布公告")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('system:announcement:edit')")
    @OperationLog(module = "system", type = "update", name = "发布公告")
    public Result<Void> publish(@PathVariable Long id) {
        boolean success = announcementService.publish(id);
        if (success) {
            return Result.success(null);
        }
        return Result.error("发布失败");
    }

    /**
     * 撤回公告
     */
    @Operation(summary = "撤回公告")
    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('system:announcement:edit')")
    @OperationLog(module = "system", type = "update", name = "撤回公告")
    public Result<Void> revoke(@PathVariable Long id) {
        boolean success = announcementService.revoke(id);
        if (success) {
            return Result.success(null);
        }
        return Result.error("撤回失败");
    }

    /**
     * 置顶/取消置顶
     */
    @Operation(summary = "置顶公告")
    @PostMapping("/{id}/pin")
    @PreAuthorize("hasAuthority('system:announcement:edit')")
    @OperationLog(module = "system", type = "update", name = "置顶公告")
    public Result<Void> pin(@PathVariable Long id, @RequestParam boolean pinned) {
        boolean success = announcementService.pin(id, pinned);
        if (success) {
            return Result.success(null);
        }
        return Result.error("操作失败");
    }

    /**
     * 标记已读
     */
    @Operation(summary = "标记已读")
    @PostMapping("/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            return Result.error("未登录或用户信息无效");
        }

        boolean success = announcementService.markAsRead(id, userId);
        if (success) {
            return Result.success(null);
        }
        return Result.error("操作失败");
    }

    /**
     * 删除公告
     */
    @Operation(summary = "删除公告")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:announcement:delete')")
    @OperationLog(module = "system", type = "delete", name = "删除公告")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = announcementService.removeById(id);
        if (success) {
            return Result.success(null);
        }
        return Result.error("删除失败");
    }
}
