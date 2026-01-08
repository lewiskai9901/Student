package com.school.management.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Performance monitoring aspect for tracking method execution times.
 *
 * <p>Provides:
 * <ul>
 *   <li>Execution time logging for slow methods</li>
 *   <li>Aggregate statistics per method</li>
 *   <li>Configurable threshold for slow method detection</li>
 * </ul>
 */
@Slf4j
@Aspect
@Component
public class PerformanceMonitorAspect {

    @Value("${performance.monitor.enabled:true}")
    private boolean enabled;

    @Value("${performance.monitor.slow-threshold-ms:500}")
    private long slowThresholdMs;

    private final ConcurrentHashMap<String, MethodStats> methodStats = new ConcurrentHashMap<>();

    /**
     * Pointcut for all application services.
     */
    @Pointcut("within(com.school.management.application..*)")
    public void applicationServiceMethods() {}

    /**
     * Pointcut for all controllers.
     */
    @Pointcut("within(com.school.management.controller..*) || within(com.school.management.interfaces.rest..*)")
    public void controllerMethods() {}

    /**
     * Pointcut for repository operations.
     */
    @Pointcut("within(com.school.management.infrastructure.persistence..*)")
    public void repositoryMethods() {}

    /**
     * Monitors execution time of service methods.
     */
    @Around("applicationServiceMethods()")
    public Object monitorApplicationService(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorExecution(joinPoint, "SERVICE");
    }

    /**
     * Monitors execution time of controller methods.
     */
    @Around("controllerMethods()")
    public Object monitorController(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorExecution(joinPoint, "CONTROLLER");
    }

    /**
     * Monitors execution time of repository methods.
     */
    @Around("repositoryMethods()")
    public Object monitorRepository(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorExecution(joinPoint, "REPOSITORY");
    }

    private Object monitorExecution(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        if (!enabled) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        String methodName = getMethodName(joinPoint);

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            recordStats(methodName, executionTime, false);

            if (executionTime > slowThresholdMs) {
                log.warn("[SLOW {}] {}: {}ms", layer, methodName, executionTime);
            } else if (log.isDebugEnabled()) {
                log.debug("[{}] {}: {}ms", layer, methodName, executionTime);
            }

            return result;
        } catch (Throwable e) {
            long executionTime = System.currentTimeMillis() - startTime;
            recordStats(methodName, executionTime, true);
            log.error("[ERROR {}] {}: {}ms - {}", layer, methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    private String getMethodName(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }

    private void recordStats(String methodName, long executionTime, boolean isError) {
        methodStats.computeIfAbsent(methodName, k -> new MethodStats())
                   .record(executionTime, isError);
    }

    /**
     * Gets statistics for all monitored methods.
     *
     * @return map of method name to statistics
     */
    public ConcurrentHashMap<String, MethodStats> getMethodStats() {
        return methodStats;
    }

    /**
     * Resets all statistics.
     */
    public void resetStats() {
        methodStats.clear();
    }

    /**
     * Statistics for a single method.
     */
    public static class MethodStats {
        private final LongAdder invocationCount = new LongAdder();
        private final LongAdder totalTime = new LongAdder();
        private final LongAdder errorCount = new LongAdder();
        private final AtomicLong maxTime = new AtomicLong(0);
        private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);

        public void record(long executionTime, boolean isError) {
            invocationCount.increment();
            totalTime.add(executionTime);

            if (isError) {
                errorCount.increment();
            }

            // Update max time
            long currentMax;
            do {
                currentMax = maxTime.get();
            } while (executionTime > currentMax && !maxTime.compareAndSet(currentMax, executionTime));

            // Update min time
            long currentMin;
            do {
                currentMin = minTime.get();
            } while (executionTime < currentMin && !minTime.compareAndSet(currentMin, executionTime));
        }

        public long getInvocationCount() {
            return invocationCount.sum();
        }

        public long getTotalTime() {
            return totalTime.sum();
        }

        public long getErrorCount() {
            return errorCount.sum();
        }

        public long getMaxTime() {
            return maxTime.get();
        }

        public long getMinTime() {
            long min = minTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }

        public double getAverageTime() {
            long count = invocationCount.sum();
            return count > 0 ? (double) totalTime.sum() / count : 0;
        }

        @Override
        public String toString() {
            return String.format(
                "count=%d, avg=%.2fms, min=%dms, max=%dms, errors=%d",
                getInvocationCount(),
                getAverageTime(),
                getMinTime(),
                getMaxTime(),
                getErrorCount()
            );
        }
    }
}
