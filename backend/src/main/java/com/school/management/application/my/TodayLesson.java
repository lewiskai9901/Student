package com.school.management.application.my;

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
