package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V6检查明细持久化对象
 */
@Data
@TableName("inspection_details")
public class InspectionDetailPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long targetId;

    // 检查项信息
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private Long itemId;
    private String itemCode;
    private String itemName;

    // 作用范围
    private String scope;
    private String individualType;
    private Long individualId;
    private String individualName;

    // 打分
    private String scoringMode;
    private BigDecimal score;
    private Integer quantity;
    private BigDecimal totalScore;

    // 评级结果(GRADE模式)
    private String gradeCode;
    private String gradeName;

    // 清单结果(CHECKLIST模式)
    private Boolean checklistChecked;

    // 备注和证据
    private String remark;
    private String evidenceIds;  // JSON array stored as string

    // 审计
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
