package com.school.management.domain.teaching.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 课表发布事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulePublishedEvent {

    /**
     * 学期ID
     */
    private Long semesterId;

    /**
     * 发布人ID
     */
    private Long publishedBy;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 课表条目数量
     */
    private Integer entryCount;

    public static SchedulePublishedEvent of(Long semesterId, Long publishedBy, int entryCount) {
        return SchedulePublishedEvent.builder()
                .semesterId(semesterId)
                .publishedBy(publishedBy)
                .publishedAt(LocalDateTime.now())
                .entryCount(entryCount)
                .build();
    }
}
