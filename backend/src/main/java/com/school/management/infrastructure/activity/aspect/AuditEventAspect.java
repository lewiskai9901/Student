package com.school.management.infrastructure.activity.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.infrastructure.activity.ActivityEvent;
import com.school.management.infrastructure.activity.ActivityEventPublisher;
import com.school.management.infrastructure.activity.annotation.AuditEvent;
import com.school.management.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * @AuditEvent 注解的 AOP 切面
 * 自动从 RequestContextHolder 提取 IP/UA/endpoint/method
 * 从 SecurityContext 提取 userId/userName
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditEventAspect {

    private final ActivityEventPublisher activityEventPublisher;
    private final ObjectMapper objectMapper;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(com.school.management.infrastructure.activity.annotation.AuditEvent)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AuditEvent annotation = method.getAnnotation(AuditEvent.class);

        if (annotation == null) {
            return joinPoint.proceed();
        }

        // Build SpEL evaluation context
        EvaluationContext spelContext = buildSpelContext(method, joinPoint.getArgs());

        ActivityEvent.ActivityEventBuilder builder = ActivityEvent.builder()
                .module(annotation.module())
                .action(annotation.action())
                .resourceType(annotation.resourceType())
                .actionLabel(annotation.label().isEmpty() ? null : annotation.label())
                .occurredAt(LocalDateTime.now());

        // Resolve SpEL expressions
        if (!annotation.resourceId().isEmpty()) {
            builder.resourceId(evaluateSpel(spelContext, annotation.resourceId()));
        }
        if (!annotation.resourceName().isEmpty()) {
            builder.resourceName(evaluateSpel(spelContext, annotation.resourceName()));
        }

        // Fill user context
        fillUserContext(builder);

        // Fill HTTP context
        fillHttpContext(builder);

        // Capture request if needed
        if (annotation.captureRequest()) {
            try {
                builder.beforeSnapshot(objectMapper.writeValueAsString(joinPoint.getArgs()));
            } catch (Exception e) {
                log.debug("序列化请求参数失败", e);
            }
        }

        Object result = null;
        try {
            result = joinPoint.proceed();
            builder.result("SUCCESS");

            // Capture response if needed
            if (annotation.captureResponse() && result != null) {
                try {
                    builder.afterSnapshot(objectMapper.writeValueAsString(result));
                } catch (Exception e) {
                    log.debug("序列化响应结果失败", e);
                }
            }
        } catch (Exception e) {
            builder.result("FAILURE");
            builder.errorMessage(e.getMessage());
            throw e;
        } finally {
            try {
                activityEventPublisher.publish(builder.build());
            } catch (Exception e) {
                log.error("发布审计事件失败", e);
            }
        }

        return result;
    }

    private EvaluationContext buildSpelContext(Method method, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] paramNames = parameterNameDiscoverer.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        return context;
    }

    private String evaluateSpel(EvaluationContext context, String expression) {
        try {
            Object value = parser.parseExpression(expression).getValue(context);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.debug("SpEL 表达式求值失败: {}", expression, e);
            return expression; // fallback to literal
        }
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
            log.debug("获取用户信息失败", e);
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
            log.debug("获取请求信息失败", e);
        }
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
