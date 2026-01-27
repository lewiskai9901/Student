package com.school.management.domain.teaching.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 调课事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleAdjustedEvent {

    /**
     * 调课申请ID
     */
    private Long adjustmentId;

    /**
     * 调整类型：1调课 2换教室 3代课 4停课
     */
    private Integer adjustmentType;

    /**
     * 原排课条目ID
     */
    private Long originalEntryId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 原日期
     */
    private LocalDate originalDate;

    /**
     * 新日期
     */
    private LocalDate newDate;

    /**
     * 原教师ID
     */
    private Long originalTeacherId;

    /**
     * 新教师ID（代课）
     */
    private Long newTeacherId;

    /**
     * 事件时间
     */
    private LocalDateTime occurredAt;

    public static ScheduleAdjustedEvent of(Long adjustmentId, Integer adjustmentType,
                                            Long originalEntryId, Long courseId, Long classId) {
        return ScheduleAdjustedEvent.builder()
                .adjustmentId(adjustmentId)
                .adjustmentType(adjustmentType)
                .originalEntryId(originalEntryId)
                .courseId(courseId)
                .classId(classId)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}
