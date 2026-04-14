package com.school.management.interfaces.rest.access;

import com.school.management.common.result.Result;
import com.school.management.infrastructure.activity.annotation.AuditEvent;
import com.school.management.infrastructure.persistence.user.UserDomainMapper;
import com.school.management.infrastructure.persistence.user.UserPO;
import com.school.management.interfaces.rest.access.dto.LoginRequest;
import com.school.management.interfaces.rest.access.dto.LoginResponse;
import com.school.management.interfaces.rest.access.dto.LogoutRequest;
import com.school.management.interfaces.rest.access.dto.RefreshTokenRequest;
import com.school.management.security.CustomUserDetails;
import com.school.management.security.CustomUserDetailsService;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证接口", description = "用户认证相关接口")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService userDetailsService;
    private final UserDomainMapper userDomainMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/login")
    @Operation(summary = "用户登录")
    @AuditEvent(module = "access", action = "LOGIN", resourceType = "AUTH", label = "用户登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("用户登录请求: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            if (!userDetails.isEnabled()) {
                return Result.error("用户已被禁用");
            }

            String accessToken = jwtTokenService.generateToken(
                    userDetails.getUserId(), userDetails.getUsername(), userDetails.getRoles());
            String refreshToken = jwtTokenService.generateRefreshToken(userDetails.getUserId());

            // 更新登录信息
            try {
                UserPO update = new UserPO();
                update.setId(userDetails.getUserId());
                update.setLastLoginTime(LocalDateTime.now());
                update.setLastLoginIp(getClientIp(httpRequest));
                userDomainMapper.updateById(update);
            } catch (Exception e) {
                log.warn("更新登录信息失败: {}", e.getMessage());
            }

            UserPO user = userDomainMapper.selectById(userDetails.getUserId());
            LoginResponse response = buildLoginResponse(accessToken, refreshToken, userDetails, user);
            return Result.success(response);

        } catch (Exception e) {
            log.warn("登录失败: username={}, error={}", request.getUsername(), e.getMessage());
            return Result.error("用户名或密码错误");
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌")
    @AuditEvent(module = "access", action = "UPDATE", resourceType = "AUTH", label = "刷新令牌")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        Long userId;
        try {
            userId = jwtTokenService.getUserIdFromToken(refreshToken);
        } catch (Exception e) {
            return Result.error("刷新令牌无效");
        }

        if (!jwtTokenService.validateRefreshToken(refreshToken, userId)) {
            return Result.error("刷新令牌无效或已过期");
        }

        CustomUserDetails userDetails = userDetailsService.loadUserByUserId(userId);
        if (!userDetails.isEnabled()) {
            return Result.error("用户不存在或已被禁用");
        }

        String newAccessToken = jwtTokenService.generateToken(
                userDetails.getUserId(), userDetails.getUsername(), userDetails.getRoles());

        UserPO user = userDomainMapper.selectById(userId);
        LoginResponse response = buildLoginResponse(newAccessToken, refreshToken, userDetails, user);
        return Result.success(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    @AuditEvent(module = "access", action = "LOGOUT", resourceType = "AUTH", label = "退出登录")
    public Result<Void> logout(@RequestBody(required = false) LogoutRequest request, HttpServletRequest httpRequest) {
        String token = getTokenFromRequest(httpRequest);
        if (token != null) {
            jwtTokenService.blacklistToken(token);
        }

        if (request != null && request.getRefreshToken() != null) {
            jwtTokenService.blacklistToken(request.getRefreshToken());
        }

        if (request != null && Boolean.TRUE.equals(request.getLogoutAll())) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails ud) {
                try {
                    redisTemplate.delete("user:session:" + ud.getUserId());
                    redisTemplate.delete("refresh_token:" + ud.getUserId());
                } catch (Exception e) {
                    log.warn("清除用户会话失败: {}", e.getMessage());
                }
            }
        }

        SecurityContextHolder.clearContext();
        return Result.success();
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public Result<LoginResponse.UserInfo> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails userDetails)) {
            return Result.error("用户未登录");
        }

        UserPO user = userDomainMapper.selectById(userDetails.getUserId());
        if (user == null) {
            return Result.error("用户不存在");
        }

        LoginResponse.UserInfo userInfo = buildUserInfo(userDetails, user);
        return Result.success(userInfo);
    }

    private LoginResponse buildLoginResponse(String accessToken, String refreshToken,
                                              CustomUserDetails userDetails, UserPO user) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(7200L)
                .userInfo(buildUserInfo(userDetails, user))
                .build();
    }

    private LoginResponse.UserInfo buildUserInfo(CustomUserDetails userDetails, UserPO user) {
        return LoginResponse.UserInfo.builder()
                .userId(userDetails.getUserId())
                .username(userDetails.getUsername())
                .realName(userDetails.getRealName())
                .phone(user != null ? user.getPhone() : null)
                .email(user != null ? user.getEmail() : null)
                .avatar(user != null ? user.getAvatar() : null)
                .gender(user != null ? user.getGender() : null)
                .status(userDetails.getStatus())
                .roles(userDetails.getRoles())
                .permissions(userDetails.getPermissions())
                .orgUnitId(userDetails.getOrgUnitId())
                .tenantId(userDetails.getTenantId())
                .userTypeCode(user != null ? user.getUserTypeCode() : null)
                .build();
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
            return xff.split(",")[0];
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isEmpty() && !"unknown".equalsIgnoreCase(xri)) {
            return xri;
        }
        return request.getRemoteAddr();
    }
}
