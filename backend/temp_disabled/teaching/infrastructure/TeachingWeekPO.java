package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 教学周持久化对象
 */
@Data
@TableName("teaching_weeks")
public class TeachingWeekPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long semesterId;

    private Integer weekNumber;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer weekType;

    private String weekLabel;

    private Boolean isActive;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
