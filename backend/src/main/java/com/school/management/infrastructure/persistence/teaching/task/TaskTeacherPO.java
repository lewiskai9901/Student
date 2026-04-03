package com.school.management.infrastructure.persistence.teaching.task;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("teaching_task_teachers")
public class TaskTeacherPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long teacherId;
    private Integer teacherRole;
    private BigDecimal workloadRatio;
    private String remark;
    private LocalDateTime createdAt;
}
