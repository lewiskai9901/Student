package com.school.management.infrastructure.extension.plugins.education.messaging;

import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 教育行业纪律违纪事件 — 迟到/缺勤/早退/打架/宿舍违规等.
 *
 * 与 AttendanceMessagingPlugin 的区别: 该插件管传统纪律分类 (DISCIPLINE 分类),
 * Attendance 管考勤系统产生的 *_EVT 事件(如 ATTENDANCE_LATE_EVT).
 */
@Component
public class DisciplineMessagingPlugin implements MessagingDomainPlugin {

    @Override public String getDomainCode() { return "discipline"; }
    @Override public String getDomainName() { return "纪律"; }

    @Override
    public List<EventTypeDef> eventTypes() {
        return List.of(
            new EventTypeDef("LATE", "迟到",
                "DISCIPLINE", "纪律", "NEGATIVE", "Clock", "#ef4444",
                List.of("USER"), "学生迟到"),
            new EventTypeDef("ABSENCE", "缺勤",
                "DISCIPLINE", "纪律", "NEGATIVE", "UserX", "#ef4444",
                List.of("USER"), "学生旷课/缺勤"),
            new EventTypeDef("EARLY_LEAVE", "早退",
                "DISCIPLINE", "纪律", "NEGATIVE", "LogOut", "#ef4444",
                List.of("USER"), "学生提前离校"),
            new EventTypeDef("CONTRABAND", "违禁品",
                "DISCIPLINE", "纪律", "NEGATIVE", "Ban", "#ef4444",
                List.of("USER"), "携带违禁品"),
            new EventTypeDef("FIGHT", "打架斗殴",
                "DISCIPLINE", "纪律", "NEGATIVE", "Swords", "#ef4444",
                List.of("USER"), "打架斗殴事件"),
            new EventTypeDef("CLASS_VIOLATION", "课堂违纪",
                "DISCIPLINE", "纪律", "NEGATIVE", "BookX", "#ef4444",
                List.of("USER", "ORG_UNIT"), "课堂纪律违规"),
            new EventTypeDef("DORM_VIOLATION", "宿舍违规",
                "DISCIPLINE", "纪律", "NEGATIVE", "Bed", "#ef4444",
                List.of("USER", "PLACE"), "宿舍管理规定违规"),
            new EventTypeDef("HYGIENE_VIOLATION", "卫生违规",
                "DISCIPLINE", "纪律", "NEGATIVE", "Trash2", "#ef4444",
                List.of("USER", "ORG_UNIT", "PLACE"), "卫生不合格"),
            new EventTypeDef("SAFETY_VIOLATION", "安全违规",
                "DISCIPLINE", "纪律", "NEGATIVE", "ShieldAlert", "#ef4444",
                List.of("USER", "ORG_UNIT", "PLACE"), "安全规定违反")
        );
    }
}
