package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insp_score_dimensions")
public class ScoreDimensionPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long scoringProfileId;
    private String dimensionCode;
    private String dimensionName;
    private Integer weight;
    private BigDecimal baseScore;
    private BigDecimal passThreshold;
    private String sourceType;
    private Long moduleTemplateId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
