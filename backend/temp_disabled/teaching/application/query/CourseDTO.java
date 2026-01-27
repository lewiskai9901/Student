package com.school.management.application.teaching.query;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程查询DTO
 */
@Data
@Builder
public class CourseDTO {

    private Long id;

    private String courseCode;

    private String courseName;

    private String englishName;

    /**
     * 课程类型: 1-必修 2-限选 3-任选 4-实践
     */
    private Integer courseType;

    private String courseTypeName;

    /**
     * 课程性质: 1-理论 2-实验 3-理论+实验 4-实践
     */
    private Integer courseNature;

    private String courseNatureName;

    private BigDecimal credits;

    private Integer totalHours;

    private Integer theoryHours;

    private Integer labHours;

    private Integer practiceHours;

    private Integer weeklyHours;

    private Long departmentId;

    private String departmentName;

    /**
     * 考核方式: 1-考试 2-考查
     */
    private Integer examType;

    private String examTypeName;

    /**
     * 成绩类型: 1-百分制 2-五级制 3-二级制
     */
    private Integer gradeType;

    private String gradeTypeName;

    private String prerequisites;

    private String description;

    private String syllabus;

    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 获取课程类型名称
     */
    public static String getCourseTypeName(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case 1 -> "必修";
            case 2 -> "限选";
            case 3 -> "任选";
            case 4 -> "实践";
            default -> "";
        };
    }

    /**
     * 获取课程性质名称
     */
    public static String getCourseNatureName(Integer nature) {
        if (nature == null) return "";
        return switch (nature) {
            case 1 -> "理论";
            case 2 -> "实验";
            case 3 -> "理论+实验";
            case 4 -> "实践";
            default -> "";
        };
    }

    /**
     * 获取考核方式名称
     */
    public static String getExamTypeName(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case 1 -> "考试";
            case 2 -> "考查";
            default -> "";
        };
    }

    /**
     * 获取成绩类型名称
     */
    public static String getGradeTypeName(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case 1 -> "百分制";
            case 2 -> "五级制";
            case 3 -> "二级制";
            default -> "";
        };
    }
}
