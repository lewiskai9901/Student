package com.school.management.domain.inspection.model.v7.platform;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;

/**
 * V7 通知规则聚合根
 * 定义项目级别的事件通知规则，支持多渠道、多接收人类型。
 */
public class NotificationRule extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    private Long projectId;
    private String ruleName;
    /** 触发事件类型，如 "SubmissionCompletedEvent" */
    private String eventType;
    /** JSON 条件表达式，为空则所有事件均触发 */
    private String condition;
    /** JSON 数组，如 ["IN_APP","EMAIL"] */
    private String channels;
    /** 接收人类型: ROLE / USER / DYNAMIC */
    private String recipientType;
    /** JSON 配置，描述具体接收人 */
    private String recipientConfig;
    private Boolean isEnabled;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected NotificationRule() {
    }

    private NotificationRule(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.projectId = builder.projectId;
        this.ruleName = builder.ruleName;
        this.eventType = builder.eventType;
        this.condition = builder.condition;
        this.channels = builder.channels;
        this.recipientType = builder.recipientType;
        this.recipientConfig = builder.recipientConfig;
        this.isEnabled = builder.isEnabled != null ? builder.isEnabled : true;
        this.priority = builder.priority != null ? builder.priority : 0;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static NotificationRule reconstruct(Builder builder) {
        return new NotificationRule(builder);
    }

    public void enable() {
        this.isEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void disable() {
        this.isEnabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateRule(String ruleName, String eventType, String condition,
                           String channels, String recipientType, String recipientConfig,
                           Integer priority) {
        this.ruleName = ruleName;
        this.eventType = eventType;
        this.condition = condition;
        this.channels = channels;
        this.recipientType = recipientType;
        this.recipientConfig = recipientConfig;
        this.priority = priority;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getProjectId() { return projectId; }
    public String getRuleName() { return ruleName; }
    public String getEventType() { return eventType; }
    public String getCondition() { return condition; }
    public String getChannels() { return channels; }
    public String getRecipientType() { return recipientType; }
    public String getRecipientConfig() { return recipientConfig; }
    public Boolean getIsEnabled() { return isEnabled; }
    public Integer getPriority() { return priority; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setCondition(String condition) { this.condition = condition; }
    public void setChannels(String channels) { this.channels = channels; }
    public void setRecipientType(String recipientType) { this.recipientType = recipientType; }
    public void setRecipientConfig(String recipientConfig) { this.recipientConfig = recipientConfig; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long projectId;
        private String ruleName;
        private String eventType;
        private String condition;
        private String channels;
        private String recipientType;
        private String recipientConfig;
        private Boolean isEnabled;
        private Integer priority;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder ruleName(String ruleName) { this.ruleName = ruleName; return this; }
        public Builder eventType(String eventType) { this.eventType = eventType; return this; }
        public Builder condition(String condition) { this.condition = condition; return this; }
        public Builder channels(String channels) { this.channels = channels; return this; }
        public Builder recipientType(String recipientType) { this.recipientType = recipientType; return this; }
        public Builder recipientConfig(String recipientConfig) { this.recipientConfig = recipientConfig; return this; }
        public Builder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public Builder priority(Integer priority) { this.priority = priority; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public NotificationRule build() { return new NotificationRule(this); }
    }
}
