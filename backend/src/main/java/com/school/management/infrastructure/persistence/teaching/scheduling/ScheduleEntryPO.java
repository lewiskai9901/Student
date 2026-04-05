package com.school.management.infrastructure.persistence.teaching.scheduling;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("schedule_entries")
public class ScheduleEntryPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long semesterId;
    private Long taskId;
    private Long teachingClassId;
    private Long courseId;
    private Long classId;
    private Long teacherId;
    private Long classroomId;
    private Integer weekday;
    private Integer startSlot;
    private Integer endSlot;
    private Integer startWeek;
    private Integer endWeek;
    private Integer weekType;
    private String consecutiveGroup;
    private Integer scheduleType;
    private Integer entryStatus;
    private Integer conflictFlag;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private Long updatedBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
