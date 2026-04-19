package com.school.management.infrastructure.extension.plugins.education.messaging;

import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.school.management.infrastructure.extension.plugins.education.constants.EducationTriggerPoints.EXAM_PUBLISHED;
import static com.school.management.infrastructure.extension.plugins.education.constants.EducationTriggerPoints.SCHEDULE_PUBLISHED;

/**
 * 教务业务消息插件 — 考试 + 课程表.
 */
@Component
public class TeachingMessagingPlugin implements MessagingDomainPlugin {

    @Override public String getDomainCode() { return "teaching"; }
    @Override public String getDomainName() { return "教务"; }

    @Override
    public List<TriggerPointDef> triggerPoints() {
        return List.of(
            new TriggerPointDef(EXAM_PUBLISHED, "考试发布",
                Map.of("examId", "Long", "examName", "String",
                       "courseId", "Long", "examDate", "String"),
                "考试安排对外发布"),
            new TriggerPointDef(SCHEDULE_PUBLISHED, "课程表发布",
                Map.of("scheduleId", "Long", "semesterId", "Long", "orgUnitId", "Long"),
                "学期课程表对外发布")
        );
    }

    @Override
    public List<EventTypeDef> eventTypes() {
        return List.of(
            new EventTypeDef("EXAM_PUBLISHED_EVT", "考试发布",
                "TEACHING", "教学", "NEUTRAL", "calendar-clock", "#6366f1",
                List.of("USER", "ORG_UNIT"), "考试已发布"),
            new EventTypeDef("SCHEDULE_PUBLISHED_EVT", "课表发布",
                "TEACHING", "教学", "NEUTRAL", "calendar", "#2563eb",
                List.of("ORG_UNIT"), "课程表已发布"),

            // ─── 旧版兼容: 早期迁移种子的无 _EVT 后缀版本 (合并自 LegacyTeachingMessagingPlugin) ───
            new EventTypeDef("EXAM_PUBLISHED", "考试发布(旧版)",
                "TEACHING", "教学", "NEUTRAL", "calendar", "#8b5cf6",
                List.of("USER", "ORG_UNIT"), "[旧版] 请用 EXAM_PUBLISHED_EVT"),
            new EventTypeDef("GRADE_SUBMITTED", "成绩已录入(旧版)",
                "TEACHING", "教学", "NEUTRAL", "edit", "#f59e0b",
                List.of("ORG_UNIT"), "[旧版] 请用 GRADE_SUBMITTED_EVT"),
            new EventTypeDef("GRADE_APPROVED", "成绩审核通过(旧版)",
                "TEACHING", "教学", "NEUTRAL", "check", "#10b981",
                List.of("ORG_UNIT"), "[旧版] 请用 GRADE_APPROVED_EVT"),
            new EventTypeDef("GRADE_PUBLISHED", "成绩公示(旧版)",
                "TEACHING", "教学", "NEUTRAL", "megaphone", "#2563eb",
                List.of("ORG_UNIT"), "[旧版] 请用 GRADE_PUBLISHED_EVT"),
            new EventTypeDef("GRADE_PUBLISHED_PERSONAL", "成绩发放(旧版)",
                "TEACHING", "教学", "NEUTRAL", "user-check", "#2563eb",
                List.of("USER"), "[旧版] 请用 GRADE_PUBLISHED_PERSONAL_EVT")
        );
    }
}
