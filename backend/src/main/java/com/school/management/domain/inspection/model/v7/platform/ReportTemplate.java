package com.school.management.domain.inspection.model.v7.platform;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;

/**
 * V7 报表模板聚合根
 * 定义报表的布局、表头/表尾配置，支持多种报表类型。
 */
public class ReportTemplate extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private String templateName;
    /** 唯一模板编码 */
    private String templateCode;
    /** 报表类型: DAILY_SUMMARY / PERIOD_REPORT / CORRECTIVE_REPORT / INSPECTOR_REPORT / CUSTOM */
    private String reportType;
    /** JSON 布局配置 */
    private String formatConfig;
    /** JSON 表头/表尾配置 */
    private String headerConfig;
    private Boolean isDefault;
    private Boolean isEnabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected ReportTemplate() {
    }

    private ReportTemplate(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.templateName = builder.templateName;
        this.templateCode = builder.templateCode;
        this.reportType = builder.reportType;
        this.formatConfig = builder.formatConfig;
        this.headerConfig = builder.headerConfig;
        this.isDefault = builder.isDefault != null ? builder.isDefault : false;
        this.isEnabled = builder.isEnabled != null ? builder.isEnabled : true;
        this.createdBy = builder.createdBy;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static ReportTemplate reconstruct(Builder builder) {
        return new ReportTemplate(builder);
    }

    public void enable() {
        this.isEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void disable() {
        this.isEnabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsDefault() {
        this.isDefault = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void unmarkAsDefault() {
        this.isDefault = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateTemplate(String templateName, String reportType,
                               String formatConfig, String headerConfig) {
        this.templateName = templateName;
        this.reportType = reportType;
        this.formatConfig = formatConfig;
        this.headerConfig = headerConfig;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public String getTemplateName() { return templateName; }
    public String getTemplateCode() { return templateCode; }
    public String getReportType() { return reportType; }
    public String getFormatConfig() { return formatConfig; }
    public String getHeaderConfig() { return headerConfig; }
    public Boolean getIsDefault() { return isDefault; }
    public Boolean getIsEnabled() { return isEnabled; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public void setFormatConfig(String formatConfig) { this.formatConfig = formatConfig; }
    public void setHeaderConfig(String headerConfig) { this.headerConfig = headerConfig; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private String templateName;
        private String templateCode;
        private String reportType;
        private String formatConfig;
        private String headerConfig;
        private Boolean isDefault;
        private Boolean isEnabled;
        private Long createdBy;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder templateName(String templateName) { this.templateName = templateName; return this; }
        public Builder templateCode(String templateCode) { this.templateCode = templateCode; return this; }
        public Builder reportType(String reportType) { this.reportType = reportType; return this; }
        public Builder formatConfig(String formatConfig) { this.formatConfig = formatConfig; return this; }
        public Builder headerConfig(String headerConfig) { this.headerConfig = headerConfig; return this; }
        public Builder isDefault(Boolean isDefault) { this.isDefault = isDefault; return this; }
        public Builder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public Builder createdBy(Long createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public ReportTemplate build() { return new ReportTemplate(this); }
    }
}
