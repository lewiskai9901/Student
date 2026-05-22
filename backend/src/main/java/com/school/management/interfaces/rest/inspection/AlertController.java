package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.AlertApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.analytics.Alert;
import com.school.management.domain.inspection.model.analytics.AlertRule;
import com.school.management.infrastructure.casbin.CasbinAccess;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/inspection")
public class AlertController {

    private final AlertApplicationService alertService;

    // ========== Alert Rules ==========

    // TODO: alert_rules 为低基数配置表 (规则通常 < 100 条), 暂保留全量返回.
    //       若未来规则量增长, 改为分页. 调用方: 前端告警规则配置页.
    @GetMapping("/alert-rules")
    @CasbinAccess(resource = "insp:alert", action = "view")
    public Result<List<AlertRule>> listAlertRules() {
        return Result.success(alertService.getAlertRules());
    }

    @GetMapping("/alert-rules/{id}")
    @CasbinAccess(resource = "insp:alert", action = "view")
    public Result<AlertRule> getAlertRule(@PathVariable Long id) {
        return Result.success(alertService.getAlertRule(id));
    }

    @PostMapping("/alert-rules")
    @CasbinAccess(resource = "insp:alert", action = "manage")
    public Result<AlertRule> createAlertRule(@RequestBody @Valid CreateAlertRuleRequest request) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.success(alertService.createAlertRule(
                request.getRuleName(), request.getMetricType(),
                request.getThresholdConfig(), request.getSeverity(),
                request.getNotificationChannels(), request.getProjectId(), userId));
    }

    @PutMapping("/alert-rules/{id}")
    @CasbinAccess(resource = "insp:alert", action = "manage")
    public Result<AlertRule> updateAlertRule(@PathVariable Long id,
                                             @RequestBody @Valid UpdateAlertRuleRequest request) {
        return Result.success(alertService.updateAlertRule(id,
                request.getRuleName(), request.getMetricType(),
                request.getThresholdConfig(), request.getSeverity(),
                request.getNotificationChannels(), request.getProjectId()));
    }

    @DeleteMapping("/alert-rules/{id}")
    @CasbinAccess(resource = "insp:alert", action = "manage")
    public Result<Void> deleteAlertRule(@PathVariable Long id) {
        alertService.deleteAlertRule(id);
        return Result.success();
    }

    // ========== Alerts ==========

    // TODO: alerts 为潜在高基数表 (告警事件随时间累积). 当前仅按 status 过滤、无分页.
    //       建议后续改为分页 + 时间窗过滤; 暂保留以不破坏现有 API 契约.
    @GetMapping("/alerts")
    @CasbinAccess(resource = "insp:alert", action = "view")
    public Result<List<Alert>> listAlerts(@RequestParam(required = false) String status) {
        return Result.success(alertService.getAlerts(status));
    }

    @GetMapping("/alerts/{id}")
    @CasbinAccess(resource = "insp:alert", action = "view")
    public Result<Alert> getAlert(@PathVariable Long id) {
        return Result.success(alertService.getAlert(id));
    }

    @PutMapping("/alerts/{id}/acknowledge")
    @CasbinAccess(resource = "insp:alert", action = "edit")
    public Result<Alert> acknowledgeAlert(@PathVariable Long id) {
        Long userId = SecurityUtils.requireCurrentUserId();
        return Result.success(alertService.acknowledgeAlert(id, userId));
    }

    @PutMapping("/alerts/{id}/resolve")
    @CasbinAccess(resource = "insp:alert", action = "edit")
    public Result<Alert> resolveAlert(@PathVariable Long id) {
        return Result.success(alertService.resolveAlert(id));
    }

    @PutMapping("/alerts/{id}/dismiss")
    @CasbinAccess(resource = "insp:alert", action = "edit")
    public Result<Alert> dismissAlert(@PathVariable Long id) {
        return Result.success(alertService.dismissAlert(id));
    }

    // ========== Request DTOs ==========

    @lombok.Data
    public static class CreateAlertRuleRequest {
        @NotBlank
        private String ruleName;
        @NotBlank
        private String metricType;
        private String thresholdConfig;
        @NotBlank
        private String severity;
        private String notificationChannels;
        private Long projectId;
    }

    @lombok.Data
    public static class UpdateAlertRuleRequest {
        @NotBlank
        private String ruleName;
        @NotBlank
        private String metricType;
        private String thresholdConfig;
        @NotBlank
        private String severity;
        private String notificationChannels;
        private Long projectId;
    }
}
