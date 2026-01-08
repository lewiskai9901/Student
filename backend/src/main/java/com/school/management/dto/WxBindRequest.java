package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 微信账号绑定请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Schema(description = "微信账号绑定请求")
public class WxBindRequest {

    @NotBlank(message = "微信登录code不能为空")
    @Schema(description = "微信登录临时凭证code", required = true)
    private String code;

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "系统用户名", required = true)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "系统密码", required = true)
    private String password;

    @Schema(description = "设备信息")
    private LoginRequest.DeviceInfo deviceInfo;
}
