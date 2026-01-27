package com.school.management.infrastructure.persistence.behavior;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("student_behavior_alerts")
public class BehaviorAlertPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long studentId;
    private Long classId;
    private String alertType;
    private String alertLevel;
    private String title;
    private String description;
    private String triggerData;
    private Boolean isRead;
    private Boolean isHandled;
    private Long handledBy;
    private LocalDateTime handledAt;
    private String handleNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
