package com.school.management.infrastructure.persistence.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评选活动持久化对象
 */
@Data
@TableName("eval_campaigns")
public class EvalCampaignPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String campaignName;
    private String campaignDescription;
    private String targetType;
    private String scopeOrgIds;
    private String evaluationPeriod;
    private String status;
    private Boolean isAutoExecute;
    private LocalDateTime lastExecutedAt;
    private LocalDateTime nextExecuteAt;
    private Integer sortOrder;
    private Long createdBy;
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
