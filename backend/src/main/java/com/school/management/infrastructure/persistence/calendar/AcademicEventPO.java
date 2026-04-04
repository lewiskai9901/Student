package com.school.management.infrastructure.persistence.calendar;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;

@Data
@TableName("academic_event")
public class AcademicEventPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long yearId;
    private Long semesterId;
    private String eventName;
    private Integer eventType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer allDay;
    private String description;
    @TableLogic
    private Integer deleted;
}
