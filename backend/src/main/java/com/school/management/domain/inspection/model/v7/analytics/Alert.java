package com.school.management.domain.inspection.model.v7.analytics;

import com.school.management.domain.shared.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V7 告警 - 指标阈值触发后生成的告警记录
 */
public class Alert implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private Long alertRuleId;
    private Long targetId;
    private String targetType;
    private String targetName;
    private BigDecimal metricValue;
    private BigDecimal thresholdValue;
    private String severity; // INFO | WARNING | CRITICAL
    private String message;
    private String status; // OPEN | ACKNOWLEDGED | RESOLVED | DISMISSED
    private Long acknowledgedBy;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime triggeredAt;

    protected Alert() {
    }

    private Alert(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.alertRuleId = builder.alertRuleId;
        this.targetId = builder.targetId;
        this.targetType = builder.targetType;
        this.targetName = builder.targetName;
        this.metricValue = builder.metricValue;
        this.thresholdValue = builder.thresholdValue;
        this.severity = builder.severity != null ? builder.severity : "WARNING";
        this.message = builder.message;
        this.status = builder.status != null ? builder.status : "OPEN";
        this.acknowledgedBy = builder.acknowledgedBy;
        this.acknowledgedAt = builder.acknowledgedAt;
        this.resolvedAt = builder.resolvedAt;
        this.triggeredAt = builder.triggeredAt != null ? builder.triggeredAt : LocalDateTime.now();
    }

    public static Alert create(Long alertRuleId, Long targetId, String targetType,
                                String targetName, BigDecimal metricValue,
                                BigDecimal thresholdValue, String severity, String message) {
        if (alertRuleId == null) {
            throw new IllegalArgumentException("告警规则ID不能为空");
        }
        if (targetId == null) {
            throw new IllegalArgumentException("目标ID不能为空");
        }
        if (targetType == null || targetType.isBlank()) {
            throw new IllegalArgumentException("目标类型不能为空");
        }
        return builder()
                .alertRuleId(alertRuleId)
                .targetId(targetId)
                .targetType(targetType)
                .targetName(targetName)
                .metricValue(metricValue)
                .thresholdValue(thresholdValue)
                .severity(severity)
                .message(message)
                .build();
    }

    public static Alert reconstruct(Builder builder) {
        return new Alert(builder);
    }

    public void acknowledge(Long userId) {
        if (!"OPEN".equals(this.status)) {
            throw new IllegalStateException("只有OPEN状态的告警可以确认，当前状态: " + this.status);
        }
        this.status = "ACKNOWLEDGED";
        this.acknowledgedBy = userId;
        this.acknowledgedAt = LocalDateTime.now();
    }

    public void resolve() {
        if ("RESOLVED".equals(this.status) || "DISMISSED".equals(this.status)) {
            throw new IllegalStateException("告警已处理，当前状态: " + this.status);
        }
        this.status = "RESOLVED";
        this.resolvedAt = LocalDateTime.now();
    }

    public void dismiss() {
        if ("RESOLVED".equals(this.status) || "DISMISSED".equals(this.status)) {
            throw new IllegalStateException("告警已处理，当前状态: " + this.status);
        }
        this.status = "DISMISSED";
        this.resolvedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getAlertRuleId() { return alertRuleId; }
    public Long getTargetId() { return targetId; }
    public String getTargetType() { return targetType; }
    public String getTargetName() { return targetName; }
    public BigDecimal getMetricValue() { return metricValue; }
    public BigDecimal getThresholdValue() { return thresholdValue; }
    public String getSeverity() { return severity; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public Long getAcknowledgedBy() { return acknowledgedBy; }
    public LocalDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public LocalDateTime getTriggeredAt() { return triggeredAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long alertRuleId;
        private Long targetId;
        private String targetType;
        private String targetName;
        private BigDecimal metricValue;
        private BigDecimal thresholdValue;
        private String severity;
        private String message;
        private String status;
        private Long acknowledgedBy;
        private LocalDateTime acknowledgedAt;
        private LocalDateTime resolvedAt;
        private LocalDateTime triggeredAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder alertRuleId(Long alertRuleId) { this.alertRuleId = alertRuleId; return this; }
        public Builder targetId(Long targetId) { this.targetId = targetId; return this; }
        public Builder targetType(String targetType) { this.targetType = targetType; return this; }
        public Builder targetName(String targetName) { this.targetName = targetName; return this; }
        public Builder metricValue(BigDecimal metricValue) { this.metricValue = metricValue; return this; }
        public Builder thresholdValue(BigDecimal thresholdValue) { this.thresholdValue = thresholdValue; return this; }
        public Builder severity(String severity) { this.severity = severity; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder status(String status) { this.status = status; return this; }
        public Builder acknowledgedBy(Long acknowledgedBy) { this.acknowledgedBy = acknowledgedBy; return this; }
        public Builder acknowledgedAt(LocalDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; return this; }
        public Builder resolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; return this; }
        public Builder triggeredAt(LocalDateTime triggeredAt) { this.triggeredAt = triggeredAt; return this; }

        public Alert build() { return new Alert(this); }
    }
}
