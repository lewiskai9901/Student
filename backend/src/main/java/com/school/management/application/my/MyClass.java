package com.school.management.application.my;

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
