package com.school.management.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.ApiResponse;
import com.school.management.entity.Announcement;
import com.school.management.entity.WechatPushRecord;
import com.school.management.mapper.WechatPushRecordMapper;
import com.school.management.service.AnnouncementService;
import com.school.management.service.WechatPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 微信推送管理控制器
 *
 * @author system
 * @since 4.5.0
 */
@Tag(name = "微信推送管理")
@RestController
@RequestMapping("/wechat/push")
@RequiredArgsConstructor
public class WechatPushController {

    private final WechatPushService wechatPushService;
    private final AnnouncementService announcementService;
    private final WechatPushRecordMapper pushRecordMapper;

    @Operation(summary = "检查推送功能状态")
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        boolean enabled = wechatPushService.isPushEnabled();
        return ApiResponse.success(Map.of(
                "enabled", enabled,
                "message", enabled ? "微信推送功能已启用" : "微信推送功能未启用，请检查配置"
        ));
    }

    @Operation(summary = "手动推送公告")
    @PostMapping("/announcement/{id}")
    @PreAuthorize("hasAuthority('wechat:push:send')")
    public ApiResponse<Map<String, Integer>> pushAnnouncement(@PathVariable Long id) {
        Announcement announcement = announcementService.getById(id);
        if (announcement == null) {
            return ApiResponse.error("公告不存在");
        }

        if (announcement.getIsPublished() != 1) {
            return ApiResponse.error("公告尚未发布，无法推送");
        }

        if (!wechatPushService.isPushEnabled()) {
            return ApiResponse.error("微信推送功能未启用");
        }

        Map<String, Integer> result = wechatPushService.pushAnnouncement(announcement);
        return ApiResponse.success("推送完成", result);
    }

    @Operation(summary = "获取推送统计")
    @GetMapping("/statistics")
    @PreAuthorize("hasAuthority('wechat:push:view')")
    public ApiResponse<Map<String, Integer>> getStatistics(
            @RequestParam String businessType,
            @RequestParam Long businessId) {
        Map<String, Integer> statistics = wechatPushService.getPushStatistics(businessType, businessId);
        return ApiResponse.success(statistics);
    }

    @Operation(summary = "分页查询推送记录")
    @GetMapping("/records")
    @PreAuthorize("hasAuthority('wechat:push:view')")
    public ApiResponse<Page<WechatPushRecord>> getRecords(
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Long businessId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {

        Page<WechatPushRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WechatPushRecord> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(StringUtils.hasText(businessType), WechatPushRecord::getBusinessType, businessType)
               .eq(businessId != null, WechatPushRecord::getBusinessId, businessId)
               .eq(userId != null, WechatPushRecord::getUserId, userId)
               .eq(StringUtils.hasText(status), WechatPushRecord::getStatus, status)
               .eq(WechatPushRecord::getDeleted, 0)
               .orderByDesc(WechatPushRecord::getCreatedAt);

        Page<WechatPushRecord> result = pushRecordMapper.selectPage(page, wrapper);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取业务的推送记录")
    @GetMapping("/records/business")
    @PreAuthorize("hasAuthority('wechat:push:view')")
    public ApiResponse<List<WechatPushRecord>> getBusinessRecords(
            @RequestParam String businessType,
            @RequestParam Long businessId) {

        List<WechatPushRecord> records = pushRecordMapper.selectByBusiness(businessType, businessId);
        return ApiResponse.success(records);
    }
}
