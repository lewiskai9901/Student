package com.school.management.infrastructure.extension.plugins.education.messaging;

import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.school.management.infrastructure.extension.plugins.education.constants.EducationTriggerPoints.ATTENDANCE_RECORDED;

/**
 * 考勤业务消息插件.
 */
@Component
public class AttendanceMessagingPlugin implements MessagingDomainPlugin {

    @Override public String getDomainCode() { return "attendance"; }
    @Override public String getDomainName() { return "考勤"; }

    @Override
    public List<TriggerPointDef> triggerPoints() {
        return List.of(
            new TriggerPointDef(ATTENDANCE_RECORDED, "考勤记录",
                Map.of("userId", "Long", "orgUnitId", "Long",
                       "attendanceType", "String", "attendanceDate", "String"),
                "学生/员工考勤记录(到/迟/缺/请假)")
        );
    }

    @Override
    public List<EventTypeDef> eventTypes() {
        return List.of(
            new EventTypeDef("ATTENDANCE_LATE_EVT", "迟到",
                "DISCIPLINE", "纪律", "NEGATIVE", "clock-alert", "#f59e0b",
                List.of("USER"), "学生迟到"),
            new EventTypeDef("ATTENDANCE_ABSENT_EVT", "缺勤",
                "DISCIPLINE", "纪律", "NEGATIVE", "x-circle", "#ef4444",
                List.of("USER"), "学生无故缺勤"),
            new EventTypeDef("ATTENDANCE_LEAVE_EVT", "请假",
                "PERSONNEL", "人事", "NEUTRAL", "calendar-minus", "#6b7280",
                List.of("USER"), "学生请假")
        );
    }
}
