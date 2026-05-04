package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.preference;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师排课偏好.
 *   preference_type: 1=不可用时间 2=偏好时间 3=偏好教室
 *   weekday: 1-7
 *   time_slot: 节次 (与 schedule_entries.start_slot 同一坐标系)
 */
@Data
@TableName("teacher_preferences")
public class TeacherPreferencePO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long teacherId;
    private Long semesterId;
    private Integer preferenceType;
    private Integer weekday;
    private Integer timeSlot;
    private Long classroomId;
    private Integer priority;
    private String reason;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
