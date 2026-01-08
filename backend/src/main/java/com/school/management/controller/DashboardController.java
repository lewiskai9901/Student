package com.school.management.controller;

import com.school.management.common.result.Result;
import com.school.management.dto.DashboardStatisticsResponse;
import com.school.management.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
@Tag(name = "仪表盘", description = "仪表盘统计数据接口")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/statistics")
    @Operation(summary = "获取仪表盘统计数据")
    public Result<DashboardStatisticsResponse> getStatistics(
        @Parameter(description = "图表数据天数（7/30/90）")
        @RequestParam(required = false, defaultValue = "7") Integer days
    ) {
        return Result.success(dashboardService.getStatistics(days));
    }
}
