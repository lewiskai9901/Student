package com.school.management.infrastructure.persistence.inspection.v6;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 模板扣分项持久化对象
 */
@Data
@TableName("template_score_items")
public class TemplateScoreItemPO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long categoryId;

    private String itemCode;

    private String itemName;

    private String description;

    private String scoringMode;

    private BigDecimal score;

    private BigDecimal minScore;

    private BigDecimal maxScore;

    private BigDecimal perPersonScore;

    private Boolean canLinkIndividual;

    private Boolean requiresPhoto;

    private Boolean requiresRemark;

    private String checkPoints;

    private Integer sortOrder;

    private Boolean isEnabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
