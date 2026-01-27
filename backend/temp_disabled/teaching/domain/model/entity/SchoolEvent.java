package com.school.management.domain.teaching.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 校历事件实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolEvent {

    private Long id;

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
     * 是否影响正常排课：1影响（停课）0不影响
     */
    @Builder.Default
    private Boolean affectSchedule = false;

    /**
     * 影响的组织单元ID列表（空表示全校）
     */
    private List<Long> affectedOrgUnits;

    /**
     * 调休：调到哪天补课
     */
    private LocalDate swapToDate;

    /**
     * 调休：按周几的课表上课（1-7）
     */
    private Integer swapWeekday;

    /**
     * 日历显示颜色
     */
    @Builder.Default
    private String color = "#1890ff";

    /**
     * 显示优先级
     */
    @Builder.Default
    private Integer priority = 0;

    /**
     * 详细描述
     */
    private String description;

    /**
     * 附件URL列表
     */
    private List<String> attachmentUrls;

    /**
     * 状态：1正常 0取消
     */
    @Builder.Default
    private Integer status = 1;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    /**
     * 获取事件类型名称
     */
    public String getEventTypeName() {
        return switch (eventType) {
            case 1 -> "法定节假日";
            case 2 -> "学校假期";
            case 3 -> "校级活动";
            case 4 -> "调休补课";
            case 5 -> "临时停课";
            default -> "未知";
        };
    }

    /**
     * 是否为假期类型
     */
    public boolean isHoliday() {
        return eventType == 1 || eventType == 2;
    }

    /**
     * 是否为调休
     */
    public boolean isSwap() {
        return eventType == 4;
    }

    /**
     * 是否影响全校
     */
    public boolean affectsWholeSchool() {
        return affectedOrgUnits == null || affectedOrgUnits.isEmpty();
    }

    /**
     * 检查是否影响指定组织单元
     */
    public boolean affectsOrgUnit(Long orgUnitId) {
        if (affectsWholeSchool()) {
            return true;
        }
        return affectedOrgUnits.contains(orgUnitId);
    }

    /**
     * 获取事件天数
     */
    public long getDays() {
        if (startDate != null && endDate != null) {
            return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }
        return 0;
    }

    /**
     * 检查日期是否在事件期间
     */
    public boolean containsDate(LocalDate date) {
        return date != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * 取消事件
     */
    public void cancel() {
        this.status = 0;
    }

    /**
     * 恢复事件
     */
    public void restore() {
        this.status = 1;
    }
}
