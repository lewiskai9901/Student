package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistence object for checklist responses.
 */
@Data
@TableName("checklist_responses")
public class ChecklistResponsePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long sessionId;

    private Long classRecordId;

    private Long checklistItemId;

    private String itemName;

    private String categoryName;

    private String result;

    private BigDecimal autoDeduction;

    private String inspectorNote;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
