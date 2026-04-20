package com.school.management.infrastructure.extension;

import com.school.management.infrastructure.extension.RelationTypePlugin.RelationTypeDef;
import com.school.management.infrastructure.extension.RelationTypePlugin.RelationTypeDef.Implied;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * W4.1 — RelationTypeDef + impliedRelations 字段行为
 */
class RelationTypeDefTest {

    @Test
    void ofFactory_defaultsImpliedToEmptyList() {
        RelationTypeDef d = RelationTypeDef.of("viewer", "user", "place", "查看", "ASSOCIATION", "看看");
        assertThat(d.impliedRelations()).isNotNull().isEmpty();
        assertThat(d.isTransitive()).isFalse();
        assertThat(d.capacityBound()).isFalse();
    }

    @Test
    void withImplied_replacesList_andKeepsOtherFields() {
        RelationTypeDef base = RelationTypeDef.of("dorm_head", "user", "place",
                "宿舍头领", "OWNERSHIP", "宿舍长");
        List<Implied> impl = List.of(
            new Implied("user", "viewer", Implied.OCCUPANTS_OF_PLACE));

        RelationTypeDef with = base.withImplied(impl);

        assertThat(with.impliedRelations()).containsExactlyElementsOf(impl);
        assertThat(with.relationCode()).isEqualTo("dorm_head");
        assertThat(with.fromType()).isEqualTo("user");
        assertThat(with.toType()).isEqualTo("place");
    }

    @Test
    void withImplied_nullArg_becomesEmptyList_notNull() {
        RelationTypeDef d = RelationTypeDef.of("x", "user", "place", "X", "ASSOCIATION", "desc")
                .withImplied(null);
        assertThat(d.impliedRelations()).isNotNull().isEmpty();
    }

    @Test
    void builderChain_transitiveThenImplied_preservesBoth() {
        RelationTypeDef d = RelationTypeDef.of("admin", "user", "org_unit", "管理员", "OWNERSHIP", "主管")
            .transitive()
            .withImplied(List.of(new Implied("user", "viewer", Implied.MEMBERS_OF_ORG)));

        assertThat(d.isTransitive()).isTrue();
        assertThat(d.impliedRelations()).hasSize(1);
        assertThat(d.impliedRelations().get(0).discoveryRule()).isEqualTo(Implied.MEMBERS_OF_ORG);
    }

    @Test
    void impliedConstants_areDistinct() {
        assertThat(Implied.OCCUPANTS_OF_PLACE).isEqualTo("OCCUPANTS_OF_PLACE");
        assertThat(Implied.MEMBERS_OF_ORG).isEqualTo("MEMBERS_OF_ORG");
        assertThat(Implied.DESCENDANTS_OF_ORG).isEqualTo("DESCENDANTS_OF_ORG");
    }
}
