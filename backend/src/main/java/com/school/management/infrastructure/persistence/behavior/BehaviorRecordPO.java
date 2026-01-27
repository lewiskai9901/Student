package com.school.management.infrastructure.persistence.behavior;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("student_behavior_records")
public class BehaviorRecordPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long studentId;
    private Long classId;
    private String behaviorType;
    private String source;
    private Long sourceId;
    private String category;
    private String title;
    private String detail;
    private BigDecimal deductionAmount;
    private String status;
    private Long recordedBy;
    private LocalDateTime recordedAt;
    private LocalDateTime notifiedAt;
    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
    private String resolutionNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
