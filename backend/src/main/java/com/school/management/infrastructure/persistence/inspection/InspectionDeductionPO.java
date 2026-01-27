package com.school.management.infrastructure.persistence.inspection;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Persistence object for inspection deductions.
 */
@Data
@TableName("inspection_deductions")
public class InspectionDeductionPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long sessionId;

    private Long classRecordId;

    private Long deductionItemId;

    private String itemName;

    private String categoryName;

    private String spaceType;

    private Long spaceId;

    private String spaceName;

    /** JSON array of student IDs */
    private String studentIds;

    /** JSON array of student names */
    private String studentNames;

    private Integer personCount;

    private BigDecimal deductionAmount;

    private String inputSource;

    private String remark;

    /** JSON array of evidence URLs */
    private String evidenceUrls;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
