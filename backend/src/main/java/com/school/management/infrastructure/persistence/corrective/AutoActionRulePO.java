package com.school.management.infrastructure.persistence.corrective;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("auto_action_rules")
public class AutoActionRulePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String ruleCode;
    private String ruleName;
    private String triggerType;
    private String triggerCondition;
    private String severity;
    private String category;
    private Integer deadlineHours;
    private Boolean autoAssign;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
