package com.school.management.application.teaching;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * 实况课表生成服务
 *
 * 将基准课表(schedule_entries)按教学周展开为每日实例(schedule_instances)，
 * 并叠加校历事件(academic_event)的影响。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InstanceGenerationService {

    private final JdbcTemplate jdbc;

    /**
     * 从基准课表生成全学期实况
     * 流程: 清除旧实况 → 读教学周 → 读基准条目 → 逐周逐条展开 → 叠加校历事件
     */
    @Transactional
    public Map<String, Object> generateInstances(Long semesterId) {
        log.info("生成实况课表: semesterId={}", semesterId);

        // 1. 清除旧的自动生成实例(保留手动/调课生成的)
        int deleted = jdbc.update(
            "DELETE FROM schedule_instances WHERE semester_id = ? AND source_type = 0", semesterId);
        log.info("清除旧实况 {} 条", deleted);

        // 2. 读教学周
        List<Map<String, Object>> weeks = jdbc.queryForList(
            "SELECT week_number, start_date, end_date FROM academic_weeks " +
            "WHERE semester_id = ? ORDER BY week_number", semesterId);
        if (weeks.isEmpty()) {
            return Map.of("generated", 0, "message", "无教学周数据，请先在校历中生成教学周");
        }

        // 3. 读基准课表
        List<Map<String, Object>> entries = jdbc.queryForList(
            "SELECT id, course_id, org_unit_id, teacher_id, classroom_id, " +
            "weekday, start_slot, end_slot, start_week, end_week, week_type " +
            "FROM schedule_entries WHERE semester_id = ? AND deleted = 0 AND entry_status = 1",
            semesterId);
        if (entries.isEmpty()) {
            return Map.of("generated", 0, "message", "无基准排课数据");
        }

        // 4. 逐周逐条展开
        int count = 0;
        for (Map<String, Object> week : weeks) {
            int weekNum = ((Number) week.get("week_number")).intValue();
            LocalDate weekStart = ((java.sql.Date) week.get("start_date")).toLocalDate();

            for (Map<String, Object> entry : entries) {
                int entryStartWeek = entry.get("start_week") != null ? ((Number) entry.get("start_week")).intValue() : 1;
                int entryEndWeek = entry.get("end_week") != null ? ((Number) entry.get("end_week")).intValue() : 20;
                int weekType = entry.get("week_type") != null ? ((Number) entry.get("week_type")).intValue() : 0;

                // 周次范围过滤
                if (weekNum < entryStartWeek || weekNum > entryEndWeek) continue;

                // 单双周过滤
                if (weekType == 1 && weekNum % 2 == 0) continue; // 单周课，偶数周跳过
                if (weekType == 2 && weekNum % 2 == 1) continue; // 双周课，奇数周跳过

                int weekday = ((Number) entry.get("weekday")).intValue();
                // 计算实际日期: weekStart是周一，weekday=1是周一
                LocalDate actualDate = weekStart.plusDays(weekday - 1);

                int startSlot = ((Number) entry.get("start_slot")).intValue();
                int endSlot = ((Number) entry.get("end_slot")).intValue();
                double actualHours = endSlot - startSlot + 1;

                jdbc.update(
                    "INSERT INTO schedule_instances (id, entry_id, semester_id, actual_date, weekday, " +
                    "week_number, start_slot, end_slot, course_id, org_unit_id, teacher_id, classroom_id, " +
                    "status, source_type, actual_hours, deleted) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, ?, 0)",
                    IdWorker.getId(), entry.get("id"), semesterId, actualDate, weekday,
                    weekNum, startSlot, endSlot,
                    entry.get("course_id"), entry.get("org_unit_id"),
                    entry.get("teacher_id"), entry.get("classroom_id"),
                    actualHours);
                count++;
            }
        }

        // 5. 叠加校历事件
        int affected = applyAllCalendarEvents(semesterId);

        log.info("生成实况 {} 条, 校历影响 {} 条", count, affected);
        return Map.of("generated", count, "calendarAffected", affected);
    }

    /**
     * 叠加所有校历事件到实况课表
     */
    private int applyAllCalendarEvents(Long semesterId) {
        List<Map<String, Object>> events = jdbc.queryForList(
            "SELECT id, event_name, start_date, end_date, affect_type, affect_scope, " +
            "substitute_weekday, affect_slots " +
            "FROM academic_event WHERE semester_id = ? AND deleted = 0 AND affect_type > 0",
            semesterId);

        int total = 0;
        for (Map<String, Object> event : events) {
            total += applySingleEvent(semesterId, event);
        }
        return total;
    }

    /**
     * 应用单个校历事件到实况
     */
    @Transactional
    public int applyCalendarEvent(Long eventId) {
        Map<String, Object> event = jdbc.queryForMap(
            "SELECT id, semester_id, event_name, start_date, end_date, affect_type, " +
            "affect_scope, substitute_weekday, affect_slots " +
            "FROM academic_event WHERE id = ? AND deleted = 0", eventId);
        Long semesterId = ((Number) event.get("semester_id")).longValue();
        return applySingleEvent(semesterId, event);
    }

    private int applySingleEvent(Long semesterId, Map<String, Object> event) {
        int affectType = event.get("affect_type") != null ? ((Number) event.get("affect_type")).intValue() : 0;
        if (affectType == 0) return 0;

        String eventName = (String) event.get("event_name");
        Long eventId = ((Number) event.get("id")).longValue();
        LocalDate startDate = ((java.sql.Date) event.get("start_date")).toLocalDate();
        LocalDate endDate = event.get("end_date") != null
            ? ((java.sql.Date) event.get("end_date")).toLocalDate() : startDate;

        int affected = 0;

        switch (affectType) {
            case 1: // 全天停课
                affected = jdbc.update(
                    "UPDATE schedule_instances SET status = 1, cancel_reason = ?, source_id = ? " +
                    "WHERE semester_id = ? AND actual_date BETWEEN ? AND ? AND status = 0 AND deleted = 0",
                    eventName, eventId, semesterId, startDate, endDate);
                break;

            case 2: // 半天停课
                String affectSlots = (String) event.get("affect_slots");
                if (affectSlots != null && affectSlots.contains("-")) {
                    String[] parts = affectSlots.split("-");
                    int slotStart = Integer.parseInt(parts[0].trim());
                    int slotEnd = Integer.parseInt(parts[1].trim());
                    affected = jdbc.update(
                        "UPDATE schedule_instances SET status = 1, cancel_reason = ?, source_id = ? " +
                        "WHERE semester_id = ? AND actual_date BETWEEN ? AND ? " +
                        "AND start_slot >= ? AND end_slot <= ? AND status = 0 AND deleted = 0",
                        eventName, eventId, semesterId, startDate, endDate, slotStart, slotEnd);
                }
                break;

            case 3: // 补课日(按某天课表上课)
                Integer subWeekday = event.get("substitute_weekday") != null
                    ? ((Number) event.get("substitute_weekday")).intValue() : null;
                if (subWeekday != null) {
                    affected = generateSubstituteInstances(semesterId, startDate, subWeekday, eventId, eventName);
                }
                break;

            case 4: // 考试周
                affected = jdbc.update(
                    "UPDATE schedule_instances SET status = 1, cancel_reason = ?, source_id = ? " +
                    "WHERE semester_id = ? AND actual_date BETWEEN ? AND ? AND status = 0 AND deleted = 0",
                    "考试周-" + eventName, eventId, semesterId, startDate, endDate);
                break;
        }

        log.info("校历事件[{}]影响 {} 条实况", eventName, affected);
        return affected;
    }

    /**
     * 生成补课日实例: 在指定日期按某天课表生成课程
     */
    private int generateSubstituteInstances(Long semesterId, LocalDate substituteDate,
                                             int sourceWeekday, Long eventId, String eventName) {
        // 查基准课表中该weekday的所有条目
        List<Map<String, Object>> entries = jdbc.queryForList(
            "SELECT id, course_id, org_unit_id, teacher_id, classroom_id, start_slot, end_slot " +
            "FROM schedule_entries WHERE semester_id = ? AND weekday = ? AND deleted = 0 AND entry_status = 1",
            semesterId, sourceWeekday);

        int count = 0;
        for (Map<String, Object> entry : entries) {
            int startSlot = ((Number) entry.get("start_slot")).intValue();
            int endSlot = ((Number) entry.get("end_slot")).intValue();

            jdbc.update(
                "INSERT INTO schedule_instances (id, entry_id, semester_id, actual_date, weekday, " +
                "start_slot, end_slot, course_id, org_unit_id, teacher_id, classroom_id, " +
                "status, source_type, source_id, actual_hours, cancel_reason, deleted) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 3, 2, ?, ?, ?, 0)",
                IdWorker.getId(), entry.get("id"), semesterId, substituteDate,
                substituteDate.getDayOfWeek().getValue(),
                startSlot, endSlot,
                entry.get("course_id"), entry.get("org_unit_id"),
                entry.get("teacher_id"), entry.get("classroom_id"),
                eventId, (double)(endSlot - startSlot + 1), "补课-" + eventName);
            count++;
        }
        return count;
    }
}
