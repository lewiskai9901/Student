package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistence object for deduction details in class scores.
 */
@Data
@TableName("deduction_details")
public class DeductionDetailPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long classScoreId;

    private Long deductionItemId;

    private String itemName;

    private Integer count;

    private BigDecimal deductionAmount;

    private String remark;

    /**
     * JSON array of evidence URLs.
     */
    private String evidenceUrls;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
