package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("course_evaluations")
public class CourseEvaluationPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private String evaluationCode;
    private String evaluationName;
    private Long semesterId;
    private Long orgUnitId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 0草稿 1进行中 2已结束 */
    private Integer status;
    /** 是否匿名 (0/1) */
    private Integer anonymous;
    private String description;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
