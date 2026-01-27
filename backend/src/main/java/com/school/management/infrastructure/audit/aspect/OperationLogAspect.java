package com.school.management.infrastructure.audit.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.infrastructure.audit.AuditLog;
import com.school.management.infrastructure.audit.AuditLogService;
import com.school.management.infrastructure.audit.annotation.OperationLog;
import com.school.management.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志AOP切面 (DDD基础设施层)
 */
@Slf4j
@Aspect
@Component("dddOperationLogAspect")
@RequiredArgsConstructor
public class OperationLogAspect {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Pointcut("@annotation(com.school.management.infrastructure.audit.annotation.OperationLog)")
    public void operationLogPointcut() {
    }

    @Around("operationLogPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        OperationLog annotation = method.getAnnotation(OperationLog.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        AuditLog.AuditAction action = mapOperationType(annotation.type());
        String description = annotation.name();

        AuditLog.AuditLogBuilder logBuilder = AuditLog.builder()
                .action(action)
                .resourceType(annotation.module())
                .description(description)
                .occurredAt(LocalDateTime.now());

        // User info
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof CustomUserDetails userDetails) {
                    logBuilder.userId(userDetails.getUserId());
                    logBuilder.username(userDetails.getUsername());
                } else if (principal instanceof String) {
                    logBuilder.username((String) principal);
                }
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败", e);
        }

        // Request info
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                logBuilder.requestMethod(request.getMethod());
                logBuilder.requestUri(request.getRequestURI());
                logBuilder.ipAddress(getClientIp(request));
                logBuilder.userAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            log.warn("获取请求信息失败", e);
        }

        Object result = null;
        try {
            result = joinPoint.proceed();
            logBuilder.result(AuditLog.AuditResult.SUCCESS);
        } catch (Exception e) {
            logBuilder.result(AuditLog.AuditResult.FAILURE);
            logBuilder.errorMessage(e.getMessage());
            throw e;
        } finally {
            try {
                auditLogService.log(logBuilder.build());
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }

        return result;
    }

    private AuditLog.AuditAction mapOperationType(String type) {
        if (type == null) return AuditLog.AuditAction.READ;
        return switch (type.toUpperCase()) {
            case "CREATE", "ADD" -> AuditLog.AuditAction.CREATE;
            case "UPDATE", "EDIT", "MODIFY" -> AuditLog.AuditAction.UPDATE;
            case "DELETE", "REMOVE" -> AuditLog.AuditAction.DELETE;
            case "EXPORT" -> AuditLog.AuditAction.EXPORT;
            case "IMPORT" -> AuditLog.AuditAction.IMPORT;
            case "APPROVE" -> AuditLog.AuditAction.APPROVE;
            case "REJECT" -> AuditLog.AuditAction.REJECT;
            case "ASSIGN" -> AuditLog.AuditAction.ASSIGN;
            default -> AuditLog.AuditAction.READ;
        };
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
