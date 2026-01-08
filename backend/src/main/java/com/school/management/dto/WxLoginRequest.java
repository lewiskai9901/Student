package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信小程序登录请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Schema(description = "微信小程序登录请求")
public class WxLoginRequest {

    @NotBlank(message = "微信登录code不能为空")
    @Schema(description = "微信登录临时凭证code", required = true)
    private String code;

    @Schema(description = "用户昵称(从微信获取)")
    private String nickName;

    @Schema(description = "用户头像URL(从微信获取)")
    private String avatarUrl;

    @Schema(description = "设备信息")
    private LoginRequest.DeviceInfo deviceInfo;
}
