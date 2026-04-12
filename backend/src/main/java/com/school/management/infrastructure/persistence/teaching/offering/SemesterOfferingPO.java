package com.school.management.infrastructure.persistence.teaching.offering;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("semester_course_offerings")
public class SemesterOfferingPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long semesterId;
    private Long planId;
    private Long planCourseId;
    private Long courseId;
    private String applicableGrade;
    private Integer weeklyHours;
    private Integer totalWeeks;
    private Integer startWeek;
    private Integer endWeek;
    private Integer weekType; // 0=每周 1=单周 2=双周
    private Integer courseCategory;
    private Integer courseType;
    private Boolean allowCombined;
    private Integer maxCombinedClasses;
    private Boolean allowWalking;
    private Integer status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
