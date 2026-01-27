package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("student_grades")
public class StudentGradePO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long batchId;
    private Long studentId;
    private Long semesterId;
    private Long courseId;
    private Long classId;
    private String batchName;
    private Integer gradeType;
    private BigDecimal totalScore;
    private String gradeLevel;
    private BigDecimal gradePoint;
    private Integer status;
    private String remark;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long updatedBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
