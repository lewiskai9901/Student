package com.school.management.infrastructure.context;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * 审计上下文拦截器
 * 在每次HTTP请求时自动填充请求上下文信息
 */
@Slf4j
@Component
public class AuditContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // 生成请求ID
            String requestId = UUID.randomUUID().toString();

            // 获取当前认证用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Long userId = null;
            String userName = "anonymous";

            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                // 从认证信息中提取用户ID和用户名
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
                    org.springframework.security.core.userdetails.UserDetails userDetails =
                            (org.springframework.security.core.userdetails.UserDetails) principal;
                    userName = userDetails.getUsername();

                    // 尝试从UserDetails中提取用户ID（需要自定义UserDetails实现）
                    if (principal instanceof com.school.management.security.CustomUserDetails) {
                        userId = ((com.school.management.security.CustomUserDetails) principal).getUserId();
                    }
                } else {
                    userName = authentication.getName();
                }
            }

            // 获取来源IP
            String sourceIp = getClientIpAddress(request);

            // 获取User-Agent
            String userAgent = request.getHeader("User-Agent");

            // 获取Referer
            String referer = request.getHeader("Referer");

            // 获取API端点和HTTP方法
            String apiEndpoint = request.getRequestURI();
            String httpMethod = request.getMethod();

            // 获取会话ID
            String sessionId = request.getSession(false) != null ?
                    request.getSession().getId() : null;

            // 构建请求上下文
            RequestContext context = RequestContext.builder()
                    .requestId(requestId)
                    .userId(userId)
                    .userName(userName)
                    .userType(userId != null ? "IAM_USER" : "ANONYMOUS")
                    .sessionId(sessionId)
                    .sourceIp(sourceIp)
                    .userAgent(userAgent)
                    .referer(referer)
                    .apiEndpoint(apiEndpoint)
                    .httpMethod(httpMethod)
                    .requestTime(System.currentTimeMillis())
                    .build();

            // 设置到ThreadLocal
            RequestContextHolder.setContext(context);

            log.debug("请求上下文已设置: requestId={}, userId={}, userName={}, ip={}, endpoint={}",
                    requestId, userId, userName, sourceIp, apiEndpoint);

        } catch (Exception e) {
            log.error("设置请求上下文失败", e);
            // 设置默认系统上下文
            RequestContextHolder.setContext(RequestContext.createSystemContext());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 请求结束后清除上下文（防止内存泄漏）
        try {
            RequestContextHolder.clearContext();
        } catch (Exception e) {
            log.error("清除请求上下文失败", e);
        }
    }

    /**
     * 获取客户端真实IP地址
     * 处理代理和负载均衡的情况
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_VIA");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("REMOTE_ADDR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况（取第一个）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }
}
