package com.school.management.infrastructure.persistence.teaching.exam;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@TableName("exam_arrangements")
public class ExamArrangementPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Long courseId;
    private Long taskId;
    private Long classId;
    private LocalDate examDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer duration;
    private Integer examForm;
    private Integer totalStudents;
    private String remark;
    private Integer status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
