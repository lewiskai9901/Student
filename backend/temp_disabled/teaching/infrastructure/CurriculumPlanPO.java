package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 培养方案持久化对象
 */
@Data
@TableName("curriculum_plans")
public class CurriculumPlanPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String planCode;

    private String planName;

    private Long majorId;

    /**
     * 适用入学年份
     */
    private Integer enrollYear;

    /**
     * 学制(年)
     */
    private Integer duration;

    /**
     * 版本号
     */
    private Integer version;

    private BigDecimal totalCredits;

    private BigDecimal requiredCredits;

    private BigDecimal electiveCredits;

    private BigDecimal practiceCredits;

    private String objectives;

    private String requirements;

    private String remark;

    /**
     * 状态: 0-草稿 1-已发布 2-已归档
     */
    private Integer status;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
