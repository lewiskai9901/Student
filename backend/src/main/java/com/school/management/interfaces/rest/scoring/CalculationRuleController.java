package com.school.management.interfaces.rest.scoring;

import com.school.management.application.scoring.CalculationRuleApplicationService;
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
 * 计算规则控制器
 */
@Tag(name = "计算规则管理", description = "计算规则的CRUD操作")
@RestController
@RequestMapping("/scoring/rules")
@RequiredArgsConstructor
public class CalculationRuleController {

    private final CalculationRuleApplicationService service;

    @Operation(summary = "获取所有规则（按类型分组）")
    @GetMapping("/grouped")
    public Result<Map<String, List<CalculationRuleDTO>>> getAllGrouped() {
        return Result.success(service.getAllGroupedByType());
    }

    @Operation(summary = "获取所有规则")
    @GetMapping
    public Result<List<CalculationRuleDTO>> getAll() {
        return Result.success(service.getAll());
    }

    @Operation(summary = "按类型获取规则")
    @GetMapping("/type/{ruleType}")
    public Result<List<CalculationRuleDTO>> getByRuleType(@PathVariable String ruleType) {
        return Result.success(service.getByRuleType(ruleType));
    }

    @Operation(summary = "根据ID获取规则")
    @GetMapping("/{id}")
    public Result<CalculationRuleDTO> getById(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @Operation(summary = "根据代码获取规则")
    @GetMapping("/code/{code}")
    public Result<CalculationRuleDTO> getByCode(@PathVariable String code) {
        return Result.success(service.getByCode(code));
    }

    @Operation(summary = "创建自定义规则")
    @PostMapping
    @PreAuthorize("hasAuthority('inspection:config:edit')")
    public Result<CalculationRuleDTO> create(@RequestBody CreateCalculationRuleCommand command) {
        return Result.success(service.create(command));
    }

    @Operation(summary = "更新规则")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inspection:config:edit')")
    public Result<CalculationRuleDTO> update(@PathVariable Long id, @RequestBody UpdateCalculationRuleCommand command) {
        return Result.success(service.update(id, command));
    }

    @Operation(summary = "删除规则")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inspection:config:edit')")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }

    @Operation(summary = "启用/禁用规则")
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('inspection:config:edit')")
    public Result<Void> toggleEnabled(@PathVariable Long id, @RequestParam boolean enabled) {
        service.toggleEnabled(id, enabled);
        return Result.success();
    }

    @Operation(summary = "调整规则优先级")
    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasAuthority('inspection:config:edit')")
    public Result<Void> updatePriority(@PathVariable Long id, @RequestParam Integer priority) {
        service.updatePriority(id, priority);
        return Result.success();
    }
}
