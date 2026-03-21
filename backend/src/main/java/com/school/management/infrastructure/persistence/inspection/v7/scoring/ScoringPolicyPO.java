package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_scoring_policies")
public class ScoringPolicyPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private String policyCode;
    private String policyName;
    private String description;
    private Integer precisionDigits;
    private Boolean isSystem;
    private Boolean isEnabled;
    private Integer sortOrder;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
