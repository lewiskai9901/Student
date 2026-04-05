package com.school.management.common.audit;

import com.school.management.common.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AOP aspect that intercepts methods annotated with @Audited and records
 * audit trail entries into the audit_trail table.
 *
 * Notifications are NOT sent from here. They should be triggered through
 * the subscription rule system (msg_subscription_rules + MessageDispatcher).
 *
 * Design principles:
 * - NEVER let audit failure break the business operation
 * - Extract operator info from SecurityUtils with fallback
 * - Extract IP from the current HTTP request with fallback
 * - Extract resource ID from the first Long/String method argument
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final JdbcTemplate jdbcTemplate;

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        // Execute the business method first
        Object result = joinPoint.proceed();

        // Record audit trail — never let failure break the business operation
        try {
            Long operatorId = null;
            String operatorName = null;
            try {
                operatorId = SecurityUtils.getCurrentUserId();
                operatorName = SecurityUtils.getCurrentUsername();
            } catch (Exception e) {
                // No auth context available — proceed without operator info
            }

            String ipAddress = null;
            try {
                ServletRequestAttributes attrs =
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    ipAddress = request.getHeader("X-Forwarded-For");
                    if (ipAddress == null || ipAddress.isBlank()) {
                        ipAddress = request.getHeader("X-Real-IP");
                    }
                    if (ipAddress == null || ipAddress.isBlank()) {
                        ipAddress = request.getRemoteAddr();
                    }
                    if (ipAddress != null && ipAddress.contains(",")) {
                        ipAddress = ipAddress.split(",")[0].trim();
                    }
                }
            } catch (Exception e) {
                // No request context — proceed without IP
            }

            String resourceId = extractResourceId(joinPoint.getArgs());

            jdbcTemplate.update(
                "INSERT INTO audit_trail (module, action, resource_type, resource_id, " +
                "operator_id, operator_name, ip_address, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                audited.module(),
                audited.action(),
                audited.resourceType(),
                resourceId,
                operatorId,
                operatorName,
                ipAddress,
                audited.description().isEmpty() ? null : audited.description()
            );
        } catch (Exception e) {
            log.warn("Failed to record audit trail for {}.{}: {}",
                    audited.module(), audited.action(), e.getMessage());
        }

        return result;
    }

    /**
     * Extract resource ID from method arguments.
     * Looks for the first Long or String argument (typically a path variable ID).
     */
    private String extractResourceId(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof Long) {
                return arg.toString();
            }
            if (arg instanceof String) {
                return (String) arg;
            }
        }
        return null;
    }
}
