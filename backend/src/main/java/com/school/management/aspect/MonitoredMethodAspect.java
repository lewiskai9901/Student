package com.school.management.aspect;

import com.school.management.annotation.Monitored;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Aspect for handling @Monitored annotated methods.
 *
 * <p>Provides fine-grained control over performance monitoring through annotation.
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class MonitoredMethodAspect {

    @Around("@annotation(com.school.management.annotation.Monitored)")
    public Object monitorMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Monitored annotation = method.getAnnotation(Monitored.class);

        String methodName = signature.getDeclaringType().getSimpleName() + "." + method.getName();
        String description = annotation.description().isEmpty() ? methodName : annotation.description();
        long threshold = annotation.threshold();

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > threshold) {
                StringBuilder logMsg = new StringBuilder();
                logMsg.append("[SLOW] ").append(description).append(": ").append(executionTime).append("ms");
                logMsg.append(" (threshold: ").append(threshold).append("ms)");

                if (annotation.includeArgs()) {
                    logMsg.append(" args=").append(Arrays.toString(joinPoint.getArgs()));
                }

                if (annotation.includeResult() && result != null) {
                    logMsg.append(" result=").append(result.getClass().getSimpleName());
                }

                log.warn(logMsg.toString());
            } else if (log.isDebugEnabled()) {
                log.debug("[OK] {}: {}ms", description, executionTime);
            }

            return result;
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[ERROR] {}: {}ms - {}", description, executionTime, e.getMessage());
            throw e;
        }
    }

    @Around("@within(com.school.management.annotation.Monitored)")
    public Object monitorClass(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> clazz = signature.getDeclaringType();
        Monitored annotation = clazz.getAnnotation(Monitored.class);

        // Skip if method has its own @Monitored annotation
        Method method = signature.getMethod();
        if (method.isAnnotationPresent(Monitored.class)) {
            return joinPoint.proceed();
        }

        String methodName = clazz.getSimpleName() + "." + method.getName();
        long threshold = annotation.threshold();

        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime > threshold) {
                log.warn("[SLOW] {}: {}ms (threshold: {}ms)", methodName, executionTime, threshold);
            }

            return result;
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("[ERROR] {}: {}ms - {}", methodName, executionTime, e.getMessage());
            throw e;
        }
    }
}
