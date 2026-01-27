package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.export.ExportCenterService;
import com.school.management.application.inspection.export.ExportRequest;
import com.school.management.application.inspection.export.ExportResult;
import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/export-center")
@RequiredArgsConstructor
@Tag(name = "导出中心", description = "统一数据导出API")
public class ExportCenterController {

    private final ExportCenterService exportCenterService;

    @GetMapping("/scenarios")
    @Operation(summary = "获取可用导出场景")
    @PreAuthorize("hasAuthority('inspection:export')")
    public Result<List<Map<String, String>>> getScenarios() {
        return Result.success(exportCenterService.getAvailableScenarios());
    }

    @PostMapping("/export")
    @Operation(summary = "执行导出")
    @PreAuthorize("hasAuthority('inspection:export')")
    public Result<ExportResult> export(@RequestBody ExportRequest request) {
        return Result.success(exportCenterService.export(request));
    }

    @PostMapping("/estimate")
    @Operation(summary = "预估导出数据量")
    @PreAuthorize("hasAuthority('inspection:export')")
    public Result<Map<String, Object>> estimate(@RequestBody ExportRequest request) {
        long count = exportCenterService.estimateCount(request);
        return Result.success(Map.of(
                "estimatedCount", count,
                "scenario", request.getScenario().name(),
                "async", count > 2000
        ));
    }
}
