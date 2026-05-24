package com.school.management.domain.inspection.model.execution;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RRULE 周期表达式 (RFC 5545 子集) - 2026-05-24.
 *
 * <p>不引入 ical4j 重量依赖, 自实现常用模式. 支持:
 * <pre>
 *   FREQ=MONTHLY;BYDAY=2FR              每月第 2 个周五
 *   FREQ=MONTHLY;BYDAY=-1FR             每月最后一个周五
 *   FREQ=MONTHLY;BYDAY=1MO              每月第 1 个周一
 *   FREQ=WEEKLY;BYDAY=MO,WE,FR          每周一、三、五 (与现有 cycleType=WEEKLY 等价, 提供替代写法)
 *   FREQ=DAILY;INTERVAL=2               每 2 天一次 (与 cycleType=DAILY frequency 等价)
 * </pre>
 *
 * <p>不支持 (留给后续): BYMONTH / BYSETPOS 多值 / EXDATE / UNTIL / COUNT.
 * 这些场景建议拆成多个 plan 表达 (业务上也更清晰).
 *
 * <p>{@link InspectionPlan#rrule} 非空时, scheduler 应**忽略 cycleType/frequency/scheduleDays**,
 * 改用本类计算 next occurrence.
 */
public final class RecurrenceRule {

    public enum Freq { DAILY, WEEKLY, MONTHLY }

    private final Freq freq;
    private final int interval;
    /** BYDAY 列表: List<{ordinal, weekday}>. ordinal: 1=第1个/2=第2个/-1=最后一个; null=不指定序号 (适用于 WEEKLY). */
    private final java.util.List<int[]> byDays;  // 每项 [ordinal, weekday(1=MO..7=SU)] ordinal=0 表示不指定

    private RecurrenceRule(Freq freq, int interval, java.util.List<int[]> byDays) {
        this.freq = freq;
        this.interval = interval;
        this.byDays = byDays;
    }

    public Freq getFreq() { return freq; }
    public int getInterval() { return interval; }

    private static final Map<String, Integer> WEEKDAY_CODES = new HashMap<>();
    static {
        WEEKDAY_CODES.put("MO", 1); WEEKDAY_CODES.put("TU", 2); WEEKDAY_CODES.put("WE", 3);
        WEEKDAY_CODES.put("TH", 4); WEEKDAY_CODES.put("FR", 5); WEEKDAY_CODES.put("SA", 6);
        WEEKDAY_CODES.put("SU", 7);
    }
    private static final Pattern BYDAY_TOKEN = Pattern.compile("(-?\\d+)?(MO|TU|WE|TH|FR|SA|SU)");

    /**
     * 解析 RRULE 字符串. 不合法返回 null, 不抛异常 (调用方决定是否退到 cycleType 兜底).
     */
    public static RecurrenceRule parse(String rrule) {
        if (rrule == null || rrule.isBlank()) return null;
        Map<String, String> kv = new HashMap<>();
        for (String part : rrule.split(";")) {
            String[] kv2 = part.split("=", 2);
            if (kv2.length == 2) kv.put(kv2[0].trim().toUpperCase(), kv2[1].trim().toUpperCase());
        }
        String freqStr = kv.get("FREQ");
        if (freqStr == null) return null;
        Freq freq;
        try { freq = Freq.valueOf(freqStr); } catch (IllegalArgumentException e) { return null; }
        int interval = 1;
        if (kv.containsKey("INTERVAL")) {
            try { interval = Math.max(1, Integer.parseInt(kv.get("INTERVAL"))); } catch (NumberFormatException ignored) {}
        }
        java.util.List<int[]> byDays = new java.util.ArrayList<>();
        if (kv.containsKey("BYDAY")) {
            for (String token : kv.get("BYDAY").split(",")) {
                Matcher m = BYDAY_TOKEN.matcher(token.trim());
                if (m.matches()) {
                    int ord = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
                    int wd = WEEKDAY_CODES.get(m.group(2));
                    byDays.add(new int[]{ord, wd});
                }
            }
        }
        return new RecurrenceRule(freq, interval, byDays);
    }

    /**
     * 判断 date 是否符合规则 — scheduler 按日轮询时调用.
     * (anchor 是 plan 的 startDate, 用于 INTERVAL 计算 e.g. 每 2 天).
     */
    public boolean matches(LocalDate date, LocalDate anchor) {
        Objects.requireNonNull(date);
        Objects.requireNonNull(anchor);
        if (date.isBefore(anchor)) return false;
        return switch (freq) {
            case DAILY -> matchesDaily(date, anchor);
            case WEEKLY -> matchesWeekly(date, anchor);
            case MONTHLY -> matchesMonthly(date, anchor);
        };
    }

    private boolean matchesDaily(LocalDate date, LocalDate anchor) {
        long days = anchor.until(date, java.time.temporal.ChronoUnit.DAYS);
        return days % interval == 0;
    }

    private boolean matchesWeekly(LocalDate date, LocalDate anchor) {
        long weeks = anchor.until(date, java.time.temporal.ChronoUnit.WEEKS);
        if (weeks % interval != 0) return false;
        if (byDays.isEmpty()) return date.getDayOfWeek() == anchor.getDayOfWeek();
        int wd = date.getDayOfWeek().getValue();
        return byDays.stream().anyMatch(arr -> arr[1] == wd);
    }

    private boolean matchesMonthly(LocalDate date, LocalDate anchor) {
        // 月度匹配: 仅 BYDAY={ord, wd} 模式 — 计算 date 是否为本月第 ord 个 wd
        if (byDays.isEmpty()) {
            return date.getDayOfMonth() == anchor.getDayOfMonth();
        }
        for (int[] arr : byDays) {
            int ord = arr[0], wd = arr[1];
            DayOfWeek dow = DayOfWeek.of(wd);
            if (ord == 0) {
                if (date.getDayOfWeek().getValue() == wd) return true;
            } else if (ord > 0) {
                LocalDate target = date.with(TemporalAdjusters.dayOfWeekInMonth(ord, dow));
                if (target.equals(date)) return true;
            } else {
                // 负数: 倒数第 |ord| 个
                LocalDate target = date.with(TemporalAdjusters.lastInMonth(dow));
                if (ord == -1 && target.equals(date)) return true;
                // -2, -3 ...: 不常用, 跳过
            }
        }
        return false;
    }

}
