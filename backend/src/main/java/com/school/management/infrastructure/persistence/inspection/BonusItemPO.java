package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistence object for bonus items.
 */
@Data
@TableName("bonus_items")
public class BonusItemPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long categoryId;

    private String itemName;

    private String bonusMode;

    private BigDecimal fixedBonus;

    private String progressiveConfig;

    private BigDecimal improvementCoefficient;

    private String description;

    private Integer sortOrder;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
