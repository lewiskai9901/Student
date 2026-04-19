package com.school.management.infrastructure.extension.plugins.core.messaging;

import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.school.management.infrastructure.extension.plugins.core.constants.CoreTriggerPoints.ORG_UNIT_CREATED;

/**
 * 组织单元业务消息插件 — 通用核心.
 */
@Component
public class OrganizationMessagingPlugin implements MessagingDomainPlugin {

    @Override public String getDomainCode() { return "organization"; }
    @Override public String getDomainName() { return "组织"; }

    @Override
    public List<TriggerPointDef> triggerPoints() {
        return List.of(
            new TriggerPointDef(ORG_UNIT_CREATED, "组织创建",
                Map.of("orgUnitId", "Long", "unitName", "String",
                       "unitType", "String", "parentId", "Long"),
                "新组织单元建立")
        );
    }

    @Override
    public List<EventTypeDef> eventTypes() {
        return List.of(
            new EventTypeDef("ORG_UNIT_CREATED_EVT", "组织新建",
                "PERSONNEL", "人事", "NEUTRAL", "building-plus", "#0ea5e9",
                List.of("ORG_UNIT"), "新组织单元创建")
        );
    }
}
