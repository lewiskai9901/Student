package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.progress;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教学进度记录.
 *   progress_status: 0=待授课 1=已完成 2=未完成 3=调课
 */
@Data
@TableName("teaching_progress")
public class TeachingProgressPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long tenantId;
    private Long taskId;
    private Long entryId;
    private Long semesterId;
    private Long orgUnitId;
    private Integer weekNumber;
    private Integer lessonNo;
    private String plannedTopic;
    private String actualTopic;
    private String chapter;
    private Integer progressStatus;
    private Integer attendanceCount;
    private Integer totalStudents;
    private String note;
    private LocalDateTime recordedAt;
    private Long recordedBy;
    private Long createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private String courseName;
    @TableField(exist = false)
    private String orgUnitName;
    @TableField(exist = false)
    private String teacherName;
}
