package com.school.management.infrastructure.extension.plugins.healthcare;

import com.school.management.infrastructure.extension.RelationTypePlugin;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.school.management.infrastructure.extension.RelationTypePlugin.RelationTypeDef.of;

/**
 * HEALTH 关系插件 reference impl.
 *
 * 声明:
 *  - family_of: 家属监护病人 (subject=家属, resource=病人).
 *    入院/出院事件触发时, 用 BY_RELATION(family_of, inward) 查病人所有家属联系人.
 *  - attending_of: 主治医师 (subject=医师, resource=病人).
 *  - nurse_of: 责任护士 (subject=护士, resource=病人).
 *  - in_ward: 所在病区 (subject=病人/护士, resource=ORG_UNIT 病区). transitive — 沿组织树传递.
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
               "入院/出院消息扇出时用 BY_RELATION(family_of, inward) 查病人的家属"),

            of(HealthcareRelations.ATTENDING_OF, "user", "user", "主治医师",
               "DELEGATION",
               "医师主治某病人 (subject=医师, resource=病人). " +
               "查病区/科室所有在治病人走 BY_RELATION(attending_of, outward)"),

            of(HealthcareRelations.NURSE_OF, "user", "user", "责任护士",
               "DELEGATION",
               "护士主责某病人 (subject=护士, resource=病人). " +
               "排班/交接班时按 BY_RELATION(nurse_of) 查负责清单"),

            of(HealthcareRelations.IN_WARD, "user", "org_unit", "所在病区",
               "MEMBERSHIP",
               "病人或医护所在病区 (subject=user, resource=ORG_UNIT 病区). " +
               "transitive=true, 若病区下有子区可继承")
               .transitive()
        );
    }

    /** 医疗场景关系码常量, 业务代码引用 */
    public static final class HealthcareRelations {
        public static final String FAMILY_OF = "family_of";
        public static final String ATTENDING_OF = "attending_of";
        public static final String NURSE_OF = "nurse_of";
        public static final String IN_WARD = "in_ward";
        private HealthcareRelations() {}
    }
}
