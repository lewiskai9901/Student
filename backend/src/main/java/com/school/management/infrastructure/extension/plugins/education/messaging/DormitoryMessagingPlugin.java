package com.school.management.infrastructure.extension.plugins.education.messaging;

import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.school.management.infrastructure.extension.plugins.core.constants.CorePlaceTriggerPoints.PLACE_OCCUPIED;
import static com.school.management.infrastructure.extension.plugins.core.constants.CorePlaceTriggerPoints.PLACE_VACATED;

/**
 * 宿舍业务消息插件 — 订阅核心 PLACE_OCCUPIED / PLACE_VACATED 触发点,
 * 转译为宿舍特定事件 DORM_CHECKIN_EVT / DORM_CHECKOUT_EVT.
 *
 * 架构边界: 通用核心场所 Place 只 fire 通用 PLACE_OCCUPIED;
 *          教育行业的"宿舍入住"语义由此插件认领,
 *          让 core Place 服务对 EDU 插件零依赖.
 */
@Component
public class DormitoryMessagingPlugin implements MessagingDomainPlugin {

    @Override
    public String getDomainCode() { return "dormitory"; }

    @Override
    public String getDomainName() { return "宿舍"; }

    // 不声明 triggerPoints — 复用核心 PLACE_OCCUPIED / PLACE_VACATED

    @Override
    public List<EventTypeDef> eventTypes() {
        return List.of(
            new EventTypeDef("DORM_CHECKIN_EVT", "入住登记",
                "PLACE", "场所", "NEUTRAL",
                "bed-double", "#0d9488",
                List.of("USER"),
                "学生新入住宿舍"),

            new EventTypeDef("DORM_CHECKOUT_EVT", "退宿登记",
                "PLACE", "场所", "NEUTRAL",
                "log-out", "#6b7280",
                List.of("USER"),
                "学生退出宿舍")
        );
    }

    /** 订阅核心触发点,映射到宿舍事件 */
    @Override
    public List<DefaultTriggerDef> defaultTriggers() {
        return List.of(
            new DefaultTriggerDef(PLACE_OCCUPIED, "DORM_CHECKIN_EVT",
                List.of(Map.of("type", "USER", "id", "{{occupantId}}"))),
            new DefaultTriggerDef(PLACE_VACATED, "DORM_CHECKOUT_EVT",
                List.of(Map.of("type", "USER", "id", "{{occupantId}}")))
        );
    }
}
