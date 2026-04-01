package com.school.management.infrastructure.persistence.academic;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 培养方案持久化对象
 * 映射到 curriculum_plans 表
 */
@Data
@TableName("curriculum_plans")
public class CurriculumPlanPO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String planCode;
    private String planName;

    @TableField("major_id")
    private Long majorId;

    private Integer gradeYear;
    private BigDecimal totalCredits;
    private BigDecimal requiredCredits;
    private BigDecimal electiveCredits;
    private BigDecimal practiceCredits;
    private String trainingObjective;
    private String graduationRequirement;
    private Integer version;
    private Integer status;
    private LocalDateTime publishedAt;
    private Long publishedBy;
    private Long createdBy;
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
