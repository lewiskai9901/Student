package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 培养方案课程持久化对象
 */
@Data
@TableName("curriculum_plan_courses")
public class PlanCoursePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long planId;

    private Long courseId;

    /**
     * 开课学期
     */
    private Integer semester;

    /**
     * 周学时
     */
    private Integer weeklyHours;

    /**
     * 考核方式
     */
    private String examType;

    /**
     * 是否必修
     */
    private Boolean isRequired;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
