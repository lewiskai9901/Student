package com.school.management.security;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final CustomUserDetailsService userDetailsService;

    private static final String HEADER_NAME = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getServletPath();
        log.debug("处理请求: {} {}", request.getMethod(), requestPath);

        // 跳过不需要认证的路径
        if (shouldSkipFilter(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取JWT令牌
        String token = getTokenFromRequest(request);

        if (StrUtil.isNotBlank(token) && jwtTokenService.validateToken(token)) {
            try {
                // 从令牌中获取用户ID
                Long userId = jwtTokenService.getUserIdFromToken(token);

                // 加载用户详情
                CustomUserDetails userDetails = userDetailsService.loadUserByUserId(userId);

                // 创建认证对象
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 设置到安全上下文
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("用户 {} 认证成功", userDetails.getUsername());

            } catch (Exception e) {
                log.error("JWT认证失败: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中获取JWT令牌
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_NAME);

        if (StrUtil.isNotBlank(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }

        return null;
    }

    /**
     * 判断是否应该跳过过滤器
     */
    private boolean shouldSkipFilter(String requestPath) {
        String[] skipPaths = {
                "/auth/login",
                "/auth/refresh",
                "/swagger-ui",
                "/api-docs",
                "/v3/api-docs",
                "/doc.html",
                "/webjars/",
                "/favicon.ico",
                "/actuator/health"
        };

        for (String skipPath : skipPaths) {
            if (requestPath.startsWith(skipPath)) {
                return true;
            }
        }

        return false;
    }
}