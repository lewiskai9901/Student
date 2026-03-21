package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_policy_calc_rules")
public class PolicyCalcRulePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long policyId;
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private Integer priority;
    private String config;
    private Boolean isEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
