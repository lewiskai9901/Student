package com.school.management.infrastructure.persistence.teaching.task;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("teaching_tasks")
public class TeachingTaskPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String taskCode;
    private Long semesterId;
    private Long courseId;
    private Long classId;
    private Long offeringId;
    private Long orgUnitId;
    private Integer studentCount;
    private Integer weeklyHours;
    private Integer totalHours;
    private Integer startWeek;
    private Integer endWeek;
    private Integer schedulingStatus;
    private Integer taskStatus;
    private String remark;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
