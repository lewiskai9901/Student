package com.school.management.controller;

import com.school.management.common.ApiResponse;
import com.school.management.service.WechatConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 微信配置管理控制器
 *
 * @author system
 * @since 4.5.0
 */
@Tag(name = "微信配置管理")
@RestController
@RequestMapping("/wechat/config")
@RequiredArgsConstructor
public class WechatConfigController {

    private final WechatConfigService wechatConfigService;

    @Operation(summary = "获取微信配置")
    @GetMapping
    @PreAuthorize("hasAuthority('system:config:view')")
    public ApiResponse<Map<String, String>> getConfigs() {
        Map<String, String> configs = wechatConfigService.getAllConfigs();
        return ApiResponse.success(configs);
    }

    @Operation(summary = "更新微信配置")
    @PutMapping
    @PreAuthorize("hasAuthority('system:config:edit')")
    public ApiResponse<Void> updateConfigs(@RequestBody Map<String, String> configs) {
        boolean success = wechatConfigService.updateConfigs(configs);
        if (success) {
            return ApiResponse.success("配置更新成功", null);
        } else {
            return ApiResponse.error("配置更新失败");
        }
    }

    @Operation(summary = "检查微信配置状态")
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getStatus() {
        boolean configured = wechatConfigService.isConfigured();
        boolean templateEnabled = wechatConfigService.isTemplateEnabled();

        return ApiResponse.success(Map.of(
                "configured", configured,
                "templateEnabled", templateEnabled,
                "message", configured
                        ? (templateEnabled ? "微信推送功能已启用" : "微信已配置，推送功能未启用")
                        : "微信配置不完整，请先配置AppID和AppSecret"
        ));
    }

    @Operation(summary = "刷新配置缓存")
    @PostMapping("/refresh")
    @PreAuthorize("hasAuthority('system:config:edit')")
    public ApiResponse<Void> refreshCache() {
        wechatConfigService.refreshCache();
        return ApiResponse.success("缓存刷新成功", null);
    }
}
