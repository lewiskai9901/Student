package com.school.management.domain.inspection.model.execution;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 检查时段值对象 (V20260524_6, 2026-05-24).
 *
 * <p>支持**跨日时段** — 当 endTime ≤ startTime 时表示跨到次日 (典型场景: 22:00→02:00 夜巡).
 * 不增加 crossDay 显式 flag, 隐式从 (start, end) 推导以保持模型紧凑.
 *
 * <p>持久化格式: "HH:mm-HH:mm" (例: "07:00-08:00" / "22:00-02:00").
 * 历史 InspectionPlan.timeSlots 字段仍是 JSON 数组 ["07:00-08:00", ...].
 *
 * <p>**消费侧用例**:
 * <ul>
 *   <li>{@link #isCrossDay()} — 任务 deadline 计算时, 跨日 slot 应把 endTime 当作次日</li>
 *   <li>{@link #toEndDateTime(LocalDate)} — 跨日时返回 baseDate+1 的 endTime</li>
 *   <li>{@link #durationMinutes()} — UI 显示时段长度</li>
 * </ul>
 */
public final class TimeSlot {

    private final LocalTime startTime;
    private final LocalTime endTime;

    public TimeSlot(LocalTime startTime, LocalTime endTime) {
        this.startTime = Objects.requireNonNull(startTime, "startTime");
        this.endTime = Objects.requireNonNull(endTime, "endTime");
    }

    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }

    /** 跨日: end ≤ start 表示 endTime 在次日 (如 22:00→02:00). */
    public boolean isCrossDay() {
        return !endTime.isAfter(startTime);
    }

    /**
     * 根据基准日期返回该时段的开始时刻.
     * 跨日 slot 的开始仍是 baseDate (当晚开始, 次日清晨结束).
     */
    public LocalDateTime toStartDateTime(LocalDate baseDate) {
        return LocalDateTime.of(baseDate, startTime);
    }

    /**
     * 根据基准日期返回该时段的结束时刻.
     * 跨日时 endTime 落在 baseDate+1.
     */
    public LocalDateTime toEndDateTime(LocalDate baseDate) {
        LocalDate endDate = isCrossDay() ? baseDate.plusDays(1) : baseDate;
        return LocalDateTime.of(endDate, endTime);
    }

    /** 时段长度 (分钟); 跨日 slot 也是真实时长 (如 22:00→02:00 = 240 min). */
    public long durationMinutes() {
        return Duration.between(toStartDateTime(LocalDate.now()), toEndDateTime(LocalDate.now())).toMinutes();
    }

    /** 解析 "HH:mm-HH:mm" 单个时段. 容错: 不合法返回 null. */
    public static TimeSlot parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        int dash = s.indexOf('-');
        if (dash <= 0) return null;
        try {
            LocalTime start = LocalTime.parse(s.substring(0, dash).trim());
            LocalTime end = LocalTime.parse(s.substring(dash + 1).trim());
            return new TimeSlot(start, end);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    /**
     * 解析 JSON 数组形如 ["07:00-08:00","22:00-02:00"] 到 List&lt;TimeSlot&gt;.
     * 容错: 任一段不合法跳过, 不抛异常.
     */
    public static List<TimeSlot> parseList(String json) {
        List<TimeSlot> result = new ArrayList<>();
        if (json == null || json.isBlank()) return result;
        // 简易解析: 提取所有 HH:mm-HH:mm 子串.
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})")
                .matcher(json);
        while (m.find()) {
            try {
                LocalTime start = LocalTime.parse(m.group(1));
                LocalTime end = LocalTime.parse(m.group(2));
                result.add(new TimeSlot(start, end));
            } catch (DateTimeParseException ignored) { /* skip */ }
        }
        return result;
    }

    @Override
    public String toString() {
        return startTime + "-" + endTime + (isCrossDay() ? " (次日)" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimeSlot ts)) return false;
        return startTime.equals(ts.startTime) && endTime.equals(ts.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime);
    }
}
