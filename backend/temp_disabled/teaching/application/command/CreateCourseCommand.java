package com.school.management.application.teaching.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建课程命令
 */
@Data
@Builder
public class CreateCourseCommand {

    @NotBlank(message = "课程代码不能为空")
    private String courseCode;

    @NotBlank(message = "课程名称不能为空")
    private String courseName;

    private String englishName;

    /**
     * 课程类型: 1-必修 2-限选 3-任选 4-实践
     */
    @NotNull(message = "课程类型不能为空")
    private Integer courseType;

    /**
     * 课程性质: 1-理论 2-实验 3-理论+实验 4-实践
     */
    @NotNull(message = "课程性质不能为空")
    private Integer courseNature;

    @NotNull(message = "学分不能为空")
    @Positive(message = "学分必须大于0")
    private BigDecimal credits;

    @NotNull(message = "总学时不能为空")
    @Positive(message = "总学时必须大于0")
    private Integer totalHours;

    /**
     * 理论学时
     */
    private Integer theoryHours;

    /**
     * 实验学时
     */
    private Integer labHours;

    /**
     * 实践学时
     */
    private Integer practiceHours;

    /**
     * 周学时
     */
    private Integer weeklyHours;

    /**
     * 开课部门ID
     */
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
     * 先修课程ID列表(JSON)
     */
    private String prerequisites;

    /**
     * 课程简介
     */
    private String description;

    /**
     * 教学大纲
     */
    private String syllabus;

    /**
     * 操作人
     */
    private Long operatorId;
}
