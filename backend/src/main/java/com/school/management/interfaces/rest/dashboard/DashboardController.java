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
 * Dashboard controller providing overview statistics for the home page.
 * Uses countSafe() to handle missing tables gracefully.
 */
@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard overview statistics API")
public class DashboardController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/overview")
    @Operation(summary = "Get dashboard overview with organization, teaching, inspection and system stats")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("organization", getOrgStats());
        result.put("teaching", getTeachingStats());
        result.put("inspection", getInspectionStats());
        result.put("system", getSystemStats());
        return Result.success(result);
    }

    private Map<String, Object> getOrgStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("orgUnitCount", countSafe(
            "SELECT COUNT(*) FROM org_units WHERE deleted = 0 AND status = 1"));
        stats.put("majorCount", countSafe(
            "SELECT COUNT(*) FROM majors WHERE deleted = 0 AND status = 1"));
        stats.put("classCount", countSafe(
            "SELECT COUNT(*) FROM classes WHERE deleted = 0 AND status = 'ACTIVE'"));
        stats.put("studentCount", countSafe(
            "SELECT COUNT(*) FROM students WHERE deleted = 0 AND student_status = 1"));
        stats.put("teacherCount", countSafe(
            "SELECT COUNT(*) FROM users WHERE deleted = 0 AND user_type_code = 'TEACHER' AND status = 1"));
        return stats;
    }

    private Map<String, Object> getTeachingStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Current semester name
        stats.put("currentSemester", stringSafe(
            "SELECT semester_code FROM semesters WHERE is_current = 1 LIMIT 1",
            "--"));

        // Get current semester id for task queries
        long semesterId = longSafe(
            "SELECT id FROM semesters WHERE is_current = 1 LIMIT 1");

        stats.put("courseCount", countSafe(
            "SELECT COUNT(*) FROM courses WHERE deleted = 0 AND status = 1"));

        if (semesterId > 0) {
            stats.put("taskCount", countSafe(
                "SELECT COUNT(*) FROM teaching_tasks WHERE deleted = 0 AND semester_id = " + semesterId));

            int totalTasks = countSafe(
                "SELECT COUNT(*) FROM teaching_tasks WHERE deleted = 0 AND task_status = 1 AND semester_id = " + semesterId);
            int scheduledTasks = countSafe(
                "SELECT COUNT(*) FROM teaching_tasks WHERE deleted = 0 AND task_status = 1 AND scheduling_status = 2 AND semester_id = " + semesterId);
            int rate = totalTasks > 0 ? (int) Math.round(scheduledTasks * 100.0 / totalTasks) : 0;
            stats.put("scheduledRate", rate);
            stats.put("unscheduledCount", totalTasks - scheduledTasks);
        } else {
            stats.put("taskCount", 0);
            stats.put("scheduledRate", 0);
            stats.put("unscheduledCount", 0);
        }

        return stats;
    }

    private Map<String, Object> getInspectionStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activeProjectCount", countSafe(
            "SELECT COUNT(*) FROM insp_projects WHERE deleted = 0 AND status = 'PUBLISHED'"));
        stats.put("pendingTaskCount", countSafe(
            "SELECT COUNT(*) FROM insp_tasks WHERE deleted = 0 AND status IN ('PENDING','CLAIMED','IN_PROGRESS')"));
        stats.put("correctiveOpenCount", countSafe(
            "SELECT COUNT(*) FROM insp_corrective_cases WHERE deleted = 0 AND status IN ('OPEN','ASSIGNED','IN_PROGRESS','SUBMITTED')"));
        return stats;
    }

    private Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", countSafe(
            "SELECT COUNT(*) FROM users WHERE deleted = 0 AND status = 1"));
        stats.put("todayLoginCount", countSafe(
            "SELECT COUNT(*) FROM users WHERE deleted = 0 AND DATE(last_login_time) = CURDATE()"));
        return stats;
    }

    // ========== Safe query helpers ==========

    private int countSafe(String sql) {
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.debug("Dashboard count query failed (table may not exist): {}", e.getMessage());
            return 0;
        }
    }

    private long longSafe(String sql) {
        try {
            Long val = jdbcTemplate.queryForObject(sql, Long.class);
            return val != null ? val : 0L;
        } catch (Exception e) {
            log.debug("Dashboard long query failed: {}", e.getMessage());
            return 0L;
        }
    }

    private String stringSafe(String sql, String defaultValue) {
        try {
            String val = jdbcTemplate.queryForObject(sql, String.class);
            return val != null ? val : defaultValue;
        } catch (Exception e) {
            log.debug("Dashboard string query failed: {}", e.getMessage());
            return defaultValue;
        }
    }

    // ========== Legacy endpoint (kept for backward compatibility) ==========

    @GetMapping("/statistics")
    @Operation(summary = "Get dashboard statistics (legacy)")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(defaultValue = "7") int days) {
        // Delegate to the new overview endpoint for backward compatibility
        return getOverview();
    }
}
