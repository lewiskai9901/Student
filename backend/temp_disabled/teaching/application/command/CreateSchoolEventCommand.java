package com.school.management.application.teaching.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 创建校历事件命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSchoolEventCommand {

    /**
     * 所属学期ID（跨学期事件可为空）
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
    @Builder.Default
    private Boolean allDay = true;

    /**
     * 是否影响正常排课
     */
    @Builder.Default
    private Boolean affectSchedule = false;

    /**
     * 影响的组织单元ID列表
     */
    private List<Long> affectedOrgUnits;

    /**
     * 调休：调到哪天补课
     */
    private LocalDate swapToDate;

    /**
     * 调休：按周几的课表上课
     */
    private Integer swapWeekday;

    /**
     * 日历显示颜色
     */
    @Builder.Default
    private String color = "#1890ff";

    /**
     * 详细描述
     */
    private String description;

    /**
     * 操作人ID
     */
    private Long operatorId;
}
