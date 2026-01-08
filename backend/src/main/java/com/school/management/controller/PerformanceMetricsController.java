package com.school.management.controller;

import com.school.management.aspect.PerformanceMonitorAspect;
import com.school.management.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for performance metrics.
 */
@RestController
@RequestMapping("/api/v2/performance")
@RequiredArgsConstructor
@Tag(name = "Performance Metrics", description = "Performance monitoring and metrics API")
public class PerformanceMetricsController {

    private final PerformanceMonitorAspect performanceMonitor;

    @GetMapping("/stats")
    @Operation(summary = "Get all method statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getStats() {
        var stats = performanceMonitor.getMethodStats();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalMethods", stats.size());

        // Sort by average time descending (slowest first)
        var sortedStats = stats.entrySet().stream()
            .sorted((a, b) -> Double.compare(
                b.getValue().getAverageTime(),
                a.getValue().getAverageTime()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> Map.of(
                    "count", e.getValue().getInvocationCount(),
                    "avgTimeMs", String.format("%.2f", e.getValue().getAverageTime()),
                    "minTimeMs", e.getValue().getMinTime(),
                    "maxTimeMs", e.getValue().getMaxTime(),
                    "errors", e.getValue().getErrorCount()
                ),
                (a, b) -> a,
                LinkedHashMap::new
            ));

        result.put("methods", sortedStats);

        return Result.success(result);
    }

    @GetMapping("/stats/slow")
    @Operation(summary = "Get slow method statistics (avg > 100ms)")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getSlowStats() {
        var stats = performanceMonitor.getMethodStats();

        var slowMethods = stats.entrySet().stream()
            .filter(e -> e.getValue().getAverageTime() > 100)
            .sorted((a, b) -> Double.compare(
                b.getValue().getAverageTime(),
                a.getValue().getAverageTime()))
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> Map.of(
                    "count", e.getValue().getInvocationCount(),
                    "avgTimeMs", String.format("%.2f", e.getValue().getAverageTime()),
                    "maxTimeMs", e.getValue().getMaxTime(),
                    "errors", e.getValue().getErrorCount()
                ),
                (a, b) -> a,
                LinkedHashMap::new
            ));

        return Result.success(Map.of(
            "slowMethodCount", slowMethods.size(),
            "methods", slowMethods
        ));
    }

    @PostMapping("/stats/reset")
    @Operation(summary = "Reset all statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> resetStats() {
        performanceMonitor.resetStats();
        return Result.success();
    }

    @GetMapping("/health")
    @Operation(summary = "Get system health summary")
    public Result<Map<String, Object>> getHealth() {
        var stats = performanceMonitor.getMethodStats();

        long totalInvocations = stats.values().stream()
            .mapToLong(PerformanceMonitorAspect.MethodStats::getInvocationCount)
            .sum();

        long totalErrors = stats.values().stream()
            .mapToLong(PerformanceMonitorAspect.MethodStats::getErrorCount)
            .sum();

        double errorRate = totalInvocations > 0
            ? (double) totalErrors / totalInvocations * 100
            : 0;

        long slowMethodCount = stats.values().stream()
            .filter(s -> s.getAverageTime() > 100)
            .count();

        return Result.success(Map.of(
            "totalInvocations", totalInvocations,
            "totalErrors", totalErrors,
            "errorRate", String.format("%.2f%%", errorRate),
            "monitoredMethods", stats.size(),
            "slowMethods", slowMethodCount,
            "status", errorRate < 5 && slowMethodCount < 10 ? "HEALTHY" : "DEGRADED"
        ));
    }
}
