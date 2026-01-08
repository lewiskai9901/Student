package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信登录响应DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "微信登录响应")
public class WxLoginResponse {

    @Schema(description = "是否已绑定系统账号")
    private Boolean bound;

    @Schema(description = "微信OpenID(未绑定时返回，用于后续绑定)")
    private String openId;

    @Schema(description = "登录响应(已绑定时返回)")
    private LoginResponse loginResponse;

    /**
     * 创建未绑定响应
     */
    public static WxLoginResponse notBound(String openId) {
        return WxLoginResponse.builder()
                .bound(false)
                .openId(openId)
                .build();
    }

    /**
     * 创建已绑定响应
     */
    public static WxLoginResponse bound(LoginResponse loginResponse) {
        return WxLoginResponse.builder()
                .bound(true)
                .loginResponse(loginResponse)
                .build();
    }
}
