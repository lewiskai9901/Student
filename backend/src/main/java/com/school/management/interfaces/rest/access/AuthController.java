package com.school.management.interfaces.rest.access;

import com.school.management.application.access.AuthJdbcApplicationService;
import com.school.management.common.annotation.PublicEndpoint;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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
    private final AuthJdbcApplicationService authJdbcService;
    private final com.school.management.infrastructure.extension.TenantPluginService tenantPluginService;
    private final com.school.management.security.LoginAttemptService loginAttemptService;

    /** 是否信任反向代理头 (X-Forwarded-For/X-Real-IP) — 默认 false, 部署在 nginx 后由 ops 置 true. */
    @org.springframework.beans.factory.annotation.Value("${security.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    @PostMapping("/login")
    @PublicEndpoint(reason = "登录无需 auth")
    @Operation(summary = "用户登录")
    @AuditEvent(module = "access", action = "LOGIN", resourceType = "AUTH", label = "用户登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        log.info("用户登录请求: {}", request.getUsername());

        // 防暴力破解: 连续失败达阈值即锁定
        if (loginAttemptService.isLocked(request.getUsername())) {
            log.warn("登录被拒 — 账户锁定中: username={}", request.getUsername());
            return Result.error("账户已锁定，请 " + loginAttemptService.lockMinutes() + " 分钟后再试");
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            // 凭据校验通过 — 清零失败计数
            loginAttemptService.reset(request.getUsername());

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
            // 凭据错误 — 记一次失败计数
            loginAttemptService.recordFailure(request.getUsername());
            log.warn("登录失败: username={}, error={}", request.getUsername(), e.getMessage());
            return Result.error("用户名或密码错误");
        }
    }

    @PostMapping("/refresh")
    @PublicEndpoint(reason = "刷新 token 用 refresh token 验证, 不需要 access token")
    @Operation(summary = "刷新令牌")
    @AuditEvent(module = "access", action = "UPDATE", resourceType = "AUTH", label = "刷新令牌")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        // 统一错误文案 — 不区分"令牌无效"/"已过期"/"用户不存在", 防用户枚举.
        final String genericError = "刷新令牌无效或已过期";

        Long userId;
        try {
            userId = jwtTokenService.getUserIdFromToken(refreshToken);
        } catch (Exception e) {
            return Result.error(genericError);
        }

        if (!jwtTokenService.validateRefreshToken(refreshToken, userId)) {
            return Result.error(genericError);
        }

        CustomUserDetails userDetails = userDetailsService.loadUserByUserId(userId);
        if (!userDetails.isEnabled()) {
            return Result.error(genericError);
        }

        String newAccessToken = jwtTokenService.generateToken(
                userDetails.getUserId(), userDetails.getUsername(), userDetails.getRoles());

        UserPO user = userDomainMapper.selectById(userId);
        LoginResponse response = buildLoginResponse(newAccessToken, refreshToken, userDetails, user);
        return Result.success(response);
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")  // Phase 6.7: logout 必须登录态
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

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails ud) {
            // 始终吊销服务端 refresh token — 防登出后 refresh token 仍能换新 access token.
            jwtTokenService.revokeAllTokensForUser(ud.getUserId());
            if (request != null && Boolean.TRUE.equals(request.getLogoutAll())) {
                try {
                    redisTemplate.delete("user:session:" + ud.getUserId());
                } catch (Exception e) {
                    log.warn("清除用户会话失败: {}", e.getMessage());
                }
            }
        }

        SecurityContextHolder.clearContext();
        return Result.success();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")  // Phase 6.7: /me 必须登录态
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
        java.util.Set<String> enabledPlugins = userDetails.getTenantId() != null
                ? tenantPluginService.enabledPlugins(userDetails.getTenantId())
                : java.util.Set.of();
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(7200L)
                .enabledPlugins(new java.util.ArrayList<>(enabledPlugins))
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
                .roleDetails(authJdbcService.loadRoleDetails(userDetails.getUserId()))
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
        // 仅在显式信任反向代理时才采信 X-Forwarded-For / X-Real-IP —
        // 否则客户端可随意伪造这些头污染 lastLoginIp.
        if (trustProxyHeaders) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
                return xff.split(",")[0].trim();
            }
            String xri = request.getHeader("X-Real-IP");
            if (xri != null && !xri.isEmpty() && !"unknown".equalsIgnoreCase(xri)) {
                return xri;
            }
        }
        return request.getRemoteAddr();
    }
}
