package com.school.management.application.inspection.evaluation;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * 评级周期 key 与边界解析 — 评级引擎完美架构.
 *
 * <p>periodKey 形态:
 * <ul>
 *   <li>DAILY   → "2026-05-23"</li>
 *   <li>WEEKLY  → "2026-W22"  (ISO week)</li>
 *   <li>MONTHLY → "2026-05"</li>
 *   <li>COUNT   → "COUNT#7"   (第 7 个 threshold 桶)</li>
 *   <li>MANUAL  → "MANUAL:2026-05-01_2026-05-31"</li>
 * </ul>
 */
public final class PeriodKeyResolver {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private PeriodKeyResolver() {
    }

    public static String forDaily(LocalDate day) {
        return DAY.format(day);
    }

    public static String forWeekly(LocalDate anyDayInWeek) {
        WeekFields wf = WeekFields.ISO;
        int week = anyDayInWeek.get(wf.weekOfWeekBasedYear());
        int year = anyDayInWeek.get(wf.weekBasedYear());
        return String.format("%04d-W%02d", year, week);
    }

    public static String forMonthly(LocalDate anyDayInMonth) {
        return MONTH.format(anyDayInMonth);
    }

    public static String forCount(int bucket) {
        return "COUNT#" + bucket;
    }

    public static String forManual(LocalDate start, LocalDate end) {
        return "MANUAL:" + DAY.format(start) + "_" + DAY.format(end);
    }

    /** evaluationPeriod (DAILY/WEEKLY/MONTHLY) → [start, end] inclusive. */
    public static LocalDate[] boundariesFor(String evaluationPeriod, LocalDate ref) {
        return switch (evaluationPeriod != null ? evaluationPeriod : "DAILY") {
            case "WEEKLY" -> {
                LocalDate monday = ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new LocalDate[]{monday, monday.plusDays(6)};
            }
            case "MONTHLY" -> {
                LocalDate first = ref.withDayOfMonth(1);
                yield new LocalDate[]{first, ref.with(TemporalAdjusters.lastDayOfMonth())};
            }
            default -> new LocalDate[]{ref, ref}; // DAILY / PER_TASK
        };
    }

    public static String forPeriod(String evaluationPeriod, LocalDate periodStart) {
        return switch (evaluationPeriod != null ? evaluationPeriod : "DAILY") {
            case "WEEKLY" -> forWeekly(periodStart);
            case "MONTHLY" -> forMonthly(periodStart);
            default -> forDaily(periodStart);
        };
    }
}
