package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("evaluation_indicators")
public class EvaluationIndicatorPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long evaluationId;
    private String indicatorName;
    private String description;
    private Integer weight;
    private Integer maxScore;
    private Integer sortOrder;
    private Integer required;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
