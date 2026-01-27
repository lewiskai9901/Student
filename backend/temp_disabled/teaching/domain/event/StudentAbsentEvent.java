package com.school.management.domain.teaching.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 学生缺勤事件
 * 用于触发量化考核扣分
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAbsentEvent {

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 排课条目ID
     */
    private Long scheduleEntryId;

    /**
     * 缺勤日期
     */
    private LocalDate absentDate;

    /**
     * 缺勤类型：1旷课 2迟到 3早退 4请假
     */
    private Integer absentType;

    /**
     * 缺勤课时数
     */
    private Integer absentHours;

    /**
     * 事件时间
     */
    private LocalDateTime occurredAt;

    public static StudentAbsentEvent truancy(Long studentId, Long classId, Long courseId,
                                              Long scheduleEntryId, LocalDate absentDate, int hours) {
        return StudentAbsentEvent.builder()
                .studentId(studentId)
                .classId(classId)
                .courseId(courseId)
                .scheduleEntryId(scheduleEntryId)
                .absentDate(absentDate)
                .absentType(1) // 旷课
                .absentHours(hours)
                .occurredAt(LocalDateTime.now())
                .build();
    }

    public static StudentAbsentEvent late(Long studentId, Long classId, Long courseId,
                                           Long scheduleEntryId, LocalDate absentDate) {
        return StudentAbsentEvent.builder()
                .studentId(studentId)
                .classId(classId)
                .courseId(courseId)
                .scheduleEntryId(scheduleEntryId)
                .absentDate(absentDate)
                .absentType(2) // 迟到
                .absentHours(0)
                .occurredAt(LocalDateTime.now())
                .build();
    }

    /**
     * 获取缺勤类型名称
     */
    public String getAbsentTypeName() {
        return switch (absentType) {
            case 1 -> "旷课";
            case 2 -> "迟到";
            case 3 -> "早退";
            case 4 -> "请假";
            default -> "未知";
        };
    }

    /**
     * 是否为旷课
     */
    public boolean isTruancy() {
        return absentType == 1;
    }
}
