package com.school.management.domain.teaching.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成绩录入事件
 * 用于触发综合测评更新等后续操作
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeRecordedEvent {

    /**
     * 成绩ID
     */
    private Long gradeId;

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 总评成绩
     */
    private BigDecimal totalScore;

    /**
     * 绩点
     */
    private BigDecimal gradePoint;

    /**
     * 学分
     */
    private BigDecimal credits;

    /**
     * 是否通过
     */
    private Boolean passed;

    /**
     * 录入教师ID
     */
    private Long inputTeacherId;

    /**
     * 事件时间
     */
    private LocalDateTime occurredAt;

    public static GradeRecordedEvent of(Long gradeId, Long semesterId, Long courseId,
                                         Long studentId, Long classId, BigDecimal totalScore,
                                         BigDecimal gradePoint, BigDecimal credits, Boolean passed,
                                         Long inputTeacherId) {
        return GradeRecordedEvent.builder()
                .gradeId(gradeId)
                .semesterId(semesterId)
                .courseId(courseId)
                .studentId(studentId)
                .classId(classId)
                .totalScore(totalScore)
                .gradePoint(gradePoint)
                .credits(credits)
                .passed(passed)
                .inputTeacherId(inputTeacherId)
                .occurredAt(LocalDateTime.now())
                .build();
    }
}
