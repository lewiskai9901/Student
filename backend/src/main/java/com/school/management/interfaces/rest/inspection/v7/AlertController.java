package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.AlertApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.analytics.Alert;
import com.school.management.domain.inspection.model.v7.analytics.AlertRule;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v7/insp")
public class AlertController {

    private final AlertApplicationService alertService;

    // ========== Alert Rules ==========

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
    public Result<AlertRule> createAlertRule(@RequestBody CreateAlertRuleRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(alertService.createAlertRule(
                request.getRuleName(), request.getMetricType(),
                request.getThresholdConfig(), request.getSeverity(),
                request.getNotificationChannels(), request.getProjectId(), userId));
    }

    @PutMapping("/alert-rules/{id}")
    @CasbinAccess(resource = "insp:alert", action = "manage")
    public Result<AlertRule> updateAlertRule(@PathVariable Long id,
                                             @RequestBody UpdateAlertRuleRequest request) {
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
        Long userId = SecurityUtils.getCurrentUserId();
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
        private String ruleName;
        private String metricType;
        private String thresholdConfig;
        private String severity;
        private String notificationChannels;
        private Long projectId;
    }

    @lombok.Data
    public static class UpdateAlertRuleRequest {
        private String ruleName;
        private String metricType;
        private String thresholdConfig;
        private String severity;
        private String notificationChannels;
        private Long projectId;
    }
}
