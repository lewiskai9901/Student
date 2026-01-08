package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.entity.NotificationRecord;
import com.school.management.service.NotificationReportService;
import com.school.management.service.WechatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 微信接口控制器
 *
 * @author system
 * @since 4.3.0
 */
@Slf4j
@RestController
@RequestMapping("/wechat")
@RequiredArgsConstructor
@Tag(name = "微信接口", description = "微信分享和公众号接口")
public class WechatController {

    private final WechatService wechatService;
    private final NotificationReportService notificationReportService;

    @PostMapping("/js-config")
    @Operation(summary = "获取微信JS-SDK配置", description = "获取用于分享等功能的JS-SDK配置")
    public Result<Map<String, Object>> getJsConfig(@RequestBody JsConfigRequest request) {
        log.info("获取微信JS-SDK配置: url={}", request.getUrl());
        Map<String, Object> config = wechatService.getJsConfig(request.getUrl());
        return Result.success(config);
    }

    @GetMapping("/share-info/{notificationId}")
    @Operation(summary = "获取分享信息", description = "获取通报分享的标题、描述、链接等信息")
    public Result<ShareInfo> getShareInfo(@PathVariable Long notificationId) {
        log.info("获取分享信息: notificationId={}", notificationId);

        NotificationRecord record = notificationReportService.getNotificationById(notificationId);

        ShareInfo shareInfo = new ShareInfo();
        shareInfo.setTitle(record.getTitle() != null ? record.getTitle() : "量化检查通报");
        shareInfo.setDesc(String.format("涉及%d人，%d个班级", record.getTotalCount(), record.getTotalClasses()));
        shareInfo.setLink(wechatService.getShareUrl(notificationId));
        shareInfo.setImgUrl(""); // 可配置默认分享图片

        return Result.success(shareInfo);
    }

    @Data
    public static class JsConfigRequest {
        private String url;
    }

    @Data
    public static class ShareInfo {
        private String title;
        private String desc;
        private String link;
        private String imgUrl;
    }
}
