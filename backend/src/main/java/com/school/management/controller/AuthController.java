package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.dto.LoginRequest;
import com.school.management.dto.LoginResponse;
import com.school.management.dto.LogoutRequest;
import com.school.management.dto.RefreshTokenRequest;
import com.school.management.dto.ProfileUpdateRequest;
import com.school.management.dto.ChangePasswordRequest;
import com.school.management.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证接口", description = "用户认证相关接口")
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户名密码登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("用户登录请求: {}", request.getUsername());

        // 获取客户端IP
        String clientIp = getClientIpAddress(httpRequest);

        LoginResponse response = authService.login(request, clientIp);
        return Result.success(response);
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("刷新令牌请求");

        LoginResponse response = authService.refreshToken(request);
        return Result.success(response);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "用户退出登录")
    public Result<Void> logout(@RequestBody LogoutRequest request, HttpServletRequest httpRequest) {
        log.info("用户退出登录");

        // 从请求头获取访问令牌
        String token = getTokenFromRequest(httpRequest);

        authService.logout(token, request);
        return Result.success();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public Result<LoginResponse.UserInfo> getCurrentUser() {
        LoginResponse.UserInfo userInfo = authService.getCurrentUserInfo();
        return Result.success(userInfo);
    }

    /**
     * 更新个人资料
     */
    @PutMapping("/profile")
    @Operation(summary = "更新个人资料", description = "更新当前登录用户的个人资料")
    public Result<Void> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        log.info("更新个人资料请求");
        authService.updateProfile(request);
        return Result.success();
    }

    /**
     * 修改密码
     */
    @PutMapping("/password")
    @Operation(summary = "修改密码", description = "修改当前登录用户的密码")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        log.info("修改密码请求");
        authService.changePassword(request);
        return Result.success();
    }

    /**
     * 从请求中获取JWT令牌
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
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