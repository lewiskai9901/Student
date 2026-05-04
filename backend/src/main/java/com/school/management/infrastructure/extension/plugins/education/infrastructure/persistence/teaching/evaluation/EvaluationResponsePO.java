package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.evaluation;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("evaluation_responses")
public class EvaluationResponsePO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long evaluationId;
    private Long taskId;
    private Long teacherId;
    private Long studentId;
    private Long orgUnitId;
    private BigDecimal totalScore;
    /** JSON 数组: [{"indicatorId":1,"score":4.5}, ...] */
    private String scoresJson;
    private String comment;
    /** 0未提交 1已提交 */
    private Integer status;
    private LocalDateTime submittedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
