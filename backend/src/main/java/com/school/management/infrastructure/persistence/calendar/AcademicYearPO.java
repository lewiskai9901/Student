package com.school.management.infrastructure.persistence.calendar;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("academic_years")
public class AcademicYearPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String yearCode;
    private String yearName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer isCurrent;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
