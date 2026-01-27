package com.school.management.interfaces.rest.teaching;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 更新校历事件请求
 */
@Data
public class UpdateSchoolEventRequest {

    /**
     * 事件名称
     */
    private String eventName;

    /**
     * 事件类型
     */
    private Integer eventType;

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
     * 影响的组织单元ID列表
     */
    private List<Long> affectedOrgUnits;

    /**
     * 调休日期
     */
    private LocalDate swapToDate;

    /**
     * 调休按周几上课
     */
    private Integer swapWeekday;

    /**
     * 显示颜色
     */
    private String color;

    /**
     * 描述
     */
    private String description;
}
