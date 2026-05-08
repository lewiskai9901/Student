package com.school.management.domain.access;

import com.school.management.domain.access.model.DataScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 DataScope enum 各值的语义注释与 ADR-001 保持一致.
 *
 * 此测试不验证 SQL 注入正确性(那是 DataPermissionInterceptorTest 的事),
 * 只确认 DataScope 公约的"宏观语义"和 AccessRelation 的"细粒度语义"
 * 在常见 scope 上是等价表达.
 *
 * 当未来重写 interceptor 时,这些测试可作为"行为不变"的快照.
 *
 * 详见: docs/design/access/ADR-001-datascope-as-access-relation-macro.md
 */
class DataScopeAccessRelationEquivalenceTest {

    @Test
    void allScopesEnumerated() {
        // 防止有人偷偷加新 scope 而没在 ADR 里登记
        // 当前 hardcoded 5 个: ALL / DEPARTMENT_AND_BELOW / DEPARTMENT / CUSTOM / SELF
        // (年级/班级粒度走插件维度 PluginDataScopeRouter, 不在此枚举)
        assertThat(DataScope.values()).hasSize(5);
    }

    @Test
    void self_implies_subjectMatchesActor() {
        // SELF 表示 subject = 当前用户 — 等价 AccessRelation 中 subject_type=user, subject_id=me
        // CalcType.CREATOR 表明 SELF 落到 created_by = me 的 WHERE 子句
        assertThat(DataScope.SELF.getCode()).isEqualTo("SELF");
        assertThat(DataScope.SELF.getCalcType()).isEqualTo(DataScope.CalcType.CREATOR);
    }

    @Test
    void department_implies_orgMembership() {
        // DEPARTMENT 等价 AccessRelation member(user=me, org_unit=my_org)
        // CalcType.USER_ORG 表明只看用户主组织
        assertThat(DataScope.DEPARTMENT.getCode()).isEqualTo("DEPARTMENT");
        assertThat(DataScope.DEPARTMENT.getCalcType()).isEqualTo(DataScope.CalcType.USER_ORG);
    }

    @Test
    void departmentAndBelow_implies_descendantsTraversal() {
        // DEPARTMENT_AND_BELOW 触发 DESCENDANTS_OF_ORG 递归 (tree_path LIKE 'path%')
        // CalcType.USER_ORG_TREE 表明下钻整棵子树
        assertThat(DataScope.DEPARTMENT_AND_BELOW.getCode()).isEqualTo("DEPARTMENT_AND_BELOW");
        assertThat(DataScope.DEPARTMENT_AND_BELOW.getCalcType()).isEqualTo(DataScope.CalcType.USER_ORG_TREE);

        // 且 level 必须 > DEPARTMENT (递归 ⊇ 单组织)
        assertThat(DataScope.DEPARTMENT_AND_BELOW.isGreaterOrEqual(DataScope.DEPARTMENT)).isTrue();
    }

    @Test
    void all_isOpenAccess() {
        // ALL 等价绕过 interceptor — 不映射到任何具体 AccessRelation
        // CalcType.NONE 表明无需任何过滤计算
        assertThat(DataScope.ALL.getCode()).isEqualTo("ALL");
        assertThat(DataScope.ALL.getCalcType()).isEqualTo(DataScope.CalcType.NONE);

        // 必须是最高优先级 (角色合并时 ALL 永远胜出)
        assertThat(DataScope.ALL.isGreaterOrEqual(DataScope.DEPARTMENT_AND_BELOW)).isTrue();
        assertThat(DataScope.ALL.isGreaterOrEqual(DataScope.SELF)).isTrue();
    }

    @Test
    void custom_delegatesToAccessRelations() {
        // CUSTOM 等价 lookup(user=me, "*", org_unit) — 走 access_relations / role_custom_scope 自定义授权
        // CalcType.CUSTOM_CONFIG 表明从配置表读
        assertThat(DataScope.CUSTOM.getCode()).isEqualTo("CUSTOM");
        assertThat(DataScope.CUSTOM.getCalcType()).isEqualTo(DataScope.CalcType.CUSTOM_CONFIG);
    }

    @Test
    void fromCodeStrict_returnsNullForPluginDims() {
        // 插件维度 (e.g. BY_MAJOR) 不应被 fromCodeStrict 解析为核心 enum
        // 这是 interceptor 路由到 PluginDataScopeRouter 的关键判断点
        assertThat(DataScope.fromCodeStrict("BY_MAJOR")).isNull();
        assertThat(DataScope.fromCodeStrict("UNKNOWN")).isNull();
        assertThat(DataScope.fromCodeStrict(null)).isNull();

        // 而核心 enum 必须严格匹配
        assertThat(DataScope.fromCodeStrict("SELF")).isEqualTo(DataScope.SELF);
        assertThat(DataScope.fromCodeStrict("CUSTOM")).isEqualTo(DataScope.CUSTOM);
    }
}
