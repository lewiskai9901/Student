package com.school.management.domain.teaching.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 培养方案课程实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanCourse {

    private Long id;

    /**
     * 培养方案ID
     */
    private Long planId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 开课学期（第几学期，1-8）
     */
    private Integer semester;

    /**
     * 是否必修
     */
    @Builder.Default
    private Boolean isRequired = true;

    /**
     * 在本方案中的课程类别（覆盖课程表默认值）
     */
    private Integer courseCategory;

    /**
     * 在本方案中的课程性质（必修/选修）
     */
    private Integer courseType;

    /**
     * 学分（覆盖课程表默认值）
     */
    private BigDecimal credits;

    /**
     * 总学时
     */
    private Integer totalHours;

    /**
     * 周学时
     */
    private Integer weeklyHours;

    /**
     * 理论学时
     */
    private Integer theoryHours;

    /**
     * 实践学时
     */
    private Integer practiceHours;

    /**
     * 考核方式
     */
    private Integer examType;

    /**
     * 是否学位课程
     */
    @Builder.Default
    private Boolean isDegreeCourse = false;

    /**
     * 排序号
     */
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 备注
     */
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // === Alias methods for backward compatibility ===

    public Integer getSemesterNumber() {
        return semester;
    }

    public void setSemesterNumber(Integer semesterNumber) {
        this.semester = semesterNumber;
    }

    /**
     * 复制（用于创建新版本）
     */
    public PlanCourse copy() {
        return PlanCourse.builder()
                .courseId(this.courseId)
                .semester(this.semester)
                .isRequired(this.isRequired)
                .courseCategory(this.courseCategory)
                .courseType(this.courseType)
                .credits(this.credits)
                .totalHours(this.totalHours)
                .weeklyHours(this.weeklyHours)
                .theoryHours(this.theoryHours)
                .practiceHours(this.practiceHours)
                .examType(this.examType)
                .isDegreeCourse(this.isDegreeCourse)
                .sortOrder(this.sortOrder)
                .remark(this.remark)
                .build();
    }

    /**
     * 获取课程类型名称
     */
    public String getCourseTypeName() {
        if (courseType == null) return null;
        return switch (courseType) {
            case 1 -> "必修";
            case 2 -> "限选";
            case 3 -> "任选";
            default -> "未知";
        };
    }

    /**
     * 获取考核方式名称
     */
    public String getExamTypeName() {
        if (examType == null) return null;
        return switch (examType) {
            case 1 -> "考试";
            case 2 -> "考查";
            default -> "未知";
        };
    }
}
