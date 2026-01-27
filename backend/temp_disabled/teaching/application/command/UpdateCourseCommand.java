package com.school.management.application.teaching.command;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新课程命令
 */
@Data
@Builder
public class UpdateCourseCommand {

    @NotNull(message = "课程ID不能为空")
    private Long id;

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

    @Positive(message = "学分必须大于0")
    private BigDecimal credits;

    @Positive(message = "总学时必须大于0")
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

    private String prerequisites;

    private String description;

    private String syllabus;

    /**
     * 状态: 0-停用 1-启用
     */
    private Integer status;

    private Long operatorId;
}
