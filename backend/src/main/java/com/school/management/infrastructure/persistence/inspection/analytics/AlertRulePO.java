package com.school.management.infrastructure.persistence.inspection.analytics;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_alert_rules")
public class AlertRulePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long orgUnitId;               // 数据权限边界 (从 project 继承, MetaObjectHandler 自动填充)
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

    @TableLogic
    private Integer deleted;
}
