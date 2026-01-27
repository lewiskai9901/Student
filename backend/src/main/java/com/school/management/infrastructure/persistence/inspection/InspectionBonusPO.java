package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistence object for inspection bonuses.
 */
@Data
@TableName("inspection_bonuses")
public class InspectionBonusPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long classRecordId;

    private Long sessionId;

    private Long classId;

    private Long bonusItemId;

    private BigDecimal bonusScore;

    private String reason;

    private Long recordedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
