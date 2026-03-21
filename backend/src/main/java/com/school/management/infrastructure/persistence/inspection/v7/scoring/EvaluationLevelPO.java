package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_evaluation_levels")
public class EvaluationLevelPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;
    private Integer levelNum;
    private String levelName;
    private String levelIcon;
    private String levelColor;
    private String conditionLogic;
    private String conditions;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
