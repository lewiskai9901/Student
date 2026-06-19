package com.school.management.domain.access.model.valueobject;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.access.model.OrgAnchor;
import com.school.management.domain.access.model.SubjectScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RelationGrant.fromM1Axes 迁移映射单测 (统一锚定 R3a) —— M1 三轴 → relation_grants 字节等价。
 * 覆盖现有 74 行实际用到的 4 锚点 (SELF/PRIMARY_ORG/ALL/PLUGIN_DIM) + SELF 成员 nuance,
 * 外加 RELATION/CUSTOM (现零数据, 但映射须完整以防将来配置)。
 */
class RelationGrantTest {

    private static RelationGrant only(List<RelationGrant> grants) {
        assertEquals(1, grants.size(), "应产恰好一条 grant");
        return grants.get(0);
    }

    @Test
    @DisplayName("ALL → {owner_org, ALL}")
    void all() {
        RelationGrant g = only(RelationGrant.fromM1Axes(OrgAnchor.ALL, null, false, null, false));
        assertEquals("owner_org", g.relation());
        assertEquals(SubjectScope.ALL, g.subject());
    }

    @Test
    @DisplayName("SELF 列锚资源 → {creator, SELF} (creator=我)")
    void selfColumn() {
        RelationGrant g = only(RelationGrant.fromM1Axes(OrgAnchor.SELF, null, false, null, false));
        assertEquals("creator", g.relation());
        assertEquals(SubjectScope.SELF, g.subject());
    }

    @Test
    @DisplayName("SELF 成员资源 → {owner_org, SELF} (member-self, 非 creator) — 迁移最易错点")
    void selfMembership() {
        RelationGrant g = only(RelationGrant.fromM1Axes(OrgAnchor.SELF, null, false, null, true));
        assertEquals("owner_org", g.relation());
        assertEquals(SubjectScope.SELF, g.subject());
    }

    @Test
    @DisplayName("PRIMARY_ORG + subtree → {owner_org, MY_ORG, subtree=true}")
    void primaryOrgSubtree() {
        RelationGrant g = only(RelationGrant.fromM1Axes(OrgAnchor.PRIMARY_ORG, null, true, null, false));
        assertEquals("owner_org", g.relation());
        assertEquals(SubjectScope.MY_ORG, g.subject());
        assertTrue(g.subtree());
    }

    @Test
    @DisplayName("PRIMARY_ORG 无 subtree → {owner_org, MY_ORG, subtree=false}")
    void primaryOrgNoSubtree() {
        RelationGrant g = only(RelationGrant.fromM1Axes(OrgAnchor.PRIMARY_ORG, null, false, null, false));
        assertEquals(SubjectScope.MY_ORG, g.subject());
        assertFalse(g.subtree());
    }

    @Test
    @DisplayName("PLUGIN_DIM:BY_CLASS → {owner_org, PLUGIN_DIM, param=BY_CLASS}")
    void pluginDim() {
        RelationGrant g = only(RelationGrant.fromM1Axes(OrgAnchor.PLUGIN_DIM, "BY_CLASS", false, null, false));
        assertEquals("owner_org", g.relation());
        assertEquals(SubjectScope.PLUGIN_DIM, g.subject());
        assertEquals("BY_CLASS", g.subjectParam());
    }

    @Test
    @DisplayName("RELATION:admin + subtree → {owner_org, RELATION, param=admin, subtree} (现零数据, 映射须完整)")
    void relation() {
        RelationGrant g = only(RelationGrant.fromM1Axes(OrgAnchor.RELATION, "admin", true, null, false));
        assertEquals("owner_org", g.relation());
        assertEquals(SubjectScope.RELATION, g.subject());
        assertEquals("admin", g.subjectParam());
        assertTrue(g.subtree());
    }

    @Test
    @DisplayName("CUSTOM_ORG + subtree → {owner_org, CUSTOM, orgIds, subtree} (subtree 须透传, 现零数据映射须完整)")
    void customOrg() {
        Set<Long> ids = Set.of(77L, 88L);
        RelationGrant g = only(RelationGrant.fromM1Axes(OrgAnchor.CUSTOM_ORG, null, true, ids, false));
        assertEquals("owner_org", g.relation());
        assertEquals(SubjectScope.CUSTOM, g.subject());
        assertEquals(ids, g.orgIds());
        assertTrue(g.subtree(), "CUSTOM_ORG 须透传 subtree (否则 CUSTOM+子树退化成裸 IN)");
    }

    // ── Jackson JSON round-trip: relation_grants 列存取依赖 (R3a-2 getScopeSpec) ──

    @Test
    @DisplayName("Jackson round-trip: List<RelationGrant> 序列化↔反序列化 (含 CUSTOM orgIds)")
    void jacksonRoundTrip() throws Exception {
        ObjectMapper om = new ObjectMapper();
        List<RelationGrant> grants = List.of(
                new RelationGrant("owner_org", SubjectScope.MY_ORG, null, true, null),
                new RelationGrant("owner_org", SubjectScope.CUSTOM, null, false, Set.of(7L, 8L)));
        String json = om.writeValueAsString(grants);
        List<RelationGrant> back = om.readValue(json, new TypeReference<List<RelationGrant>>() {});
        assertEquals(grants, back, "relation_grants JSON 须无损往返 (record + -parameters)");
    }

    @Test
    @DisplayName("Jackson 反序列化迁移最小 JSON (subject 枚举 + 缺省字段补 null/false)")
    void jacksonDeserializeMinimalMigrationJson() throws Exception {
        ObjectMapper om = new ObjectMapper();
        String json = "[{\"relation\":\"owner_org\",\"subject\":\"PLUGIN_DIM\",\"subjectParam\":\"BY_CLASS\"}]";
        List<RelationGrant> grants = om.readValue(json, new TypeReference<List<RelationGrant>>() {});
        assertEquals(1, grants.size());
        RelationGrant g = grants.get(0);
        assertEquals("owner_org", g.relation());
        assertEquals(SubjectScope.PLUGIN_DIM, g.subject());
        assertEquals("BY_CLASS", g.subjectParam());
        assertFalse(g.subtree());
        assertNull(g.orgIds());
    }
}
