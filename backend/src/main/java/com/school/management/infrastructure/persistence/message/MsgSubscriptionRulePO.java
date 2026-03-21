package com.school.management.infrastructure.persistence.message;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息订阅规则持久化对象
 */
@Data
@TableName("msg_subscription_rules")
public class MsgSubscriptionRulePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String ruleName;
    private String eventCategory;
    private String eventType;
    private String targetMode;
    private String targetConfig;
    private String channel;
    private Long templateId;
    private Integer isEnabled;
    private Integer sortOrder;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
