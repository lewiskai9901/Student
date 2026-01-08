package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistence object for class scores in inspection records.
 */
@Data
@TableName("class_scores")
public class ClassScorePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long recordId;

    private Long classId;

    private String className;

    private Integer baseScore;

    private BigDecimal totalDeduction;

    private BigDecimal finalScore;

    private String ranking;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
