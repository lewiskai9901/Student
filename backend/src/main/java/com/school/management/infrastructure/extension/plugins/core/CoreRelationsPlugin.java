package com.school.management.infrastructure.extension.plugins.core;

import com.school.management.infrastructure.extension.RelationTypePlugin;
import com.school.management.infrastructure.extension.RelationTypePlugin.RelationTypeDef.Implied;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.school.management.infrastructure.extension.RelationTypePlugin.RelationTypeDef.of;

/**
 * 平台核心关系 (任何行业都会用到,不可删除).
 *
 * 业务代码应通过 {@code RelationTypeRegistry.Relations.MEMBER} 等常量引用这些关系码.
 */
@Component
public class CoreRelationsPlugin implements RelationTypePlugin {

    @Override
    public String getSourceName() { return "CORE"; }

    @Override
    public String getTier() { return "CORE"; }

    @Override
    public List<RelationTypeDef> getRelationTypes() {
        return List.of(
            // 成员关系
            of("member", "user", "org_unit", "成员",
               "MEMBERSHIP", "用户属于某组织"),

            // 管理关系 (organization) — 主管理员单一,副管理员可多人
            // W4 reference demo: admin(org) 派生两条 implied:
            //   1) viewer on user via MEMBERS_OF_ORG — 能管组织就能看组织里的人
            //   2) admin on org_unit via DESCENDANTS_OF_ORG — 管理权沿组织树下沉 (isTransitive 的显式展开)
            of("admin", "user", "org_unit", "主管理员",
               "OWNERSHIP", "组织的主负责人(如班主任/部门主管),沿子组织传递").transitive().withMaxPerResource(1)
               .withImplied(List.of(
                   new Implied("user",     "viewer", Implied.MEMBERS_OF_ORG),
                   new Implied("org_unit", "admin",  Implied.DESCENDANTS_OF_ORG)
               )),
            of("deputy", "user", "org_unit", "副管理员",
               "OWNERSHIP", "组织副负责人").transitive(),

            // 管理关系 (place) — 负责人单一,管理者可多人
            of("admin", "user", "place", "场所负责人",
               "OWNERSHIP", "场所的主负责人").withMaxPerResource(1),
            // W4.4 reference demo: 场所管理者 → 自动对场所内所有 occupant(user) 有 viewer 权限
            // 语义: "能管场所" 意味着 "能看到场所里的人",派生规则走 OCCUPANTS_OF_PLACE discovery
            of("manages", "user", "place", "场所管理者",
               "OWNERSHIP", "非主责管理者 (保洁/物业等)")
               .withImplied(List.of(
                   new Implied("user", "viewer", Implied.OCCUPANTS_OF_PLACE)
               )),

            // 归属 — 一个场所只能归属一个主组织
            of("belongs_to", "place", "org_unit", "归属",
               "ASSOCIATION", "场所归属某组织").withMaxPerResource(1),

            // 占用 — 受场所容量约束
            of("occupies", "user", "place", "占用",
               "MEMBERSHIP", "宿舍入住/工位使用 (含 check_in/check_out 时间)").withCapacityBound(),

            // 委托
            of("delegated_to", "user", "user", "委托",
               "DELEGATION", "权限临时委托给另一用户"),

            // 订阅
            of("watches", "user", "org_unit", "关注",
               "SUBSCRIPTION", "用户订阅某组织的动态")
        );
    }
}
