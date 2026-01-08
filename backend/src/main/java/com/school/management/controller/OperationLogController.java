package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.annotation.OperationLog;
import com.school.management.common.result.Result;
import com.school.management.dto.OperationLogQueryRequest;
import com.school.management.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志控制器
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/system/operation-logs")
@RequiredArgsConstructor
@Tag(name = "操作日志管理", description = "操作日志管理相关接口")
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志
     */
    @GetMapping
    @Operation(summary = "分页查询操作日志", description = "分页查询操作日志列表")
    @PreAuthorize("hasAuthority('system:operlog:view')")
    @OperationLog(module = "system", type = "query", name = "查询操作日志")
    public Result<IPage<com.school.management.entity.OperationLog>> queryPage(@Valid OperationLogQueryRequest request) {
        log.info("分页查询操作日志: page={}, size={}", request.getPageNum(), request.getPageSize());
        IPage<com.school.management.entity.OperationLog> page = operationLogService.queryPage(request);
        return Result.success(page);
    }

    /**
     * 根据ID查询操作日志
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询操作日志详情", description = "根据ID查询操作日志详情")
    @PreAuthorize("hasAuthority('system:operlog:view')")
    public Result<com.school.management.entity.OperationLog> getById(
            @Parameter(description = "日志ID") @PathVariable Long id) {
        log.info("查询操作日志详情: {}", id);
        com.school.management.entity.OperationLog operationLog = operationLogService.getById(id);
        return Result.success(operationLog);
    }

    /**
     * 删除操作日志
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除操作日志", description = "删除指定操作日志")
    @PreAuthorize("hasAuthority('system:operlog:delete')")
    @OperationLog(module = "system", type = "delete", name = "删除操作日志")
    public Result<Void> deleteById(@Parameter(description = "日志ID") @PathVariable Long id) {
        log.info("删除操作日志: {}", id);
        operationLogService.deleteById(id);
        return Result.success();
    }

    /**
     * 批量删除操作日志
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除操作日志", description = "批量删除多个操作日志")
    @PreAuthorize("hasAuthority('system:operlog:delete')")
    @OperationLog(module = "system", type = "delete", name = "批量删除操作日志")
    public Result<Void> deleteBatch(@RequestBody List<Long> ids) {
        log.info("批量删除操作日志: count={}", ids.size());
        operationLogService.deleteBatch(ids);
        return Result.success();
    }

    /**
     * 清空操作日志
     */
    @DeleteMapping("/clear")
    @Operation(summary = "清空操作日志", description = "清空指定日期之前的操作日志")
    @PreAuthorize("hasAuthority('system:operlog:clear')")
    @OperationLog(module = "system", type = "delete", name = "清空操作日志")
    public Result<Void> clearLogs(
            @Parameter(description = "清空此日期之前的日志")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeDate) {
        log.info("清空操作日志: beforeDate={}", beforeDate);
        operationLogService.clearLogs(beforeDate);
        return Result.success();
    }
}
