package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.dto.LoginResponse;
import com.school.management.dto.WxBindRequest;
import com.school.management.dto.WxLoginRequest;
import com.school.management.dto.WxLoginResponse;
import com.school.management.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 微信小程序认证控制器
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/miniapp/auth")
@RequiredArgsConstructor
@Tag(name = "微信小程序认证接口", description = "微信小程序登录、绑定相关接口")
public class MiniappAuthController {

    private final AuthService authService;

    /**
     * 微信小程序登录
     */
    @PostMapping("/wx-login")
    @Operation(summary = "微信登录", description = "使用微信code登录，如果未绑定则返回openId用于绑定")
    public Result<WxLoginResponse> wxLogin(@Valid @RequestBody WxLoginRequest request,
                                           HttpServletRequest httpRequest) {
        log.info("微信小程序登录请求");
        String clientIp = getClientIpAddress(httpRequest);
        WxLoginResponse response = authService.wxLogin(request, clientIp);
        return Result.success(response);
    }

    /**
     * 微信账号绑定
     */
    @PostMapping("/bind")
    @Operation(summary = "绑定系统账号", description = "将微信账号与系统账号绑定")
    public Result<LoginResponse> bindAccount(@Valid @RequestBody WxBindRequest request,
                                             HttpServletRequest httpRequest) {
        log.info("微信账号绑定请求: username={}", request.getUsername());
        String clientIp = getClientIpAddress(httpRequest);
        LoginResponse response = authService.wxBind(request, clientIp);
        return Result.success(response);
    }

    /**
     * 检查微信是否已绑定
     */
    @GetMapping("/check-binding")
    @Operation(summary = "检查绑定状态", description = "检查指定openId是否已绑定系统账号")
    public Result<Boolean> checkBinding(@RequestParam String openId) {
        boolean bound = authService.checkWxBinding(openId);
        return Result.success(bound);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0];
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
