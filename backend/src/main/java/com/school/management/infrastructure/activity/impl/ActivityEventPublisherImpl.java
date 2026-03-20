package com.school.management.infrastructure.activity.impl;

import com.school.management.infrastructure.activity.ActivityEvent;
import com.school.management.infrastructure.activity.ActivityEventBuilder;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
import com.school.management.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 活动事件发布实现
 * 通过 Spring ApplicationEventPublisher 发布，由 ActivityEventListener 异步持久化
 * newEvent() 自动填充 HTTP 上下文和用户信息
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEventPublisherImpl implements ActivityEventPublisher {

    private final ApplicationEventPublisher springEventPublisher;

    @Override
    public void publish(ActivityEvent event) {
        springEventPublisher.publishEvent(event);
    }

    @Override
    public ActivityEventBuilder newEvent(String module, String resourceType, String action) {
        ActivityEvent.ActivityEventBuilder builder = ActivityEvent.builder()
                .module(module)
                .resourceType(resourceType)
                .action(action)
                .requestId(generateRequestId())
                .occurredAt(LocalDateTime.now());

        // Auto-fill user context
        fillUserContext(builder);

        // Auto-fill HTTP context
        fillHttpContext(builder);

        return new ActivityEventBuilder(this, builder);
    }

    private void fillUserContext(ActivityEvent.ActivityEventBuilder builder) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Object principal = auth.getPrincipal();
                if (principal instanceof CustomUserDetails userDetails) {
                    builder.userId(userDetails.getUserId());
                    builder.userName(userDetails.getUsername());
                } else if (principal instanceof String) {
                    builder.userName((String) principal);
                }
            }
        } catch (Exception e) {
            log.debug("填充用户上下文失败", e);
        }
    }

    private void fillHttpContext(ActivityEvent.ActivityEventBuilder builder) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                builder.httpMethod(request.getMethod());
                builder.apiEndpoint(request.getRequestURI());
                builder.sourceIp(getClientIp(request));
                builder.userAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            log.debug("填充HTTP上下文失败", e);
        }
    }

    private String generateRequestId() {
        // Try to reuse existing request ID from context
        try {
            String existing = com.school.management.infrastructure.context.RequestContextHolder.getCurrentRequestId();
            if (existing != null && !existing.isEmpty()) {
                return existing;
            }
        } catch (Exception ignored) {
        }
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
