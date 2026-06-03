package com.school.management.infrastructure.extension.plugins.education.application.mydashboard;

/**
 * /my/schedule/today 一节课次。
 */
public record TodayLesson(
        Long instanceId,
        Integer startSlot,
        Integer endSlot,
        String startTime,
        String endTime,
        String courseName,
        String className,
        String classroomName,
        Integer status,
        boolean canSign
) {}
