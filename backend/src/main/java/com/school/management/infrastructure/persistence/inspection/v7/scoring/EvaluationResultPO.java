package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("insp_evaluation_results")
public class EvaluationResultPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long ruleId;
    private String targetType;
    private Long targetId;
    private String targetName;
    private LocalDate cycleDate;
    private Integer levelNum;
    private String levelName;
    private BigDecimal score;
    private Integer rankNo;
    private String details;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
