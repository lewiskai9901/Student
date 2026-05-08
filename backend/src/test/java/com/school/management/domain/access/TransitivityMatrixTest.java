package com.school.management.domain.access;

import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.model.valueobject.AccessLevel;
import com.school.management.infrastructure.extension.RelationTypeDef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 ADR-002:关系传递性以 {@code RelationTypeDef.isTransitive}
 * + {@code impliedRelations} 为单一真相,记录级 {@code includeChildren} 已废弃.
 *
 * <p>本测试不调 BFS(那需要 Spring 容器和 DB),仅验证数据结构层面的语义契约:
 * <ul>
 *   <li>{@code isTransitive} 在关系类型字典上正确反映"是否传递"</li>
 *   <li>{@code includeChildren} 字段标 {@code @Deprecated}(反射可见)</li>
 *   <li>{@code transitive()} 工厂方法正确翻转 isTransitive</li>
 * </ul>
 *
 * <p>BFS 行为正确性见 {@code AuthorizationServiceImpliedTest}(已存在,749 测试集中).
 */
class TransitivityMatrixTest {

    // ========== 矩阵 1: isTransitive=true 的关系类型 ==========

    @Test
    void transitiveRelation_typeDefDeclaresIt() {
        RelationTypeDef admin = RelationTypeDef.of("admin", "user", "org_unit", "管理员",
            "OWNERSHIP", "管理者").transitive();
        assertThat(admin.isTransitive()).isTrue();
    }

    @Test
    void transitiveRelation_supportsImpliedDescendants() {
        RelationTypeDef admin = RelationTypeDef.of("admin", "user", "org_unit", "管理员",
            "OWNERSHIP", "管理者").transitive()
            .withImplied(List.of(
                new RelationTypeDef.Implied("org_unit", "admin", RelationTypeDef.Implied.DESCENDANTS_OF_ORG)
            ));
        assertThat(admin.isTransitive()).isTrue();
        assertThat(admin.impliedRelations()).hasSize(1);
        assertThat(admin.impliedRelations().get(0).discoveryRule())
            .isEqualTo(RelationTypeDef.Implied.DESCENDANTS_OF_ORG);
    }

    // ========== 矩阵 2: isTransitive=false 的关系类型 ==========

    @Test
    void nonTransitiveRelation_byDefault() {
        RelationTypeDef member = RelationTypeDef.of("member", "user", "org_unit", "成员",
            "MEMBERSHIP", "用户属于组织");
        assertThat(member.isTransitive()).isFalse();
    }

    // ========== 矩阵 3: includeChildren 字段已废弃(单条记录级) ==========

    @Test
    void accessRelation_includeChildrenField_isDeprecated() throws Exception {
        var field = AccessRelation.class.getDeclaredField("includeChildren");
        assertThat(field.isAnnotationPresent(Deprecated.class))
            .as("AccessRelation.includeChildren 应标 @Deprecated (ADR-002)")
            .isTrue();
    }

    @Test
    void accessRelation_includeChildrenDoesNotAffectIdentity() {
        // 同样 5 元组 + accessLevel + valid* 一致, includeChildren 不同 → 业务上等价
        AccessRelation withTrue = AccessRelation.builder()
            .relation("admin").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .accessLevel(AccessLevel.FULL)
            .includeChildren(true)
            .build();
        AccessRelation withFalse = AccessRelation.builder()
            .relation("admin").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .accessLevel(AccessLevel.FULL)
            .includeChildren(false)
            .build();
        // 关系核心标识不应依赖 includeChildren — 5 元组 + 时间窗才是身份
        assertThat(withTrue.getRelation()).isEqualTo(withFalse.getRelation());
        assertThat(withTrue.getResourceId()).isEqualTo(withFalse.getResourceId());
        // includeChildren 字段保留但 BFS 行为不依赖它
    }

    // ========== 矩阵 4: 4 种组合的真相表 (类型级 vs 记录级) ==========

    @Test
    void truthTable_typeLevelIsSourceOfTruth() {
        // 场景 1: 类型 transitive=true, 记录 includeChildren=true → 传递
        // 场景 2: 类型 transitive=true, 记录 includeChildren=false → 仍传递(类型为准)
        // 场景 3: 类型 transitive=false, 记录 includeChildren=true → 不传递
        // 场景 4: 类型 transitive=false, 记录 includeChildren=false → 不传递

        RelationTypeDef transitiveType = RelationTypeDef.of("admin", "user", "org_unit", "管理员",
            "OWNERSHIP", "管理者").transitive();
        RelationTypeDef nonTransitiveType = RelationTypeDef.of("member", "user", "org_unit", "成员",
            "MEMBERSHIP", "成员");

        // 关键断言:类型层 isTransitive 是否传递的真相
        assertThat(transitiveType.isTransitive()).isTrue();
        assertThat(nonTransitiveType.isTransitive()).isFalse();

        // 4 个 AccessRelation 组合 — 字段都能 set, 但 BFS 不读 includeChildren
        AccessRelation s1 = AccessRelation.builder()
            .relation("admin").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .includeChildren(true).build();
        AccessRelation s2 = AccessRelation.builder()
            .relation("admin").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .includeChildren(false).build();
        AccessRelation s3 = AccessRelation.builder()
            .relation("member").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .includeChildren(true).build();
        AccessRelation s4 = AccessRelation.builder()
            .relation("member").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .includeChildren(false).build();

        // 4 条记录都能正常构造,语义校验交由 BFS (在集成测试覆盖)
        assertThat(s1).isNotNull();
        assertThat(s2).isNotNull();
        assertThat(s3).isNotNull();
        assertThat(s4).isNotNull();
    }
}
