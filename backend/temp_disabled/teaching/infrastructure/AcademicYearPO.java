package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学年持久化对象
 */
@Data
@TableName("academic_years")
public class AcademicYearPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String yearCode;

    private String yearName;

    private LocalDate startDate;

    private LocalDate endDate;

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
