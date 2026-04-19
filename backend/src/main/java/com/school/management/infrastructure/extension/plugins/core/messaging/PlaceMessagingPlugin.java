package com.school.management.infrastructure.extension.plugins.core.messaging;

import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.school.management.infrastructure.extension.plugins.core.constants.CorePlaceTriggerPoints.PLACE_OCCUPIED;
import static com.school.management.infrastructure.extension.plugins.core.constants.CorePlaceTriggerPoints.PLACE_VACATED;

/**
 * 通用场所消息插件 — 声明 PLACE_OCCUPIED / PLACE_VACATED 核心触发点.
 *
 * 行业插件订阅这些核心点, 按需转译为行业特定事件 (如 EDU 的 DORM_CHECKIN_EVT).
 */
@Component
public class PlaceMessagingPlugin implements MessagingDomainPlugin {

    @Override public String getDomainCode() { return "place"; }
    @Override public String getDomainName() { return "场所"; }

    @Override
    public List<TriggerPointDef> triggerPoints() {
        return List.of(
            new TriggerPointDef(PLACE_OCCUPIED, "场所占用",
                Map.of(
                    "occupantId", "Long",
                    "occupantName", "String",
                    "placeId", "Long",
                    "placeName", "String",
                    "placeTypeCode", "String"
                ),
                "用户入住/占用场所(宿舍/工位/酒店通用)"),

            new TriggerPointDef(PLACE_VACATED, "场所退出",
                Map.of(
                    "occupantId", "Long",
                    "occupantName", "String",
                    "placeId", "Long",
                    "placeName", "String",
                    "placeTypeCode", "String"
                ),
                "用户退出场所占用")
        );
    }
}
