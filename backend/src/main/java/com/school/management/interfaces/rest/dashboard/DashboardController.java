package com.school.management.interfaces.rest.dashboard;

import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Dashboard controller providing statistics for the home page.
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard statistics API")
public class DashboardController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/statistics")
    @Operation(summary = "Get dashboard statistics")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> stats = new HashMap<>();

        try {
            Long studentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM students WHERE deleted = 0", Long.class);
            stats.put("studentCount", studentCount != null ? studentCount : 0);
        } catch (Exception e) {
            log.warn("Failed to query student count: {}", e.getMessage());
            stats.put("studentCount", 0);
        }

        try {
            Long classCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM classes WHERE deleted = 0", Long.class);
            stats.put("classCount", classCount != null ? classCount : 0);
        } catch (Exception e) {
            log.warn("Failed to query class count: {}", e.getMessage());
            stats.put("classCount", 0);
        }

        try {
            Long dormitoryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM places WHERE deleted = 0 AND place_type = 'ROOM'", Long.class);
            stats.put("dormitoryCount", dormitoryCount != null ? dormitoryCount : 0);
        } catch (Exception e) {
            log.warn("Failed to query dormitory count: {}", e.getMessage());
            stats.put("dormitoryCount", 0);
        }

        try {
            Long todayCheckCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM daily_check WHERE DATE(check_date) = CURDATE() AND deleted = 0",
                Long.class);
            stats.put("todayCheckCount", todayCheckCount != null ? todayCheckCount : 0);
        } catch (Exception e) {
            log.warn("Failed to query today check count: {}", e.getMessage());
            stats.put("todayCheckCount", 0);
        }

        stats.put("occupancyRate", 0);
        stats.put("completedChecks", 0);
        stats.put("pendingChecks", 0);
        stats.put("completionRate", 0);
        stats.put("chartData", Collections.emptyList());
        stats.put("checkCategories", Collections.emptyList());
        stats.put("recentRecords", Collections.emptyList());

        return Result.success(stats);
    }
}
