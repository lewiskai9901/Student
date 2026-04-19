package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.grade;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("grade_batches")
public class GradeBatchPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String batchCode;
    private String batchName;
    private Long semesterId;
    private Long courseId;
    private Long orgUnitId;
    @TableField(exist = false)
    private Long examBatchId;
    private Integer gradeType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String courseName;
    @TableField(exist = false)
    private String className;
    @TableField(exist = false)
    private String createdByName;
    @TableField(exist = false)
    private java.time.LocalDateTime inputDeadline; // alias of endTime for UI
}
