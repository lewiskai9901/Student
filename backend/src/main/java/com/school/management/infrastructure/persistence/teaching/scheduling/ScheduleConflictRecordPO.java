package com.school.management.infrastructure.persistence.teaching.scheduling;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("schedule_conflict_records")
public class ScheduleConflictRecordPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long semesterId;
    private String detectionBatch;
    private Integer conflictCategory;
    private String conflictType;
    private Integer severity;
    private String description;
    private String detail;
    @TableField("entry_id_1")
    private Long entryId1;
    @TableField("entry_id_2")
    private Long entryId2;
    private Long constraintId;
    private Integer resolutionStatus;
    private String resolutionNote;
    private Long resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
