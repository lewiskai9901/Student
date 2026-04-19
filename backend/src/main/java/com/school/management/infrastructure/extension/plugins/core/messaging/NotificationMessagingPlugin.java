package com.school.management.infrastructure.extension.plugins.core.messaging;

import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 通用通知类事件 — 公告/日程变更,任何行业都会用.
 *
 * GRADE_RELEASE 属于教育专属,放在 education/AcademicMessagingPlugin.
 */
@Component
public class NotificationMessagingPlugin implements MessagingDomainPlugin {

    @Override public String getDomainCode() { return "notification"; }
    @Override public String getDomainName() { return "通知"; }

    @Override
    public List<EventTypeDef> eventTypes() {
        return List.of(
            new EventTypeDef("ANNOUNCEMENT", "公告",
                "NOTIFICATION", "通知", "NEUTRAL", "Megaphone", "#3b82f6",
                List.of("USER", "ORG_UNIT"), "系统或组织发布的通用公告"),
            new EventTypeDef("SCHEDULE_CHANGE", "日程变更",
                "NOTIFICATION", "通知", "NEUTRAL", "Calendar", "#3b82f6",
                List.of("USER", "ORG_UNIT"), "课表/日程/会议时间变更通知")
        );
    }
}
