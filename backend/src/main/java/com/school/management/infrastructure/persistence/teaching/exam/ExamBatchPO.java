package com.school.management.infrastructure.persistence.teaching.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("exam_batches")
public class ExamBatchPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String batchCode;
    private String batchName;
    private Long semesterId;
    private Integer examType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
