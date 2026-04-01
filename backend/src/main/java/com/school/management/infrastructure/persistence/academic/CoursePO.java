package com.school.management.infrastructure.persistence.academic;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程持久化对象
 * 映射到 courses 表
 */
@Data
@TableName("courses")
public class CoursePO {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String courseCode;
    private String courseName;
    private String courseNameEn;
    private Integer courseCategory;
    private Integer courseType;
    private Integer courseNature;
    private BigDecimal credits;
    private Integer totalHours;
    private Integer theoryHours;
    private Integer practiceHours;
    private Integer weeklyHours;
    private Integer examType;

    @TableField("org_unit_id")
    private Long orgUnitId;

    private String description;
    private Integer status;

    private Long createdBy;
    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
