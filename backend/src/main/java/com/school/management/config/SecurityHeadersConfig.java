package com.school.management.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;

/**
 * 安全响应头配置
 * 添加必要的HTTP安全头，防止XSS、点击劫持等安全威胁
 *
 * @author system
 * @since 1.0.0
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersConfig implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // X-Content-Type-Options: 防止MIME类型嗅探
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // X-Frame-Options: 防止点击劫持攻击
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // X-XSS-Protection: 启用浏览器XSS过滤器
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // Strict-Transport-Security: 强制使用HTTPS（生产环境）
        // 注意: 仅在HTTPS环境下启用
        // httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Content-Security-Policy: 内容安全策略
        httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self' data:; " +
                "connect-src 'self' http://localhost:* https:;");

        // Referrer-Policy: 控制Referer头信息
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions-Policy: 控制浏览器功能权限
        httpResponse.setHeader("Permissions-Policy",
                "geolocation=(), microphone=(), camera=()");

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // 初始化操作（如果需要）
    }

    @Override
    public void destroy() {
        // 清理操作（如果需要）
    }
}
