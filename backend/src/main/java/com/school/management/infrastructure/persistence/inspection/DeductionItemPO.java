package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistence object for deduction items.
 */
@Data
@TableName("deduction_items")
public class DeductionItemPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long categoryId;

    private String itemCode;

    private String itemName;

    private String description;

    private String deductionMode;

    private BigDecimal fixedScore;

    private BigDecimal perPersonScore;

    private BigDecimal minScore;

    private BigDecimal maxScore;

    private Integer sortOrder;

    private Boolean isEnabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
