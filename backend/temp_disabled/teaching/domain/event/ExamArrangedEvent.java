package com.school.management.domain.teaching.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 考试安排事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamArrangedEvent {

    /**
     * 考试安排ID
     */
    private Long arrangementId;

    /**
     * 考试批次ID
     */
    private Long batchId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 考试日期
     */
    private LocalDate examDate;

    /**
     * 开始时间
     */
    private LocalTime startTime;

    /**
     * 结束时间
     */
    private LocalTime endTime;

    /**
     * 应考人数
     */
    private Integer totalStudents;

    /**
     * 事件时间
     */
    private LocalDateTime occurredAt;

    public static ExamArrangedEvent of(Long arrangementId, Long batchId, Long courseId,
                                        LocalDate examDate, LocalTime startTime, LocalTime endTime,
                                        Integer totalStudents) {
        return ExamArrangedEvent.builder()
                .arrangementId(arrangementId)
                .batchId(batchId)
                .courseId(courseId)
                .examDate(examDate)
                .startTime(startTime)
                .endTime(endTime)
                .totalStudents(totalStudents)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}
