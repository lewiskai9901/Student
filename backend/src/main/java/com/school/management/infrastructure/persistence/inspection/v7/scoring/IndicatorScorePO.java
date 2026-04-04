package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("insp_indicator_scores")
public class IndicatorScorePO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long indicatorId;
    private Long targetId;
    private String targetName;
    private String targetType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal score;
    private String gradeCode;
    private String gradeName;
    private String gradeColor;
    private Integer sourceCount;
    private String detail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
