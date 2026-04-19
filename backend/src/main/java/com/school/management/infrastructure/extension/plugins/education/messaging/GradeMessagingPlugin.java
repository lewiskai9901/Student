package com.school.management.infrastructure.extension.plugins.education.messaging;

import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.school.management.infrastructure.extension.plugins.education.constants.EducationTriggerPoints.*;

/**
 * 成绩业务消息插件.
 */
@Component
public class GradeMessagingPlugin implements MessagingDomainPlugin {

    @Override
    public String getDomainCode() { return "grade"; }

    @Override
    public String getDomainName() { return "成绩"; }

    @Override
    public List<TriggerPointDef> triggerPoints() {
        Map<String, String> commonSchema = Map.of(
            "batchId", "Long", "batchName", "String",
            "semesterId", "Long", "courseId", "Long", "orgUnitId", "Long"
        );
        return List.of(
            new TriggerPointDef(GRADE_SUBMITTED, "成绩录入",
                commonSchema, "教师录入成绩后提交"),
            new TriggerPointDef(GRADE_APPROVED, "成绩审核通过",
                commonSchema, "教务审核通过成绩批次"),
            new TriggerPointDef(GRADE_PUBLISHED, "成绩公示(班级)",
                commonSchema, "成绩对外公示,班级级事件"),
            new TriggerPointDef(GRADE_PUBLISHED_PERSONAL, "成绩发放(个人)",
                Map.of("studentId", "Long", "studentName", "String",
                       "batchId", "Long", "batchName", "String"),
                "成绩定向发送给学生本人及其家长")
        );
    }

    @Override
    public List<EventTypeDef> eventTypes() {
        return List.of(
            new EventTypeDef("GRADE_SUBMITTED_EVT", "成绩已录入",
                "ACADEMIC", "学业", "NEUTRAL", "edit-3", "#2563eb",
                List.of("ORG_UNIT"), "教师录入完毕,待审核"),

            new EventTypeDef("GRADE_APPROVED_EVT", "成绩已审核",
                "ACADEMIC", "学业", "POSITIVE", "check-circle", "#10b981",
                List.of("ORG_UNIT"), "教务审核通过"),

            new EventTypeDef("GRADE_PUBLISHED_EVT", "成绩已公示",
                "ACADEMIC", "学业", "NEUTRAL", "megaphone", "#f59e0b",
                List.of("ORG_UNIT"), "成绩对外公示"),

            new EventTypeDef("GRADE_PUBLISHED_PERSONAL_EVT", "成绩已发放",
                "ACADEMIC", "学业", "NEUTRAL", "mail", "#0ea5e9",
                List.of("USER"), "定向通知学生/家长")
        );
    }

    @Override
    public List<DefaultTriggerDef> defaultTriggers() {
        return List.of(
            new DefaultTriggerDef(GRADE_SUBMITTED, "GRADE_SUBMITTED_EVT",
                List.of(Map.of("type", "ORG_UNIT", "id", "{{orgUnitId}}"))),
            new DefaultTriggerDef(GRADE_APPROVED, "GRADE_APPROVED_EVT",
                List.of(Map.of("type", "ORG_UNIT", "id", "{{orgUnitId}}"))),
            new DefaultTriggerDef(GRADE_PUBLISHED, "GRADE_PUBLISHED_EVT",
                List.of(Map.of("type", "ORG_UNIT", "id", "{{orgUnitId}}"))),
            new DefaultTriggerDef(GRADE_PUBLISHED_PERSONAL, "GRADE_PUBLISHED_PERSONAL_EVT",
                List.of(Map.of("type", "USER", "id", "{{studentId}}")))
        );
    }
}
