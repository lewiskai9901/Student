package com.school.management.common.audit;

import com.school.management.common.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * AOP aspect that automatically logs all write (POST/PUT/DELETE/PATCH) API requests
 * into the operation_logs table.
 *
 * GET requests are skipped to avoid overwhelming the table.
 * Auth endpoints and health checks are also skipped.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final JdbcTemplate jdbcTemplate;

    @Pointcut("within(com.school.management.interfaces.rest..*)")
    public void controllerMethods() {}

    @Around("controllerMethods()")
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // Get request info
        String method = "";
        String url = "";
        String ip = "";
        String userAgent = "";

        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                method = request.getMethod();
                url = request.getRequestURI();
                ip = getClientIp(request);
                userAgent = request.getHeader("User-Agent");
            }
        } catch (Exception ignored) {}

        // Skip GET requests, auth endpoints, and actuator (too noisy / irrelevant)
        if ("GET".equals(method)
                || url.contains("/auth/login")
                || url.contains("/auth/refresh")
                || url.contains("/actuator")) {
            return joinPoint.proceed();
        }

        // Get user info
        Long userId = null;
        String username = null;
        try {
            userId = SecurityUtils.getCurrentUserId();
            username = SecurityUtils.getCurrentUsername();
        } catch (Exception ignored) {}

        // Execute the actual controller method
        Object result = null;
        int status = 1; // 1 = success
        String errorMsg = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            status = 0; // 0 = failed
            errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }
            throw e;
        } finally {
            long executeTime = System.currentTimeMillis() - startTime;

            String module = extractModule(url);
            String operationType = mapHttpMethod(method);
            String operationName = joinPoint.getSignature().toShortString();
            int responseStatus = status == 1 ? 200 : 500;

            // Truncate user agent to fit column size
            if (userAgent != null && userAgent.length() > 500) {
                userAgent = userAgent.substring(0, 500);
            }

            try {
                jdbcTemplate.update(
                    "INSERT INTO operation_logs (user_id, username, operation_module, operation_type, " +
                    "operation_name, request_method, request_url, response_status, " +
                    "ip_address, user_agent, execute_time, status, error_message, tenant_id) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)",
                    userId, username, module, operationType,
                    operationName, method, url, responseStatus,
                    ip, userAgent, executeTime, status, errorMsg
                );
            } catch (Exception e) {
                log.warn("Failed to record operation log: {}", e.getMessage());
            }
        }
    }

    /**
     * Extract module name from URL path.
     * e.g. /api/academic/majors -> academic
     *      /api/teaching/schedules -> teaching
     *      /api/roles -> roles
     */
    private String extractModule(String url) {
        String path = url.replace("/api/", "").replace("/api", "");
        String[] parts = path.split("/");
        if (parts.length > 0 && !parts[0].isEmpty()) {
            return parts[0];
        }
        return "unknown";
    }

    private String mapHttpMethod(String method) {
        return switch (method) {
            case "POST" -> "CREATE";
            case "PUT" -> "UPDATE";
            case "DELETE" -> "DELETE";
            case "PATCH" -> "UPDATE";
            default -> method;
        };
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip;
    }
}
