package com.school.management.infrastructure.logging;

import com.school.management.common.util.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Phase 8.2: MDC 上下文注入 — 给每个 HTTP 请求挂上 traceId / userId / requestUri,
 * 供 logback-spring.xml 的 encoder 附加到每行 log.
 *
 * 顺序 (Order HIGHEST_PRECEDENCE+10): 早于 JwtAuthenticationFilter, 先写 traceId 覆盖整条链;
 * 响应前尝试补 userId (此时 SecurityContext 已由 JwtFilter 填).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class MdcContextFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String USER_ID = "userId";
    public static final String REQUEST_URI = "requestUri";
    public static final String REQUEST_METHOD = "requestMethod";
    public static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String incoming = request.getHeader(TRACE_HEADER);
        String traceId = (incoming != null && !incoming.isBlank())
            ? incoming
            : UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        MDC.put(TRACE_ID, traceId);
        MDC.put(REQUEST_URI, request.getRequestURI());
        MDC.put(REQUEST_METHOD, request.getMethod());
        response.setHeader(TRACE_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
            try {
                Long userId = SecurityUtils.getCurrentUserId();
                if (userId != null) MDC.put(USER_ID, userId.toString());
            } catch (Exception ignored) {
                // 静态资源 / 未登录路径, 不补 userId
            }
        } finally {
            MDC.clear();
        }
    }
}
