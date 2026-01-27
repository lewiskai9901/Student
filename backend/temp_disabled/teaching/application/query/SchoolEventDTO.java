package com.school.management.application.teaching.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 校历事件DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolEventDTO {

    private Long id;

    /**
     * 所属学期ID
     */
    private Long semesterId;

    /**
     * 事件代码
     */
    private String eventCode;

    /**
     * 事件名称
     */
    private String eventName;

    /**
     * 事件类型
     */
    private Integer eventType;

    /**
     * 事件类型名称
     */
    private String eventTypeName;

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 是否全天事件
     */
    private Boolean allDay;

    /**
     * 是否影响排课
     */
    private Boolean affectSchedule;

    /**
     * 调休日期
     */
    private LocalDate swapToDate;

    /**
     * 调休按周几上课
     */
    private Integer swapWeekday;

    /**
     * 日历显示颜色
     */
    private String color;

    /**
     * 描述
     */
    private String description;

    /**
     * 状态
     */
    private Integer status;
}
