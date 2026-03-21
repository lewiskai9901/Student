package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insp_policy_grade_bands")
public class PolicyGradeBandPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long policyId;
    private String gradeCode;
    private String gradeName;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
