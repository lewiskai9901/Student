package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistence object for class inspection records.
 */
@Data
@TableName("class_inspection_records")
public class ClassInspectionRecordPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long sessionId;

    private Long classId;

    private String className;

    private Long orgUnitId;

    private String orgUnitName;

    private Integer baseScore;

    private BigDecimal totalDeduction;

    private BigDecimal bonusScore;

    private BigDecimal finalScore;

    private String status;

    private String remarks;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
