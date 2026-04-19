package com.school.management.infrastructure.extension.plugins.education.messaging;

import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.school.management.infrastructure.extension.plugins.education.constants.EducationTriggerPoints.ENROLLMENT_ADMITTED;
import static com.school.management.infrastructure.extension.plugins.education.constants.EducationTriggerPoints.ENROLLMENT_REGISTERED;
import static com.school.management.infrastructure.extension.plugins.education.constants.EducationTriggerPoints.STUDENT_ENROLLED;
import static com.school.management.infrastructure.extension.plugins.education.constants.EducationTriggerPoints.STUDENT_STATUS_CHANGED;

/**
 * 招生与学籍业务消息插件.
 */
@Component
public class EnrollmentMessagingPlugin implements MessagingDomainPlugin {

    @Override public String getDomainCode() { return "enrollment"; }
    @Override public String getDomainName() { return "招生与学籍"; }

    @Override
    public List<TriggerPointDef> triggerPoints() {
        return List.of(
            new TriggerPointDef(ENROLLMENT_ADMITTED, "录取确认",
                Map.of("applicantId", "Long", "classId", "Long"),
                "招生录取确认"),
            new TriggerPointDef(ENROLLMENT_REGISTERED, "新生报到",
                Map.of("studentId", "Long", "classId", "Long"),
                "新生报到注册"),
            new TriggerPointDef(STUDENT_ENROLLED, "学籍注册",
                Map.of("studentId", "Long", "classId", "Long"),
                "学生学籍正式建立"),
            new TriggerPointDef(STUDENT_STATUS_CHANGED, "学籍状态变更",
                Map.of("studentId", "Long", "oldStatus", "String", "newStatus", "String"),
                "休学/复学/毕业/退学")
        );
    }

    @Override
    public List<EventTypeDef> eventTypes() {
        return List.of(
            new EventTypeDef("ENROLLMENT_ADMITTED_EVT", "录取通知",
                "ACADEMIC", "学业", "POSITIVE", "mail-check", "#10b981",
                List.of("USER"), "学生被录取"),
            new EventTypeDef("ENROLLMENT_REGISTERED_EVT", "报到完成",
                "ACADEMIC", "学业", "POSITIVE", "user-check", "#10b981",
                List.of("USER"), "新生完成报到"),
            new EventTypeDef("STUDENT_STATUS_CHANGED_EVT", "学籍变更",
                "ACADEMIC", "学业", "NEUTRAL", "refresh-cw", "#6b7280",
                List.of("USER"), "学生学籍状态发生变更")
        );
    }
}
