package com.school.management.domain.inspection.model.v7.analytics;

import com.school.management.domain.shared.Entity;

import java.time.LocalDateTime;

/**
 * V7 告警规则 - 定义指标阈值触发条件
 */
public class AlertRule implements Entity<Long> {

    private Long id;
    private Long tenantId;
    private String ruleName;
    private String metricType; // SCORE_DROP | CONSECUTIVE_FAIL | HIGH_DEVIATION | LOW_COMPLIANCE | OVERDUE_CORRECTION
    private String thresholdConfig; // JSON
    private String severity; // INFO | WARNING | CRITICAL
    private String notificationChannels; // JSON
    private Boolean isEnabled;
    private Long projectId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected AlertRule() {
    }

    private AlertRule(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.ruleName = builder.ruleName;
        this.metricType = builder.metricType;
        this.thresholdConfig = builder.thresholdConfig;
        this.severity = builder.severity != null ? builder.severity : "WARNING";
        this.notificationChannels = builder.notificationChannels;
        this.isEnabled = builder.isEnabled != null ? builder.isEnabled : true;
        this.projectId = builder.projectId;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt != null ? builder.updatedAt : LocalDateTime.now();
    }

    public static AlertRule create(String ruleName, String metricType, String thresholdConfig,
                                    String severity, String notificationChannels,
                                    Long projectId, Long createdBy) {
        if (ruleName == null || ruleName.isBlank()) {
            throw new IllegalArgumentException("规则名称不能为空");
        }
        if (metricType == null || metricType.isBlank()) {
            throw new IllegalArgumentException("指标类型不能为空");
        }
        if (thresholdConfig == null || thresholdConfig.isBlank()) {
            throw new IllegalArgumentException("阈值配置不能为空");
        }
        return builder()
                .ruleName(ruleName)
                .metricType(metricType)
                .thresholdConfig(thresholdConfig)
                .severity(severity)
                .notificationChannels(notificationChannels)
                .projectId(projectId)
                .createdBy(createdBy)
                .build();
    }

    public static AlertRule reconstruct(Builder builder) {
        return new AlertRule(builder);
    }

    public void updateRule(String ruleName, String metricType, String thresholdConfig,
                           String severity, String notificationChannels, Long projectId) {
        if (ruleName != null && !ruleName.isBlank()) {
            this.ruleName = ruleName;
        }
        if (metricType != null) {
            this.metricType = metricType;
        }
        if (thresholdConfig != null) {
            this.thresholdConfig = thresholdConfig;
        }
        if (severity != null) {
            this.severity = severity;
        }
        if (notificationChannels != null) {
            this.notificationChannels = notificationChannels;
        }
        this.projectId = projectId;
        this.updatedAt = LocalDateTime.now();
    }

    public void enable() {
        this.isEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void disable() {
        this.isEnabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public String getRuleName() { return ruleName; }
    public String getMetricType() { return metricType; }
    public String getThresholdConfig() { return thresholdConfig; }
    public String getSeverity() { return severity; }
    public String getNotificationChannels() { return notificationChannels; }
    public Boolean getIsEnabled() { return isEnabled; }
    public Long getProjectId() { return projectId; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private String ruleName;
        private String metricType;
        private String thresholdConfig;
        private String severity;
        private String notificationChannels;
        private Boolean isEnabled;
        private Long projectId;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder ruleName(String ruleName) { this.ruleName = ruleName; return this; }
        public Builder metricType(String metricType) { this.metricType = metricType; return this; }
        public Builder thresholdConfig(String thresholdConfig) { this.thresholdConfig = thresholdConfig; return this; }
        public Builder severity(String severity) { this.severity = severity; return this; }
        public Builder notificationChannels(String notificationChannels) { this.notificationChannels = notificationChannels; return this; }
        public Builder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public AlertRule build() { return new AlertRule(this); }
    }
}
