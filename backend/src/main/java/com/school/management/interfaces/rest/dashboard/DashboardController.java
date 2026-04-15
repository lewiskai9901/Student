package com.school.management.interfaces.rest.dashboard;

import com.school.management.application.dashboard.DashboardOverviewQueryService;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Dashboard overview endpoint. 所有 SQL 在 {@link DashboardOverviewQueryService} 中按用户
 * 数据范围收敛；此层只负责 HTTP 绑定与 {@code @CasbinAccess} 门禁。
 */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard overview statistics API")
public class DashboardController {

    private final DashboardOverviewQueryService overviewService;

    @GetMapping("/overview")
    @Operation(summary = "Get dashboard overview with organization, teaching, inspection and system stats")
    @CasbinAccess(resource = "dashboard", action = "view")
    public Result<Map<String, Object>> getOverview() {
        return Result.success(overviewService.getOverview());
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get dashboard statistics (legacy)")
    @CasbinAccess(resource = "dashboard", action = "view")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(defaultValue = "7") int days) {
        return getOverview();
    }
}
