package com.school.management.infrastructure.extension.plugins.healthcare;

import com.school.management.infrastructure.extension.RelationTypePlugin;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.school.management.infrastructure.extension.RelationTypePlugin.RelationTypeDef.of;

/**
 * Phase 8.6: HEALTH 关系插件 reference impl.
 *
 * 声明:
 *  - family_of: 家属监护病人 (subject=家属, resource=病人).
 *    入院/出院事件触发时, 用 BY_RELATION(family_of, inward) 查病人所有家属联系人.
 *
 * 业务代码引用用 {@link HealthcareRelations} 常量, 避免裸字符串.
 */
@Component
@SuppressWarnings("deprecation")
public class HealthcareRelationsPlugin implements RelationTypePlugin {

    @Override public String getSourceName() { return "HealthcarePlugin"; }

    @Override public String getTier() { return "DOMAIN"; }

    @Override
    public List<RelationTypeDef> getRelationTypes() {
        return List.of(
            of(HealthcareRelations.FAMILY_OF, "user", "user", "家属监护",
               "ASSOCIATION",
               "家属监护病人 (subject=家属, resource=病人). " +
               "入院/出院消息扇出时用 BY_RELATION(family_of, inward) 查病人的家属")
        );
    }

    /** 医疗场景关系码常量, 业务代码引用 */
    public static final class HealthcareRelations {
        public static final String FAMILY_OF = "family_of";
        private HealthcareRelations() {}
    }
}
