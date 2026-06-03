package com.school.management.infrastructure.extension.plugins.education.application.mydashboard;

import java.util.List;

/**
 * /my/classes 一个班级及当前教师在其中的角色摘要。
 */
public record MyClass(
        Long classId,
        String className,
        Integer studentCount,
        boolean isHeadTeacher,
        List<String> subjects
) {}
