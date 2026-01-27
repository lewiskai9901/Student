package com.school.management.domain.teaching.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * 时间段值对象（周几+节次）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {

    /**
     * 周几（1-7，1=周一）
     */
    private Integer weekday;

    /**
     * 节次
     */
    private Integer slot;

    /**
     * 获取周几名称
     */
    public String getWeekdayName() {
        return switch (weekday) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            case 4 -> "周四";
            case 5 -> "周五";
            case 6 -> "周六";
            case 7 -> "周日";
            default -> "未知";
        };
    }

    /**
     * 获取描述
     */
    public String getDescription() {
        return getWeekdayName() + "第" + slot + "节";
    }

    /**
     * 创建时间段
     */
    public static TimeSlot of(int weekday, int slot) {
        return TimeSlot.builder()
                .weekday(weekday)
                .slot(slot)
                .build();
    }

    /**
     * 是否为上午
     */
    public boolean isMorning() {
        return slot >= 1 && slot <= 4;
    }

    /**
     * 是否为下午
     */
    public boolean isAfternoon() {
        return slot >= 5 && slot <= 8;
    }

    /**
     * 是否为晚上
     */
    public boolean isEvening() {
        return slot >= 9;
    }

    /**
     * 是否为工作日
     */
    public boolean isWeekday() {
        return weekday >= 1 && weekday <= 5;
    }

    /**
     * 是否为周末
     */
    public boolean isWeekend() {
        return weekday == 6 || weekday == 7;
    }

    /**
     * 获取下一个时间段
     */
    public TimeSlot next() {
        if (slot < 10) {
            return TimeSlot.of(weekday, slot + 1);
        } else if (weekday < 7) {
            return TimeSlot.of(weekday + 1, 1);
        }
        return null;
    }

    /**
     * 获取上一个时间段
     */
    public TimeSlot previous() {
        if (slot > 1) {
            return TimeSlot.of(weekday, slot - 1);
        } else if (weekday > 1) {
            return TimeSlot.of(weekday - 1, 10);
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeSlot timeSlot = (TimeSlot) o;
        return Objects.equals(weekday, timeSlot.weekday) && Objects.equals(slot, timeSlot.slot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(weekday, slot);
    }
}
