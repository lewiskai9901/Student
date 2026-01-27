package com.school.management.domain.teaching.model.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 周次范围值对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekRange {

    /**
     * 起始周
     */
    private Integer startWeek;

    /**
     * 结束周
     */
    private Integer endWeek;

    /**
     * 单双周类型：0每周 1单周 2双周
     */
    @Builder.Default
    private Integer weekType = 0;

    /**
     * 创建周次范围
     */
    public static WeekRange of(int startWeek, int endWeek) {
        return WeekRange.builder()
                .startWeek(startWeek)
                .endWeek(endWeek)
                .weekType(0)
                .build();
    }

    /**
     * 创建单周范围
     */
    public static WeekRange oddWeeks(int startWeek, int endWeek) {
        return WeekRange.builder()
                .startWeek(startWeek)
                .endWeek(endWeek)
                .weekType(1)
                .build();
    }

    /**
     * 创建双周范围
     */
    public static WeekRange evenWeeks(int startWeek, int endWeek) {
        return WeekRange.builder()
                .startWeek(startWeek)
                .endWeek(endWeek)
                .weekType(2)
                .build();
    }

    /**
     * 获取总周数
     */
    public int getTotalWeeks() {
        int total = endWeek - startWeek + 1;
        if (weekType != 0) {
            return (total + 1) / 2;
        }
        return total;
    }

    /**
     * 检查周次是否在范围内
     */
    public boolean contains(int weekNumber) {
        if (weekNumber < startWeek || weekNumber > endWeek) {
            return false;
        }
        if (weekType == 0) {
            return true;
        }
        boolean isOdd = (weekNumber % 2 == 1);
        return (weekType == 1 && isOdd) || (weekType == 2 && !isOdd);
    }

    /**
     * 检查两个范围是否重叠
     */
    public boolean overlaps(WeekRange other) {
        // 检查范围重叠
        if (endWeek < other.startWeek || startWeek > other.endWeek) {
            return false;
        }
        // 检查单双周
        if (weekType != 0 && other.weekType != 0 && !weekType.equals(other.weekType)) {
            return false;
        }
        return true;
    }

    /**
     * 获取所有有效周次列表
     */
    public List<Integer> getWeekNumbers() {
        List<Integer> weeks = new ArrayList<>();
        for (int w = startWeek; w <= endWeek; w++) {
            if (contains(w)) {
                weeks.add(w);
            }
        }
        return weeks;
    }

    /**
     * 获取描述
     */
    public String getDescription() {
        String base = startWeek + "-" + endWeek + "周";
        return switch (weekType) {
            case 1 -> base + "(单)";
            case 2 -> base + "(双)";
            default -> base;
        };
    }

    /**
     * 获取单双周名称
     */
    public String getWeekTypeName() {
        return switch (weekType) {
            case 0 -> "每周";
            case 1 -> "单周";
            case 2 -> "双周";
            default -> "未知";
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WeekRange weekRange = (WeekRange) o;
        return Objects.equals(startWeek, weekRange.startWeek)
                && Objects.equals(endWeek, weekRange.endWeek)
                && Objects.equals(weekType, weekRange.weekType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startWeek, endWeek, weekType);
    }
}
