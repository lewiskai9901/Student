package com.school.management.infrastructure.persistence.teaching.offering;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("class_course_assignments")
public class ClassCourseAssignmentPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long semesterId;
    private Long orgUnitId;
    private Long offeringId;
    private Long courseId;
    private Integer weeklyHours;
    private Integer studentCount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
