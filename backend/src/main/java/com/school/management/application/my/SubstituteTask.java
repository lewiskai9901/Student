package com.school.management.application.my;

import java.time.LocalDate;

/**
 * /my/tasks/substitute 分配给当前教师的代课请求。
 */
public record SubstituteTask(
        Long taskId,
        String courseName,
        LocalDate scheduledDate,
        Integer startSlot,
        Integer endSlot,
        String requesterName,
        String requestedAt
) {}
