package com.school.management.infrastructure.persistence.inspection.v7.platform;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("insp_holiday_calendars")
public class HolidayCalendarPO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String calendarName;
    private Integer year;
    private String holidays;
    private String workdays;
    private Boolean isDefault;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
