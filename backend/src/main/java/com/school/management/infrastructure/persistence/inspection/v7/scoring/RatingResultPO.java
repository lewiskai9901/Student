package com.school.management.infrastructure.persistence.inspection.v7.scoring;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("insp_rating_results")
public class RatingResultPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long dimensionId;
    private Long targetId;
    private String targetName;
    private String targetType;
    private LocalDate cycleDate;
    private BigDecimal score;
    private String grade;
    private Integer rankNo;
    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
