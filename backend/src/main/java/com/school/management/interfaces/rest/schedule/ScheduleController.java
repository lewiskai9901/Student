package com.school.management.interfaces.rest.schedule;

import com.school.management.application.schedule.ScheduleApplicationService;
import com.school.management.application.schedule.ScheduleExecutionEngine;
import com.school.management.application.schedule.command.CreatePolicyCommand;
import com.school.management.application.schedule.command.UpdatePolicyCommand;
import com.school.management.common.result.Result;
import com.school.management.domain.schedule.model.ScheduleExecution;
import com.school.management.domain.schedule.model.SchedulePolicy;
import com.school.management.security.JwtTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v2/schedule")
@Tag(name = "Schedule Management V4", description = "排班管理接口")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleApplicationService scheduleService;
    private final ScheduleExecutionEngine executionEngine;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/policies")
    @Operation(summary = "创建排班策略")
    @PreAuthorize("hasAuthority('schedule:policy:manage')")
    public Result<PolicyResponse> createPolicy(@Valid @RequestBody CreatePolicyRequest request) {
        CreatePolicyCommand command = CreatePolicyCommand.builder()
            .policyName(request.getPolicyName())
            .policyType(request.getPolicyType())
            .rotationAlgorithm(request.getRotationAlgorithm())
            .templateId(request.getTemplateId())
            .inspectorPool(request.getInspectorPool())
            .scheduleConfig(request.getScheduleConfig())
            .excludedDates(request.getExcludedDates())
            .createdBy(getCurrentUserId())
            .build();
        SchedulePolicy policy = scheduleService.createPolicy(command);
        return Result.success(PolicyResponse.fromDomain(policy));
    }

    @PutMapping("/policies/{id}")
    @Operation(summary = "更新排班策略")
    @PreAuthorize("hasAuthority('schedule:policy:manage')")
    public Result<PolicyResponse> updatePolicy(@PathVariable Long id, @Valid @RequestBody CreatePolicyRequest request) {
        UpdatePolicyCommand command = UpdatePolicyCommand.builder()
            .policyId(id)
            .policyName(request.getPolicyName())
            .rotationAlgorithm(request.getRotationAlgorithm())
            .inspectorPool(request.getInspectorPool())
            .scheduleConfig(request.getScheduleConfig())
            .excludedDates(request.getExcludedDates())
            .build();
        SchedulePolicy policy = scheduleService.updatePolicy(command);
        return Result.success(PolicyResponse.fromDomain(policy));
    }

    @GetMapping("/policies")
    @Operation(summary = "获取排班策略列表")
    @PreAuthorize("hasAuthority('schedule:policy:view')")
    public Result<List<PolicyResponse>> listPolicies() {
        List<SchedulePolicy> policies = scheduleService.listPolicies();
        return Result.success(policies.stream().map(PolicyResponse::fromDomain).collect(Collectors.toList()));
    }

    @GetMapping("/policies/{id}")
    @Operation(summary = "获取排班策略详情")
    @PreAuthorize("hasAuthority('schedule:policy:view')")
    public Result<PolicyResponse> getPolicy(@PathVariable Long id) {
        SchedulePolicy policy = scheduleService.getPolicy(id);
        return Result.success(PolicyResponse.fromDomain(policy));
    }

    @PutMapping("/policies/{id}/enable")
    @Operation(summary = "启用排班策略")
    @PreAuthorize("hasAuthority('schedule:policy:manage')")
    public Result<Void> enablePolicy(@PathVariable Long id) {
        scheduleService.enablePolicy(id);
        return Result.success(null);
    }

    @PutMapping("/policies/{id}/disable")
    @Operation(summary = "禁用排班策略")
    @PreAuthorize("hasAuthority('schedule:policy:manage')")
    public Result<Void> disablePolicy(@PathVariable Long id) {
        scheduleService.disablePolicy(id);
        return Result.success(null);
    }

    @DeleteMapping("/policies/{id}")
    @Operation(summary = "删除排班策略")
    @PreAuthorize("hasAuthority('schedule:policy:manage')")
    public Result<Void> deletePolicy(@PathVariable Long id) {
        scheduleService.deletePolicy(id);
        return Result.success(null);
    }

    @GetMapping("/executions")
    @Operation(summary = "获取排班执行记录")
    @PreAuthorize("hasAuthority('schedule:policy:view')")
    public Result<List<ExecutionResponse>> listExecutions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ScheduleExecution> executions = scheduleService.listExecutions(startDate, endDate);
        return Result.success(executions.stream().map(ExecutionResponse::fromDomain).collect(Collectors.toList()));
    }

    @PostMapping("/executions/trigger")
    @Operation(summary = "手动触发排班执行")
    @PreAuthorize("hasAuthority('schedule:policy:manage')")
    public Result<ExecutionResponse> triggerExecution(
            @RequestParam Long policyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        ScheduleExecution execution = executionEngine.triggerManual(policyId, date);
        return Result.success(ExecutionResponse.fromDomain(execution));
    }

    private Long getCurrentUserId() {
        return jwtTokenService.getCurrentUserId();
    }
}
