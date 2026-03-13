package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_webhook_subscriptions")
public class WebhookSubscriptionPO {

    @TableId(type = IdType.AUTO)
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

    @TableLogic
    private Integer deleted;
}
