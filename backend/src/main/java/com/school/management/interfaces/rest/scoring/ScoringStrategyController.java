package com.school.management.interfaces.rest.scoring;

import com.school.management.application.scoring.ScoringStrategyApplicationService;
import com.school.management.application.scoring.dto.*;
import com.school.management.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 计分策略控制器
 */
@Tag(name = "计分策略管理", description = "计分策略的CRUD操作")
@RestController
@RequestMapping("/scoring/strategies")
@RequiredArgsConstructor
public class ScoringStrategyController {

    private final ScoringStrategyApplicationService service;

    @Operation(summary = "获取所有策略（按分类分组）")
    @GetMapping("/grouped")
    public Result<Map<String, List<ScoringStrategyDTO>>> getAllGrouped() {
        return Result.success(service.getAllGroupedByCategory());
    }

    @Operation(summary = "获取所有策略")
    @GetMapping
    public Result<List<ScoringStrategyDTO>> getAll() {
        return Result.success(service.getAll());
    }

    @Operation(summary = "按分类获取策略")
    @GetMapping("/category/{category}")
    public Result<List<ScoringStrategyDTO>> getByCategory(@PathVariable String category) {
        return Result.success(service.getByCategory(category));
    }

    @Operation(summary = "根据ID获取策略")
    @GetMapping("/{id}")
    public Result<ScoringStrategyDTO> getById(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @Operation(summary = "根据代码获取策略")
    @GetMapping("/code/{code}")
    public Result<ScoringStrategyDTO> getByCode(@PathVariable String code) {
        return Result.success(service.getByCode(code));
    }

    @Operation(summary = "创建自定义策略")
    @PostMapping
    @PreAuthorize("hasAuthority('inspection:config:edit')")
    public Result<ScoringStrategyDTO> create(@RequestBody CreateScoringStrategyCommand command) {
        return Result.success(service.create(command));
    }

    @Operation(summary = "更新策略")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inspection:config:edit')")
    public Result<ScoringStrategyDTO> update(@PathVariable Long id, @RequestBody UpdateScoringStrategyCommand command) {
        return Result.success(service.update(id, command));
    }

    @Operation(summary = "删除策略")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inspection:config:edit')")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用策略")
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('inspection:config:edit')")
    public Result<Void> toggleEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        service.toggleEnabled(id, enabled);
        return Result.success();
    }
}
