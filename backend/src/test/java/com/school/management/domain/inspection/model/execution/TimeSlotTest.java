package com.school.management.domain.inspection.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimeSlot 跨日时段")
class TimeSlotTest {

    @Test
    @DisplayName("普通时段 isCrossDay=false")
    void normalSlot() {
        TimeSlot ts = TimeSlot.parse("07:00-08:00");
        assertThat(ts.isCrossDay()).isFalse();
        assertThat(ts.durationMinutes()).isEqualTo(60);
    }

    @Test
    @DisplayName("夜巡跨日 22:00-02:00")
    void crossDayNightSlot() {
        TimeSlot ts = TimeSlot.parse("22:00-02:00");
        assertThat(ts.isCrossDay()).isTrue();
        assertThat(ts.durationMinutes()).isEqualTo(240); // 4 小时
        LocalDate today = LocalDate.of(2026, 5, 24);
        assertThat(ts.toStartDateTime(today)).isEqualTo(LocalDateTime.of(2026, 5, 24, 22, 0));
        assertThat(ts.toEndDateTime(today)).isEqualTo(LocalDateTime.of(2026, 5, 25, 2, 0)); // 次日
    }

    @Test
    @DisplayName("边界: end == start 也视为跨日 (24h)")
    void sameStartEnd() {
        TimeSlot ts = TimeSlot.parse("08:00-08:00");
        assertThat(ts.isCrossDay()).isTrue();
        assertThat(ts.durationMinutes()).isEqualTo(24 * 60);
    }

    @Test
    @DisplayName("parseList JSON 数组解析多段含跨日")
    void parseListWithCrossDay() {
        List<TimeSlot> slots = TimeSlot.parseList("[\"07:00-08:00\",\"22:00-02:00\"]");
        assertThat(slots).hasSize(2);
        assertThat(slots.get(0).isCrossDay()).isFalse();
        assertThat(slots.get(1).isCrossDay()).isTrue();
    }

    @Test
    @DisplayName("不合法格式跳过, 不抛异常")
    void invalidEntries() {
        assertThat(TimeSlot.parse(null)).isNull();
        assertThat(TimeSlot.parse("abc")).isNull();
        assertThat(TimeSlot.parse("25:00-26:00")).isNull(); // 非法小时
        assertThat(TimeSlot.parseList("garbage")).isEmpty();
    }
}
