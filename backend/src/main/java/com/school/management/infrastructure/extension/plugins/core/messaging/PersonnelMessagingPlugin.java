package com.school.management.infrastructure.extension.plugins.core.messaging;

import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 人事变动通用事件 — 入职/离职/调动等,任何行业都会用.
 *
 * 事件类型已在 DB 种子里存在(V*.sql 迁移),此插件只负责认领 +
 * 打上 industry=CORE / plugin_class 标签,让插件平台不再归为"历史数据".
 */
@Component
public class PersonnelMessagingPlugin implements MessagingDomainPlugin {

    @Override public String getDomainCode() { return "personnel"; }
    @Override public String getDomainName() { return "人事"; }

    @Override
    public List<EventTypeDef> eventTypes() {
        return List.of(
            new EventTypeDef("CHECKIN", "入住",
                "PERSONNEL", "人事", "NEUTRAL", "Home", "#6b7280",
                List.of("USER"), "用户入住场所(宿舍/酒店/设施均可)"),
            new EventTypeDef("CHECKOUT", "退宿",
                "PERSONNEL", "人事", "NEUTRAL", "DoorOpen", "#6b7280",
                List.of("USER"), "用户退出场所占用"),
            new EventTypeDef("ENROLLED", "入学",
                "PERSONNEL", "人事", "NEUTRAL", "School", "#6b7280",
                List.of("USER"), "用户入学/入职/注册"),
            new EventTypeDef("MEMBER_JOIN", "成员加入",
                "PERSONNEL", "人事", "NEUTRAL", "UserPlus", "#6b7280",
                List.of("USER", "ORG_UNIT"), "成员加入组织单元"),
            new EventTypeDef("MEMBER_LEAVE", "成员离开",
                "PERSONNEL", "人事", "NEUTRAL", "UserMinus", "#6b7280",
                List.of("USER", "ORG_UNIT"), "成员退出组织单元"),
            new EventTypeDef("TRANSFER", "调动",
                "PERSONNEL", "人事", "NEUTRAL", "ArrowRightLeft", "#6b7280",
                List.of("USER"), "用户在组织间调动")
        );
    }
}
