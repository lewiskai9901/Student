package com.school.management.infrastructure.extension.plugins.education;

import com.school.management.infrastructure.extension.DataResourceProvider;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.school.management.infrastructure.extension.DataResourceProvider.DataResourceDef.of;

/**
 * 教育行业数据资源 — 学生类加年级/班级/专业维度,
 * 成绩/考试类没有年级维度 (批次本身跨年级), 宿舍等组织向走 5 种常规 scope.
 *
 * 本 Provider 覆盖 V20260429_1 migration 中 EDU 段的 seed, 启动时 UPDATE 覆盖.
 */
@Component
public class EducationDataResourceProvider implements DataResourceProvider {

    @Override
    public List<DataResourceDef> getDataResources() {
        return List.of(
            // 学生类: 加年级/班级/专业维度
            of("student",    "ALL", "BY_GRADE", "BY_CLASS", "BY_MAJOR", "SELF", "CUSTOM"),
            of("attendance", "ALL", "BY_GRADE", "BY_CLASS", "BY_MAJOR", "SELF", "CUSTOM"),

            // 成绩/考试类: 跨年级, 不给 BY_GRADE
            of("grade_batch",   "ALL", "BY_CLASS", "SELF", "CUSTOM"),
            of("student_grade", "ALL", "BY_CLASS", "SELF", "CUSTOM"),
            of("exam",          "ALL", "BY_CLASS", "SELF", "CUSTOM"),
            of("exam_batch",    "ALL", "BY_CLASS", "SELF", "CUSTOM"),

            // 教学任务/班级: 按班级/专业
            of("teaching_task", "ALL", "BY_CLASS", "BY_MAJOR", "SELF", "CUSTOM"),
            of("school_class",  "ALL", "BY_CLASS", "BY_MAJOR", "SELF", "CUSTOM"),

            // 宿舍 / enrollment 等组织向
            of("dormitory",  "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM"),
            of("enrollment", "ALL", "DEPARTMENT_AND_BELOW", "DEPARTMENT", "SELF", "CUSTOM")
        );
    }
}
