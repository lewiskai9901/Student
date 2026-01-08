package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 退出登录请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Schema(description = "退出登录请求")
public class LogoutRequest {

    @Schema(description = "刷新令牌")
    private String refreshToken;

    @Schema(description = "是否退出所有设备")
    private Boolean logoutAll = false;
}