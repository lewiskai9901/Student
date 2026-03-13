package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_notification_rules")
public class NotificationRulePO {

    @TableId(type = IdType.AUTO)
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

    @TableLogic
    private Integer deleted;
}
