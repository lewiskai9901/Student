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

import java.util.List;
import java.util.Set;

/**
 * AOP aspect that intercepts methods annotated with @Audited and records
 * audit trail entries into the audit_trail table.
 *
 * Additionally triggers in-app notifications (written to msg_notifications)
 * for key business actions (PUBLISH, APPROVE, REJECT, ADMIT, REGISTER)
 * so the message center is automatically populated with relevant events.
 *
 * Design principles:
 * - NEVER let audit/notification failure break the business operation
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

    /** Actions that should trigger notifications to relevant users */
    private static final Set<String> NOTIFICATION_ACTIONS = Set.of(
            "PUBLISH", "APPROVE", "REJECT", "ADMIT", "REGISTER"
    );

    @Around("@annotation(audited)")
    public Object around(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        // Execute the business method first
        Object result = joinPoint.proceed();

        // Record audit trail — never let failure break the business operation
        Long operatorId = null;
        String operatorName = null;
        try {
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

        // Trigger notifications for key business actions
        if (NOTIFICATION_ACTIONS.contains(audited.action())) {
            try {
                triggerNotification(audited, operatorName);
            } catch (Exception e) {
                log.warn("Failed to trigger notification for {}.{}: {}",
                        audited.module(), audited.action(), e.getMessage());
            }
        }

        return result;
    }

    /**
     * Trigger notifications for important business actions.
     * Writes directly to msg_notifications (the table the frontend message center reads).
     * Uses role-based lookup to find admin users who should be notified.
     */
    private void triggerNotification(Audited audited, String operatorName) {
        String action = audited.action();
        String resourceType = audited.resourceType();
        String module = audited.module();
        String description = audited.description();

        String title = buildNotificationTitle(action, resourceType);
        String content = buildNotificationContent(action, resourceType, description, operatorName);
        String msgType = mapActionToMsgType(action);
        String sourceEventType = module.toUpperCase() + "_" + action;

        // Find admin users to notify
        List<Long> adminUserIds = findAdminUserIds();
        if (adminUserIds.isEmpty()) {
            return;
        }

        // Insert into msg_notifications for each admin user
        for (Long userId : adminUserIds) {
            try {
                jdbcTemplate.update(
                    "INSERT INTO msg_notifications (tenant_id, user_id, title, content, " +
                    "msg_type, source_event_type, source_ref_type, is_read) " +
                    "VALUES (1, ?, ?, ?, ?, ?, ?, 0)",
                    userId, title, content, msgType, sourceEventType, resourceType
                );
            } catch (Exception e) {
                log.warn("Failed to insert notification for user {}: {}", userId, e.getMessage());
            }
        }
        log.info("Triggered {} notification for {}.{} to {} admin(s)",
                action, module, resourceType, adminUserIds.size());
    }

    /**
     * Find admin user IDs by querying users with admin roles.
     */
    private List<Long> findAdminUserIds() {
        try {
            return jdbcTemplate.queryForList(
                "SELECT DISTINCT ur.user_id FROM user_roles ur " +
                "JOIN roles r ON ur.role_id = r.id AND r.deleted = 0 " +
                "WHERE r.role_code IN ('SUPER_ADMIN', 'ADMIN') " +
                "AND ur.deleted = 0 " +
                "LIMIT 20",
                Long.class
            );
        } catch (Exception e) {
            log.warn("Failed to query admin users for notification: {}", e.getMessage());
            return List.of();
        }
    }

    private String buildNotificationTitle(String action, String resourceType) {
        return switch (action) {
            case "PUBLISH" -> resourceType + " 已发布";
            case "APPROVE" -> resourceType + " 审批通过";
            case "REJECT" -> resourceType + " 审批拒绝";
            case "ADMIT" -> resourceType + " 已录取";
            case "REGISTER" -> resourceType + " 已报到注册";
            default -> resourceType + " " + action;
        };
    }

    private String buildNotificationContent(String action, String resourceType,
                                             String description, String operatorName) {
        String operator = operatorName != null ? operatorName : "系统";
        if (description != null && !description.isEmpty()) {
            return String.format("%s 操作: %s (操作人: %s)", action, description, operator);
        }
        return String.format("%s 对 %s 执行了 %s 操作", operator, resourceType, action);
    }

    private String mapActionToMsgType(String action) {
        return switch (action) {
            case "PUBLISH" -> "INSPECTION_PUBLISHED";
            case "APPROVE" -> "TASK_APPROVED";
            case "REJECT" -> "TASK_REJECTED";
            default -> "SYSTEM";
        };
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
