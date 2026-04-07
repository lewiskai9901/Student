package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 教师任职记录持久化对象
 */
@Data
@TableName("teacher_assignments")
public class TeacherAssignmentPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 班级ID
     */
    private Long orgUnitId;

    /**
     * 教师ID
     */
    private Long teacherId;

    /**
     * 教师姓名
     */
    private String teacherName;

    /**
     * 任职角色: HEAD_TEACHER, DEPUTY_HEAD_TEACHER, SUBJECT_TEACHER, COUNSELOR
     */
    private String role;

    /**
     * 任职开始日期
     */
    private LocalDate startDate;

    /**
     * 任职结束日期
     */
    private LocalDate endDate;

    /**
     * 是否当前任职
     */
    private Boolean isCurrent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
