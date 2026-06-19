package com.school.management.infrastructure.persistence.place;

import com.school.management.infrastructure.extension.Contribution;
import com.school.management.infrastructure.extension.ResourceRelationDef;
import com.school.management.infrastructure.extension.plugins.core.CoreManifest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 守护: {@code place} 资源的组织锚点 (resource_relations 的 owner_org 关系) 必须是投影列
 * {@code effective_org_unit_id}, 不得回退旧列名 {@code org_unit_id}。
 *
 * <p>统一锚定 R2.4 起锚点真相在 {@link CoreManifest#contribute()} 的 {@link ResourceRelationDef}
 * (写入 {@code resource_relations}), 不再是 {@code @DataPermission} 注解 (注解锚属性已删)。
 * 故本守护从"断言注解"迁为"断言 manifest 声明"。
 *
 * <p>背景 (V20260612_2 场所归属关系化): 归属真相在 access_relations 的
 * {@code belongs_to|place|org_unit} 覆盖点关系, {@code places.effective_org_unit_id} 是解析后
 * 含继承的投影列。数据权限过滤必须走投影列 —— 旧 {@code org_unit_id} 列 NULL=继承,
 * {@code org_unit_id IN (子树)} 永远排除继承态场所 (旧模型缺陷)。
 */
class PlaceDataPermissionOrgFieldTest {

    @Test
    void placeOwnerOrg_usesEffectiveProjectionColumn() {
        String col = new CoreManifest().contribute()
            .filter(c -> c instanceof Contribution.ResourceRelationContribution)
            .map(c -> ((Contribution.ResourceRelationContribution) c).def())
            .filter(d -> d.resourceCode().equals("place") && d.relationCode().equals("owner_org"))
            .map(ResourceRelationDef::columnName)
            .findFirst().orElse(null);

        assertThat(col)
            .as("place 的 owner_org 锚点必须是投影列 effective_org_unit_id (归属真相在 belongs_to 关系)")
            .isEqualTo("effective_org_unit_id");
    }
}
