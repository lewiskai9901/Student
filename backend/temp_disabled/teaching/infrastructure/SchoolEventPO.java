package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 校历事件持久化对象
 */
@Data
@TableName(value = "school_events", autoResultMap = true)
public class SchoolEventPO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long semesterId;

    private String eventCode;

    private String eventName;

    private Integer eventType;

    private LocalDate startDate;

    private LocalDate endDate;

    private LocalTime startTime;

    private LocalTime endTime;

    private Boolean allDay;

    private Boolean affectSchedule;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> affectedOrgUnits;

    private LocalDate swapToDate;

    private Integer swapWeekday;

    private String color;

    private Integer priority;

    private String description;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> attachmentUrls;

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
