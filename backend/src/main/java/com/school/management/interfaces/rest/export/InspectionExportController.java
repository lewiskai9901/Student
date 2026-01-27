package com.school.management.interfaces.rest.export;

import com.school.management.application.export.InspectionExportService;
import com.school.management.application.export.command.CreateExportCommand;
import com.school.management.application.export.query.ExportTaskDTO;
import com.school.management.common.result.Result;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/inspection/export")
@Tag(name = "Inspection Export V4", description = "量化检查数据导出接口")
@RequiredArgsConstructor
public class InspectionExportController {

    private final InspectionExportService exportService;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/create")
    @Operation(summary = "创建导出任务")
    @PreAuthorize("hasAuthority('inspection:export:create')")
    public Result<ExportTaskResponse> createExport(@Valid @RequestBody CreateExportRequest request) {
        CreateExportCommand command = CreateExportCommand.builder()
            .exportType(request.getExportType())
            .exportFormat(request.getExportFormat())
            .filters(request.getFilters())
            .createdBy(getCurrentUserId())
            .build();
        ExportTaskDTO task = exportService.createExportTask(command);
        return Result.success(ExportTaskResponse.fromDTO(task));
    }

    @GetMapping("/tasks")
    @Operation(summary = "获取我的导出任务列表")
    @PreAuthorize("hasAuthority('inspection:export:view')")
    public Result<List<ExportTaskResponse>> listTasks() {
        List<ExportTaskDTO> tasks = exportService.listMyTasks(getCurrentUserId());
        return Result.success(tasks.stream().map(ExportTaskResponse::fromDTO).collect(Collectors.toList()));
    }

    @GetMapping("/tasks/{taskCode}")
    @Operation(summary = "获取导出任务详情")
    @PreAuthorize("hasAuthority('inspection:export:view')")
    public Result<ExportTaskResponse> getTask(@PathVariable String taskCode) {
        ExportTaskDTO task = exportService.getTask(taskCode);
        return Result.success(ExportTaskResponse.fromDTO(task));
    }

    private Long getCurrentUserId() {
        return jwtTokenService.getCurrentUserId();
    }
}
