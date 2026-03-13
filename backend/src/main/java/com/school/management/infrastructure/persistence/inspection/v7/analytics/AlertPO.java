package com.school.management.infrastructure.persistence.inspection.v7.analytics;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insp_alerts")
public class AlertPO {

    @TableId(type = IdType.AUTO)
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

    @TableLogic
    private Integer deleted;
}
