package com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.student;

import com.school.management.infrastructure.access.DataPermission;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 守护: {@link SchoolClassMapper} 的 {@code @DataPermission.orgUnitField} 必须是 {@code id}
 * (班级自身 org_unit), 不得是 {@code org_unit_id}。
 *
 * <p>背景: {@code classes} 是 {@code org_units} 上的 VIEW (type_code='CLASS'):
 * <ul>
 *   <li>{@code classes.id} = 班级自身 org_unit id</li>
 *   <li>{@code classes.org_unit_id} = {@code o.parent_id} = **父节点(年级)**</li>
 * </ul>
 * 数据权限拦截器对 org-field 模块按 {@code orgField IN (子树)} 过滤。若 orgField 取
 * {@code org_unit_id}(父年级), 则:
 * <ul>
 *   <li>CUSTOM=某班X → 过滤 {@code org_unit_id IN 子树(X)={X}} = 父节点为X的班级 = **空**,
 *       授权了某班却看不到该班自身 (真库实证);</li>
 *   <li>DEPARTMENT(精确) 被当成"下一级"(返回直接子班级), 破坏 DEPARTMENT vs
 *       DEPARTMENT_AND_BELOW 语义。</li>
 * </ul>
 * 取 {@code id} 后, 班级自身 org 落在 scope 覆盖的 org 集合内即可见 — 各 scope 全部正确。
 */
class SchoolClassDataPermissionOrgFieldTest {

    @Test
    void orgUnitField_isClassOwnOrgId_notParentColumn() {
        DataPermission ann = SchoolClassMapper.class.getAnnotation(DataPermission.class);
        assertThat(ann).as("SchoolClassMapper 必须有 @DataPermission").isNotNull();
        assertThat(ann.orgUnitField())
            .as("classes.id=班级自身 org, classes.org_unit_id=父年级; 数据权限须按班级自身 org 过滤")
            .isEqualTo("id");
    }
}
