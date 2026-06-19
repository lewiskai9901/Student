package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.student;

import com.school.management.infrastructure.extension.Contribution;
import com.school.management.infrastructure.extension.ResourceRelationDef;
import com.school.management.infrastructure.extension.plugins.education.EducationManifest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 守护: {@code school_class} 资源的组织锚点 (resource_relations 的 owner_org 关系) 必须是
 * {@code id} (班级自身 org_unit), 不得是 {@code org_unit_id} (= 父年级)。
 *
 * <p>统一锚定 R2.4 起锚点真相在 {@link EducationManifest#contribute()} 的 {@link ResourceRelationDef},
 * 不再是 {@code @DataPermission} 注解。故本守护从"断言注解"迁为"断言 manifest 声明"。
 *
 * <p>背景: {@code classes} 是 {@code org_units} 上的 VIEW: {@code classes.id}=班级自身 org_unit id;
 * {@code classes.org_unit_id}={@code o.parent_id}=父年级。{@code orgField IN (子树)} 过滤须按
 * 班级自身 org (id); 取 {@code org_unit_id}(父年级) 会授某班却看不到该班自身 (真库实证)。
 */
class SchoolClassDataPermissionOrgFieldTest {

    @Test
    void schoolClassOwnerOrg_isClassOwnOrgId_notParentColumn() {
        String col = new EducationManifest().contribute()
            .filter(c -> c instanceof Contribution.ResourceRelationContribution)
            .map(c -> ((Contribution.ResourceRelationContribution) c).def())
            .filter(d -> d.resourceCode().equals("school_class") && d.relationCode().equals("owner_org"))
            .map(ResourceRelationDef::columnName)
            .findFirst().orElse(null);

        assertThat(col)
            .as("classes.id=班级自身 org, classes.org_unit_id=父年级; 数据权限须按班级自身 org 过滤")
            .isEqualTo("id");
    }
}
