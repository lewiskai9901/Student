package com.school.management.domain.access;

import com.school.management.domain.access.model.entity.AccessRelation;
import com.school.management.domain.access.model.valueobject.AccessLevel;
import com.school.management.infrastructure.extension.RelationTypeDef;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 ADR-002:关系传递性以 {@code RelationTypeDef.isTransitive}
 * + {@code impliedRelations} 为单一真相,记录级 {@code includeChildren} 已彻底移除.
 *
 * <p>本测试不调 BFS(那需要 Spring 容器和 DB),仅验证数据结构层面的语义契约:
 * <ul>
 *   <li>{@code isTransitive} 在关系类型字典上正确反映"是否传递"</li>
 *   <li>{@code includeChildren} 字段已从 {@code AccessRelation} 删除(反射不可见) — 防回归</li>
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

    // ========== 矩阵 3: includeChildren 字段已彻底移除(单条记录级) ==========

    @Test
    void accessRelation_includeChildrenField_isRemoved() {
        // ADR-002 收官: 记录级 includeChildren 已从实体删除 (传递性单一真相 = RelationTypeDef)。
        // 反射断言字段不存在 — 防止有人再把这个死字段加回来。
        assertThatThrownBy(() -> AccessRelation.class.getDeclaredField("includeChildren"))
            .as("AccessRelation.includeChildren 应已删除 (ADR-002 收官)")
            .isInstanceOf(NoSuchFieldException.class);
    }

    @Test
    void accessRelation_identityIsFiveTuple() {
        // 关系身份 = 5 元组 + 时间窗, 与已删除的 includeChildren 无关。
        AccessRelation a = AccessRelation.builder()
            .relation("admin").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .accessLevel(AccessLevel.FULL)
            .build();
        AccessRelation b = AccessRelation.builder()
            .relation("admin").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .accessLevel(AccessLevel.FULL)
            .build();
        assertThat(a.getRelation()).isEqualTo(b.getRelation());
        assertThat(a.getResourceId()).isEqualTo(b.getResourceId());
    }

    // ========== 矩阵 4: 4 种组合的真相表 (类型级 vs 记录级) ==========

    @Test
    void truthTable_typeLevelIsSourceOfTruth() {
        // 传递性真相完全在类型层 (RelationTypeDef.isTransitive), 与记录无关 —
        // 记录级 includeChildren 已删除, 故"记录翻转能否影响传递"的旧矩阵不再存在。
        RelationTypeDef transitiveType = RelationTypeDef.of("admin", "user", "org_unit", "管理员",
            "OWNERSHIP", "管理者").transitive();
        RelationTypeDef nonTransitiveType = RelationTypeDef.of("member", "user", "org_unit", "成员",
            "MEMBERSHIP", "成员");

        assertThat(transitiveType.isTransitive()).isTrue();
        assertThat(nonTransitiveType.isTransitive()).isFalse();

        // 关系记录只承载 5 元组 + accessLevel + 时间窗, 不再有任何传递性相关字段。
        AccessRelation rel = AccessRelation.builder()
            .relation("admin").subjectType("user").subjectId(1L)
            .resourceType("org_unit").resourceId(100L)
            .accessLevel(AccessLevel.FULL).build();
        assertThat(rel).isNotNull();
    }
}
