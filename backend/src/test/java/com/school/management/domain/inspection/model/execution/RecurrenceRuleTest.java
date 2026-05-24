package com.school.management.domain.inspection.model.execution;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecurrenceRule RRULE 解析 + 匹配")
class RecurrenceRuleTest {

    @Test
    @DisplayName("FREQ=MONTHLY;BYDAY=2FR 每月第 2 个周五")
    void monthlySecondFriday() {
        RecurrenceRule rrule = RecurrenceRule.parse("FREQ=MONTHLY;BYDAY=2FR");
        assertThat(rrule).isNotNull();
        // 2026-01 第 2 个周五 = 2026-01-09 (1月 2,9,16,23,30 都是周五)
        assertThat(rrule.matches(LocalDate.of(2026, 1, 9), LocalDate.of(2026, 1, 1))).isTrue();
        assertThat(rrule.matches(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1))).isFalse(); // 第1个
        assertThat(rrule.matches(LocalDate.of(2026, 1, 16), LocalDate.of(2026, 1, 1))).isFalse(); // 第3个
        // 2026-05 第 2 个周五 = 2026-05-08
        assertThat(rrule.matches(LocalDate.of(2026, 5, 8), LocalDate.of(2026, 1, 1))).isTrue();
    }

    @Test
    @DisplayName("FREQ=MONTHLY;BYDAY=-1FR 每月最后一个周五")
    void monthlyLastFriday() {
        RecurrenceRule rrule = RecurrenceRule.parse("FREQ=MONTHLY;BYDAY=-1FR");
        assertThat(rrule).isNotNull();
        // 2026-05 最后周五 = 2026-05-29
        assertThat(rrule.matches(LocalDate.of(2026, 5, 29), LocalDate.of(2026, 1, 1))).isTrue();
        assertThat(rrule.matches(LocalDate.of(2026, 5, 22), LocalDate.of(2026, 1, 1))).isFalse();
    }

    @Test
    @DisplayName("FREQ=WEEKLY;BYDAY=MO,WE,FR 每周一三五")
    void weeklyMonWedFri() {
        RecurrenceRule rrule = RecurrenceRule.parse("FREQ=WEEKLY;BYDAY=MO,WE,FR");
        LocalDate anchor = LocalDate.of(2026, 5, 18); // 周一
        assertThat(rrule.matches(LocalDate.of(2026, 5, 18), anchor)).isTrue();  // Mon
        assertThat(rrule.matches(LocalDate.of(2026, 5, 19), anchor)).isFalse(); // Tue
        assertThat(rrule.matches(LocalDate.of(2026, 5, 20), anchor)).isTrue();  // Wed
        assertThat(rrule.matches(LocalDate.of(2026, 5, 22), anchor)).isTrue();  // Fri
        assertThat(rrule.matches(LocalDate.of(2026, 5, 23), anchor)).isFalse(); // Sat
    }

    @Test
    @DisplayName("FREQ=DAILY;INTERVAL=2 每 2 天")
    void dailyEvery2Days() {
        RecurrenceRule rrule = RecurrenceRule.parse("FREQ=DAILY;INTERVAL=2");
        LocalDate anchor = LocalDate.of(2026, 5, 1);
        assertThat(rrule.matches(anchor, anchor)).isTrue();
        assertThat(rrule.matches(anchor.plusDays(1), anchor)).isFalse();
        assertThat(rrule.matches(anchor.plusDays(2), anchor)).isTrue();
        assertThat(rrule.matches(anchor.plusDays(7), anchor)).isFalse();
        assertThat(rrule.matches(anchor.plusDays(8), anchor)).isTrue();
    }

    @Test
    @DisplayName("非法 RRULE 返回 null")
    void invalidParse() {
        assertThat(RecurrenceRule.parse(null)).isNull();
        assertThat(RecurrenceRule.parse("")).isNull();
        assertThat(RecurrenceRule.parse("BAD")).isNull();
        assertThat(RecurrenceRule.parse("FREQ=YEARLY")).isNull(); // 暂不支持 YEARLY
    }
}
