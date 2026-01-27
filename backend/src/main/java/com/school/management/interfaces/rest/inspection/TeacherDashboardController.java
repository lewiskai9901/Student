package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.TeacherDashboardQueryService;
import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST controller for teacher dashboard data.
 */
@RestController
@RequestMapping("/teacher-dashboard")
@Tag(name = "Teacher Dashboard", description = "Teacher dashboard query API")
public class TeacherDashboardController {

    private final TeacherDashboardQueryService dashboardService;

    public TeacherDashboardController(TeacherDashboardQueryService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/overview")
    @Operation(summary = "Get class overview for teacher dashboard")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<Map<String, Object>> getOverview(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekEnd) {
        return Result.success(dashboardService.getOverview(classId, weekStart, weekEnd));
    }

    @GetMapping("/deductions")
    @Operation(summary = "Get deduction details for a class")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<List<Map<String, Object>>> getDeductions(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(dashboardService.getDeductionDetails(classId, startDate, endDate));
    }

    @GetMapping("/top-issues")
    @Operation(summary = "Get top N frequent issues for a class")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<List<Map<String, Object>>> getTopIssues(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "5") int topN) {
        return Result.success(dashboardService.getTopIssues(classId, startDate, endDate, topN));
    }

    @GetMapping("/students/violations")
    @Operation(summary = "Get student violation rankings for a class")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<List<Map<String, Object>>> getStudentViolations(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.success(dashboardService.getStudentViolations(classId, startDate, endDate));
    }

    @GetMapping("/improvement")
    @Operation(summary = "Get improvement data comparing two periods")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<Map<String, Object>> getImprovement(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate currentStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate currentEnd,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate previousStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate previousEnd) {
        return Result.success(dashboardService.getImprovement(classId, currentStart, currentEnd, previousStart, previousEnd));
    }
}
