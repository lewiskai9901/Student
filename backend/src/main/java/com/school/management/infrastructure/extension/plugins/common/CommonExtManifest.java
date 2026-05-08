package com.school.management.infrastructure.extension.plugins.common;

import com.school.management.infrastructure.extension.Contribution;
import com.school.management.infrastructure.extension.PluginPackage;
import com.school.management.infrastructure.extension.RelationTypeDef;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 跨行业通用扩展插件 (tier=COMMON_EXT).
 *
 * <p>居于 CORE 与 DOMAIN 之间, 装载多个行业都用但又不属于平台核心治理的关系/类型.
 * 当前承载: family_of (家属) + emergency_contact (紧急联系人).
 *
 * <p>教育插件的 guardian_of 和医疗插件的 family_of 已合并到这里 (Phase 3 W3.2).
 */
@Component
public class CommonExtManifest implements PluginPackage {

    private static final String SOURCE = "CommonExtPlugin";
    private static final String TIER = "COMMON_EXT";

    @Override public String getIndustryCode() { return "COMMON_EXT"; }
    @Override public String getIndustryName() { return "跨行业通用扩展"; }
    @Override public List<String> getDependsOn() { return List.of("CORE"); }
    @Override public Map<String, String> getDependsOnWithVersion() {
        return Map.of("CORE", ">=1.0.0 <2.0.0");
    }
    @Override public boolean owns(Class<?> pluginClass) {
        return pluginClass.getPackageName().contains(".plugins.common");
    }

    @Override
    public Stream<Contribution> contribute() {
        return Stream.of(
            wrap(RelationTypeDef.of(CommonExtRelations.FAMILY_OF, "user", "user", "亲属",
                "ASSOCIATION", "家属/家长 — 跨学校(家长↔学生)/医院(家属↔病人)/养老 通用. " +
                "消息扇出走 BY_RELATION(family_of, inward) 查 resource user 的家属")),

            wrap(RelationTypeDef.of(CommonExtRelations.EMERGENCY_CONTACT, "user", "user", "紧急联系人",
                "ASSOCIATION", "通用紧急联系人 — 入院/事故/失联场景"))
        );
    }

    private Contribution.RelationTypeContribution wrap(RelationTypeDef def) {
        return new Contribution.RelationTypeContribution(SOURCE, TIER, def);
    }
}
