package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "admin")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;

    @Schema(description = "登录类型: 1-PC端 2-小程序", example = "1")
    private Integer loginType = 1;

    @Schema(description = "验证码")
    private String captcha;

    @Schema(description = "验证码Key")
    private String captchaKey;

    @Schema(description = "设备信息")
    private DeviceInfo deviceInfo;

    @Data
    @Schema(description = "设备信息")
    public static class DeviceInfo {

        @Schema(description = "设备ID")
        private String deviceId;

        @Schema(description = "平台: web/ios/android")
        private String platform;

        @Schema(description = "版本号")
        private String version;

        @Schema(description = "用户代理")
        private String userAgent;
    }
}