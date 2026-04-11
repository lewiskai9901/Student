package com.school.management.infrastructure.persistence.academic;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 方案课程持久化对象
 * 映射到 curriculum_plan_courses 表
 */
@Data
@TableName("curriculum_plan_courses")
public class PlanCoursePO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("plan_id")
    private Long planId;

    @TableField("course_id")
    private Long courseId;

    private Integer semesterNumber;
    private Integer courseCategory;
    private Integer courseType;
    private BigDecimal credits;
    private Integer totalHours;
    private Integer weeklyHours;
    private Integer theoryHours;
    private Integer practiceHours;
    private Integer assessmentMethod;
    private Integer sortOrder;
    private String remark;

    /** JOIN 字段，非持久化 */
    @TableField(exist = false)
    private String courseCode;

    /** JOIN 字段，非持久化 */
    @TableField(exist = false)
    private String courseName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
