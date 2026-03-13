package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("insp_grade_bands")
public class GradeBandPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long scoringProfileId;
    private Long dimensionId;
    private String gradeCode;
    private String gradeName;
    private BigDecimal minScore;
    private BigDecimal maxScore;
    private String color;
    private String icon;
    private Integer sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
