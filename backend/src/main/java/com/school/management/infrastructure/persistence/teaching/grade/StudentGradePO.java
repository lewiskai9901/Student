package com.school.management.infrastructure.persistence.teaching.grade;

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
    private Long semesterId;
    private Long taskId;
    private Long courseId;
    private Long studentId;
    private Long classId;
    private BigDecimal totalScore;
    private BigDecimal gradePoint;
    private Integer passed;
    private BigDecimal creditsEarned;
    private Integer gradeStatus;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
