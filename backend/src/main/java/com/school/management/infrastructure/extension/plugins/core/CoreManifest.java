package com.school.management.infrastructure.extension.plugins.core;

import com.school.management.infrastructure.extension.Contribution;
import com.school.management.infrastructure.extension.PluginPackage;
import com.school.management.infrastructure.extension.RelationTypeDef;
import com.school.management.infrastructure.extension.RelationTypeDef.Implied;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

/**
 * 通用核心插件包 — 任何部署都需要,不可卸载.
 *
 * 包含:
 *  - 9 个核心关系类型 (Phase 2 W2.2: 已迁入本 Manifest 的 contribute() 流)
 *  - 通用权限 / 超级管理员角色
 *  - 通用事件类型(USER_CREATED / ORG_MERGED 等)
 *
 * Phase 2 W2.2: CoreRelationsPlugin 已删, 关系定义直接在此声明.
 * 业务代码引用关系码用 {@link CoreRelations} 常量.
 */
@Component
public class CoreManifest implements PluginPackage {

    private static final String SOURCE = "CORE";
    private static final String TIER = "CORE";

    @Override
    public String getIndustryCode() { return "CORE"; }

    @Override
    public String getIndustryName() { return "通用核心"; }

    @Override
    public boolean owns(Class<?> pluginClass) {
        String pkg = pluginClass.getPackageName();
        // core 包 + 未标注 industry 的旧插件(保守落到 CORE)
        return pkg.contains(".plugins.core") || !pkg.contains(".plugins.");
    }

    @Override
    public Stream<Contribution> contribute() {
        return Stream.of(
            // 成员关系
            wrap(RelationTypeDef.of(CoreRelations.MEMBER, "user", "org_unit", "成员",
                "MEMBERSHIP", "用户属于某组织")),

            // 管理关系 (organization) — 主管理员单一,副管理员可多人
            // W4 reference demo: admin(org) 派生两条 implied:
            //   1) viewer on user via MEMBERS_OF_ORG — 能管组织就能看组织里的人
            //   2) admin on org_unit via DESCENDANTS_OF_ORG — 管理权沿组织树下沉 (isTransitive 的显式展开)
            wrap(RelationTypeDef.of(CoreRelations.ADMIN, "user", "org_unit", "主管理员",
                "OWNERSHIP", "组织的主负责人(如班主任/部门主管),沿子组织传递")
                .transitive().withMaxPerResource(1)
                .withImplied(List.of(
                    new Implied("user",     "viewer", Implied.MEMBERS_OF_ORG),
                    new Implied("org_unit", "admin",  Implied.DESCENDANTS_OF_ORG)
                ))),

            wrap(RelationTypeDef.of(CoreRelations.DEPUTY, "user", "org_unit", "副管理员",
                "OWNERSHIP", "组织副负责人").transitive()),

            // 管理关系 (place) — 负责人单一,管理者可多人
            wrap(RelationTypeDef.of(CoreRelations.ADMIN, "user", "place", "场所负责人",
                "OWNERSHIP", "场所的主负责人").withMaxPerResource(1)),

            // W4.4 reference demo: 场所管理者 → 自动对场所内所有 occupant(user) 有 viewer 权限
            // 语义: "能管场所" 意味着 "能看到场所里的人",派生规则走 OCCUPANTS_OF_PLACE discovery
            wrap(RelationTypeDef.of(CoreRelations.MANAGES, "user", "place", "场所管理者",
                "OWNERSHIP", "非主责管理者 (保洁/物业等)")
                .withImplied(List.of(
                    new Implied("user", "viewer", Implied.OCCUPANTS_OF_PLACE)
                ))),

            // 归属 — 一个场所只能归属一个主组织
            wrap(RelationTypeDef.of(CoreRelations.BELONGS_TO, "place", "org_unit", "归属",
                "ASSOCIATION", "场所归属某组织").withMaxPerResource(1)),

            // 占用 — 受场所容量约束
            wrap(RelationTypeDef.of(CoreRelations.OCCUPIES, "user", "place", "占用",
                "MEMBERSHIP", "宿舍入住/工位使用 (含 check_in/check_out 时间)").withCapacityBound()),

            // 委托
            wrap(RelationTypeDef.of(CoreRelations.DELEGATED_TO, "user", "user", "委托",
                "DELEGATION", "权限临时委托给另一用户")),

            // 订阅
            wrap(RelationTypeDef.of(CoreRelations.WATCHES, "user", "org_unit", "关注",
                "SUBSCRIPTION", "用户订阅某组织的动态"))
        );
    }

    private static Contribution.RelationTypeContribution wrap(RelationTypeDef def) {
        return new Contribution.RelationTypeContribution(SOURCE, TIER, def);
    }
}
