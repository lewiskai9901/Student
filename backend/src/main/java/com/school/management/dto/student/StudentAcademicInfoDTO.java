package com.school.management.dto.student;

import lombok.Data;

import java.time.LocalDate;

/**
 * 学生学业信息DTO
 * 包含：班级、年级、专业、学制、入学日期等学业相关信息
 *
 * @author system
 * @version 2.0.0
 * @since 2024-12-31
 */
@Data
public class StudentAcademicInfoDTO {

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 年级ID
     */
    private Long gradeId;

    /**
     * 专业ID
     */
    private Long majorId;

    /**
     * 专业方向ID
     */
    private Long majorDirectionId;

    /**
     * 专业级别/层次(如:中专/大专)
     */
    private String educationLevel;

    /**
     * 学制(如:3年)
     */
    private String studyLength;

    /**
     * 学历(如:中专/大专)
     */
    private String degreeType;

    /**
     * 入学层次(如:本科、专科等)
     */
    private String entryLevel;

    /**
     * 学制(如:4年制、3年制等)
     */
    private String educationSystem;

    /**
     * 入学日期
     */
    private LocalDate admissionDate;

    /**
     * 毕业日期
     */
    private LocalDate graduationDate;

    /**
     * 学生状态(0在校 1毕业 2退学 3休学)
     */
    private Integer studentStatus;

    /**
     * 毕业学校
     */
    private String graduatedSchool;
}
