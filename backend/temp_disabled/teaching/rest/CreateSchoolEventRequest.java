package com.school.management.interfaces.rest.teaching;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 创建校历事件请求
 */
@Data
public class CreateSchoolEventRequest {

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
     * 事件类型：1法定节假日 2学校假期 3校级活动 4调休补课 5临时停课
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
    private Boolean allDay = true;

    /**
     * 是否影响排课
     */
    private Boolean affectSchedule = false;

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
    private String color = "#1890ff";

    /**
     * 描述
     */
    private String description;
}
