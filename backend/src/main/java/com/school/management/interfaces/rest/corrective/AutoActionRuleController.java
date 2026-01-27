package com.school.management.interfaces.rest.corrective;

import com.school.management.application.corrective.CorrectiveActionApplicationService;
import com.school.management.common.result.Result;
import com.school.management.domain.corrective.model.AutoActionRule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v2/corrective-actions/rules")
@Tag(name = "Auto Action Rules", description = "自动创建规则管理")
public class AutoActionRuleController {

    private final CorrectiveActionApplicationService service;

    public AutoActionRuleController(CorrectiveActionApplicationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "获取所有启用规则")
    @PreAuthorize("hasAuthority('corrective:action:view')")
    public Result<List<AutoActionRuleResponse>> list() {
        return Result.success(service.listEnabledRules().stream()
                .map(AutoActionRuleResponse::fromDomain)
                .collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取规则详情")
    @PreAuthorize("hasAuthority('corrective:action:view')")
    public Result<AutoActionRuleResponse> getById(@PathVariable Long id) {
        return service.getRule(id)
                .map(r -> Result.success(AutoActionRuleResponse.fromDomain(r)))
                .orElse(Result.error("Rule not found"));
    }

    @PostMapping
    @Operation(summary = "创建规则")
    @PreAuthorize("hasAuthority('corrective:action:create')")
    public Result<AutoActionRuleResponse> create(@RequestBody AutoActionRule rule) {
        AutoActionRule saved = service.saveRule(rule);
        return Result.success(AutoActionRuleResponse.fromDomain(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新规则")
    @PreAuthorize("hasAuthority('corrective:action:create')")
    public Result<AutoActionRuleResponse> update(@PathVariable Long id, @RequestBody AutoActionRule rule) {
        rule.setId(id);
        AutoActionRule saved = service.saveRule(rule);
        return Result.success(AutoActionRuleResponse.fromDomain(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除规则")
    @PreAuthorize("hasAuthority('corrective:action:create')")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteRule(id);
        return Result.success(null);
    }
}
