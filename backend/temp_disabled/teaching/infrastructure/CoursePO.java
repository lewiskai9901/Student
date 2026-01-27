package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程持久化对象
 */
@Data
@TableName("courses")
public class CoursePO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String courseCode;

    private String courseName;

    private String englishName;

    /**
     * 课程类型: 1-必修 2-限选 3-任选 4-实践
     */
    private Integer courseType;

    /**
     * 课程性质: 1-理论 2-实验 3-理论+实验 4-实践
     */
    private Integer courseNature;

    private BigDecimal credits;

    private Integer totalHours;

    private Integer theoryHours;

    private Integer labHours;

    private Integer practiceHours;

    private Integer weeklyHours;

    private Long departmentId;

    /**
     * 考核方式: 1-考试 2-考查
     */
    private Integer examType;

    /**
     * 成绩类型: 1-百分制 2-五级制 3-二级制
     */
    private Integer gradeType;

    /**
     * 先修课程(JSON)
     */
    private String prerequisites;

    private String description;

    private String syllabus;

    /**
     * 状态: 0-停用 1-启用
     */
    private Integer status;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
