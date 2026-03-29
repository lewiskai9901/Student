package com.school.management.infrastructure.persistence.teaching.scheduling;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("scheduling_constraints")
public class SchedulingConstraintPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long semesterId;
    private String constraintName;
    private Integer constraintLevel;
    private Long targetId;
    private String targetName;
    private String constraintType;
    private Boolean isHard;
    private Integer priority;
    private String params;
    private String effectiveWeeks;
    private Boolean enabled;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
