package com.school.management.infrastructure.persistence.teaching.teachingclass;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("teaching_classes")
public class TeachingClassPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long semesterId;
    private String className;
    private String classCode;
    private Long courseId;
    private Integer classType;
    private Integer weeklyHours;
    private Integer studentCount;
    private String requiredRoomType;
    private Integer requiredCapacity;
    private Integer startWeek;
    private Integer endWeek;
    private Integer status;
    private String remark;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
