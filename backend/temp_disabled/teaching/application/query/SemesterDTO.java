package com.school.management.application.teaching.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学期DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemesterDTO {

    private Long id;

    /**
     * 所属学年ID
     */
    private Long academicYearId;

    /**
     * 学期代码
     */
    private String semesterCode;

    /**
     * 学期名称
     */
    private String semesterName;

    /**
     * 学期类型：1秋季 2春季
     */
    private Integer semesterType;

    /**
     * 学期开始日期
     */
    private LocalDate startDate;

    /**
     * 学期结束日期
     */
    private LocalDate endDate;

    /**
     * 教学开始日期
     */
    private LocalDate teachingStartDate;

    /**
     * 教学结束日期
     */
    private LocalDate teachingEndDate;

    /**
     * 考试周开始日期
     */
    private LocalDate examStartDate;

    /**
     * 考试周结束日期
     */
    private LocalDate examEndDate;

    /**
     * 教学周数
     */
    private Integer totalTeachingWeeks;

    /**
     * 是否当前学期
     */
    private Boolean isCurrent;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 获取学期类型名称
     */
    public String getSemesterTypeName() {
        return semesterType == 1 ? "秋季学期" : "春季学期";
    }
}
