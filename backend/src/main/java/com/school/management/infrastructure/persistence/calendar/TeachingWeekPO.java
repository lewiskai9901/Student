package com.school.management.infrastructure.persistence.calendar;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("academic_weeks")
public class TeachingWeekPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long semesterId;
    private Integer weekNumber;
    private String weekName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer isCurrent;
    private Integer status;
}
