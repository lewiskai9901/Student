package com.school.management.infrastructure.extension.plugins.education.messaging;

import com.school.management.infrastructure.extension.Contribution;
import com.school.management.infrastructure.extension.MessagingDomainPlugin;
import com.school.management.infrastructure.extension.PluginPackage;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

/**
 * Track M3 reference: 宿舍业务消息插件 — 已从 {@code MessagingDomainPlugin} (@Deprecated)
 * 迁移至 {@link PluginPackage#contribute()} + {@link Contribution.EventTypeContribution}.
 *
 * <h3>架构边界</h3>
 * 通用核心场所 Place 只 fire 通用触发点 PLACE_OCCUPIED / PLACE_VACATED.
 * 教育行业的 "宿舍入住" 语义由本插件认领为事件类型 (DORM_CHECKIN_EVT / DORM_CHECKOUT_EVT),
 * 让 core Place 服务对 EDU 插件零依赖.
 *
 * <h3>本插件声明</h3>
 * <ul>
 *   <li>0 触发点: 复用 CORE 的 PLACE_OCCUPIED / PLACE_VACATED, 无需重复声明</li>
 *   <li>2 事件类型: DORM_CHECKIN_EVT / DORM_CHECKOUT_EVT</li>
 * </ul>
 *
 * <h3>默认触发器不走 SPI</h3>
 * 原 {@code defaultTriggers()} 把 PLACE_OCCUPIED → DORM_CHECKIN_EVT 的默认绑定声明在
 * 代码里. Track M3 决定把默认触发器语义交给 admin UI / DB seed 管理 —
 * 不同租户可能想绑不同事件类型, 编码强绑不够灵活.
 */
@Component
public class DormitoryMessagingPlugin implements PluginPackage {

    private static final String DOMAIN_CODE = "dormitory";
    private static final String DOMAIN_NAME = "宿舍";

    // ═════════ PluginManifest 元数据 (复用 EDU industry) ═════════

    @Override public String getIndustryCode() { return "EDU"; }
    @Override public String getIndustryName() { return "教育行业"; }
    @Override public List<String> getDependsOn() { return List.of("CORE"); }

    @Override
    public boolean owns(Class<?> pluginClass) {
        return pluginClass.getPackageName().contains(".plugins.education");
    }

    // ═════════ Track M3: 细粒度 Contribution (仅 event type) ═════════

    @Override
    public Stream<Contribution> contribute() {
        return Stream.of(
            new Contribution.EventTypeContribution(DOMAIN_CODE, DOMAIN_NAME,
                new MessagingDomainPlugin.EventTypeDef(
                    "DORM_CHECKIN_EVT", "入住登记",
                    "PLACE", "场所", "NEUTRAL",
                    "bed-double", "#0d9488",
                    List.of("USER"),
                    "学生新入住宿舍")),
            new Contribution.EventTypeContribution(DOMAIN_CODE, DOMAIN_NAME,
                new MessagingDomainPlugin.EventTypeDef(
                    "DORM_CHECKOUT_EVT", "退宿登记",
                    "PLACE", "场所", "NEUTRAL",
                    "log-out", "#6b7280",
                    List.of("USER"),
                    "学生退出宿舍"))
        );
    }
}
