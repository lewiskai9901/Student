package com.school.management.domain.inspection.model.v7.platform;

import com.school.management.domain.shared.AggregateRoot;

import java.time.LocalDateTime;

/**
 * V7 Webhook 订阅聚合根
 * 支持外部系统通过 Webhook 订阅检查平台事件。
 */
public class WebhookSubscription extends AggregateRoot<Long> {

    private Long id;
    private Long tenantId;
    /** 可为 null 表示订阅所有项目的事件 */
    private Long projectId;
    private String subscriptionName;
    private String targetUrl;
    /** HMAC 签名密钥 */
    private String secret;
    /** JSON 数组，订阅的事件类型列表 */
    private String eventTypes;
    private Boolean isEnabled;
    private Integer retryCount;
    private LocalDateTime lastTriggeredAt;
    /** 最近一次触发状态: SUCCESS / FAILED */
    private String lastStatus;
    /** IM平台类型: GENERIC|DINGTALK|WECOM|FEISHU|SLACK */
    private String platform;
    /** 消息模板(支持变量替换) */
    private String messageTemplate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    protected WebhookSubscription() {
    }

    private WebhookSubscription(Builder builder) {
        this.id = builder.id;
        this.tenantId = builder.tenantId;
        this.projectId = builder.projectId;
        this.subscriptionName = builder.subscriptionName;
        this.targetUrl = builder.targetUrl;
        this.secret = builder.secret;
        this.eventTypes = builder.eventTypes;
        this.isEnabled = builder.isEnabled != null ? builder.isEnabled : true;
        this.retryCount = builder.retryCount != null ? builder.retryCount : 3;
        this.lastTriggeredAt = builder.lastTriggeredAt;
        this.lastStatus = builder.lastStatus;
        this.platform = builder.platform != null ? builder.platform : "GENERIC";
        this.messageTemplate = builder.messageTemplate;
        this.createdAt = builder.createdAt != null ? builder.createdAt : LocalDateTime.now();
        this.updatedAt = builder.updatedAt;
    }

    public static WebhookSubscription reconstruct(Builder builder) {
        return new WebhookSubscription(builder);
    }

    public void enable() {
        this.isEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void disable() {
        this.isEnabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void recordTriggerSuccess() {
        this.lastTriggeredAt = LocalDateTime.now();
        this.lastStatus = "SUCCESS";
        this.updatedAt = LocalDateTime.now();
    }

    public void recordTriggerFailure() {
        this.lastTriggeredAt = LocalDateTime.now();
        this.lastStatus = "FAILED";
        this.updatedAt = LocalDateTime.now();
    }

    public void updateSubscription(String subscriptionName, String targetUrl,
                                   String secret, String eventTypes, Integer retryCount) {
        this.subscriptionName = subscriptionName;
        this.targetUrl = targetUrl;
        this.secret = secret;
        this.eventTypes = eventTypes;
        this.retryCount = retryCount;
        this.updatedAt = LocalDateTime.now();
    }

    // Getters
    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Long getTenantId() { return tenantId; }
    public Long getProjectId() { return projectId; }
    public String getSubscriptionName() { return subscriptionName; }
    public String getTargetUrl() { return targetUrl; }
    public String getSecret() { return secret; }
    public String getEventTypes() { return eventTypes; }
    public Boolean getIsEnabled() { return isEnabled; }
    public Integer getRetryCount() { return retryCount; }
    public LocalDateTime getLastTriggeredAt() { return lastTriggeredAt; }
    public String getLastStatus() { return lastStatus; }
    public String getPlatform() { return platform; }
    public String getMessageTemplate() { return messageTemplate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // Setters
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public void setSubscriptionName(String subscriptionName) { this.subscriptionName = subscriptionName; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public void setSecret(String secret) { this.secret = secret; }
    public void setEventTypes(String eventTypes) { this.eventTypes = eventTypes; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; }
    public void setLastStatus(String lastStatus) { this.lastStatus = lastStatus; }
    public void setPlatform(String platform) { this.platform = platform; }
    public void setMessageTemplate(String messageTemplate) { this.messageTemplate = messageTemplate; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private Long tenantId;
        private Long projectId;
        private String subscriptionName;
        private String targetUrl;
        private String secret;
        private String eventTypes;
        private Boolean isEnabled;
        private Integer retryCount;
        private LocalDateTime lastTriggeredAt;
        private String lastStatus;
        private String platform;
        private String messageTemplate;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder tenantId(Long tenantId) { this.tenantId = tenantId; return this; }
        public Builder projectId(Long projectId) { this.projectId = projectId; return this; }
        public Builder subscriptionName(String subscriptionName) { this.subscriptionName = subscriptionName; return this; }
        public Builder targetUrl(String targetUrl) { this.targetUrl = targetUrl; return this; }
        public Builder secret(String secret) { this.secret = secret; return this; }
        public Builder eventTypes(String eventTypes) { this.eventTypes = eventTypes; return this; }
        public Builder isEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public Builder retryCount(Integer retryCount) { this.retryCount = retryCount; return this; }
        public Builder lastTriggeredAt(LocalDateTime lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; return this; }
        public Builder lastStatus(String lastStatus) { this.lastStatus = lastStatus; return this; }
        public Builder platform(String platform) { this.platform = platform; return this; }
        public Builder messageTemplate(String messageTemplate) { this.messageTemplate = messageTemplate; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public WebhookSubscription build() { return new WebhookSubscription(this); }
    }
}
