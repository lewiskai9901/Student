package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学期持久化对象
 */
@Data
@TableName("semesters")
public class SemesterPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long academicYearId;

    private String semesterCode;

    private String semesterName;

    private Integer semesterType;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalDate teachingStartDate;

    private LocalDate teachingEndDate;

    private LocalDate examStartDate;

    private LocalDate examEndDate;

    private Integer totalTeachingWeeks;

    private Boolean isCurrent;

    private Integer status;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
