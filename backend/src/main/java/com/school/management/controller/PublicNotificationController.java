package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.entity.NotificationRecord;
import com.school.management.service.NotificationReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 公开通报接口（无需登录）
 *
 * @author system
 * @since 4.3.0
 */
@Slf4j
@RestController
@RequestMapping("/public/notifications")
@RequiredArgsConstructor
@Tag(name = "公开通报接口", description = "无需登录即可访问的通报接口，用于分享查看")
public class PublicNotificationController {

    private final NotificationReportService notificationReportService;

    @GetMapping("/{id}")
    @Operation(summary = "获取通报详情", description = "公开获取通报详情，用于分享链接查看")
    public Result<NotificationRecord> getNotification(@PathVariable Long id) {
        log.info("公开访问通报: id={}", id);

        NotificationRecord record = notificationReportService.getNotificationById(id);

        if (record == null) {
            return Result.error("通报不存在");
        }

        // 只返回已发布的通报，草稿不对外公开
        if (record.getPublishStatus() == null || record.getPublishStatus() != NotificationRecord.PUBLISH_STATUS_PUBLISHED) {
            return Result.error("该通报尚未发布");
        }

        return Result.success(record);
    }
}
