package com.school.management.domain.inspection.model.template.snapshot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.execution.TargetType;
import com.school.management.domain.inspection.model.template.ItemType;
import com.school.management.domain.inspection.model.template.TemplateVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SnapshotTree 单元测试 (J1 真重放).
 *
 * <p>验证:
 * <ul>
 *   <li>从典型 InspTemplateApplicationService.publishRootSection 写入的 JSON 反序列化</li>
 *   <li>parent → children 索引</li>
 *   <li>section → items 索引</li>
 *   <li>损坏 JSON / NULL snapshot → empty Optional, 不抛</li>
 *   <li>SnapshotSection / SnapshotItem 字段映射 ↔ live entity 转换</li>
 * </ul>
 */
@DisplayName("SnapshotTree — 模板结构快照反序列化")
class SnapshotTreeTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("典型 JSON → 解析出 root + descendants + items + index")
    void typicalJson_parsesAndIndexes() {
        String json =
                "{"
                + "\"rootSection\":{\"id\":100,\"templateId\":100,\"sectionCode\":\"ROOT\",\"sectionName\":\"Root\",\"targetType\":\"ORG\"},"
                + "\"sections\":["
                + "{\"id\":101,\"templateId\":100,\"parentSectionId\":100,\"sectionCode\":\"A\",\"sectionName\":\"SA\",\"targetType\":\"ORG\"},"
                + "{\"id\":102,\"templateId\":100,\"parentSectionId\":100,\"sectionCode\":\"B\",\"sectionName\":\"SB\",\"targetType\":\"ORG\"}"
                + "],"
                + "\"items\":["
                + "{\"id\":201,\"sectionId\":101,\"itemCode\":\"I-1\",\"itemName\":\"Item1\",\"itemType\":\"RATING\"},"
                + "{\"id\":202,\"sectionId\":101,\"itemCode\":\"I-2\",\"itemName\":\"Item2\",\"itemType\":\"PASS_FAIL\"},"
                + "{\"id\":203,\"sectionId\":102,\"itemCode\":\"I-3\",\"itemName\":\"Item3\",\"itemType\":\"NUMBER\"}"
                + "],"
                + "\"snapshotAt\":\"2026-05-17T10:00:00\""
                + "}";
        TemplateVersion v = TemplateVersion.reconstruct(
                999L, 100L, 1, json, null, 1L, LocalDateTime.now());

        Optional<SnapshotTree> treeOpt = SnapshotTree.from(v, mapper);
        assertThat(treeOpt).isPresent();
        SnapshotTree tree = treeOpt.get();

        // 3 sections: root + 2 descendants
        assertThat(tree.getSections()).hasSize(3);
        assertThat(tree.getItems()).hasSize(3);

        // parent → children 索引
        List<SnapshotSection> children = tree.findByParentSectionId(100L);
        assertThat(children).hasSize(2);
        assertThat(children).extracting(SnapshotSection::getSectionCode).containsExactlyInAnyOrder("A", "B");

        // section by id
        assertThat(tree.findSectionById(101L)).isPresent()
                .get().extracting(SnapshotSection::getSectionName).isEqualTo("SA");
        assertThat(tree.findSectionById(999L)).isEmpty();

        // section → items
        List<SnapshotItem> sectionAItems = tree.findItemsBySectionId(101L);
        assertThat(sectionAItems).hasSize(2);
        assertThat(sectionAItems).extracting(SnapshotItem::getItemCode).containsExactlyInAnyOrder("I-1", "I-2");

        List<SnapshotItem> sectionBItems = tree.findItemsBySectionId(102L);
        assertThat(sectionBItems).hasSize(1);
        assertThat(sectionBItems.get(0).getItemType()).isEqualTo(ItemType.NUMBER);

        // section without items → empty list, not null
        assertThat(tree.findItemsBySectionId(999L)).isEmpty();
    }

    @Test
    @DisplayName("snapshot 为 null → 空 Optional, 不抛")
    void nullSnapshot_returnsEmpty() {
        TemplateVersion v = TemplateVersion.reconstruct(
                1L, 100L, 1, null, null, 1L, LocalDateTime.now());
        assertThat(SnapshotTree.from(v, mapper)).isEmpty();
    }

    @Test
    @DisplayName("snapshot 空字符串 → 空 Optional")
    void blankSnapshot_returnsEmpty() {
        TemplateVersion v = TemplateVersion.reconstruct(
                1L, 100L, 1, "   ", null, 1L, LocalDateTime.now());
        assertThat(SnapshotTree.from(v, mapper)).isEmpty();
    }

    @Test
    @DisplayName("损坏 JSON → 空 Optional, 不抛")
    void malformedJson_returnsEmpty() {
        TemplateVersion v = TemplateVersion.reconstruct(
                1L, 100L, 1, "{ invalid json", null, 1L, LocalDateTime.now());
        assertThat(SnapshotTree.from(v, mapper)).isEmpty();
    }

    @Test
    @DisplayName("JSON 仅有 rootSection 无 descendants/items → 仅 1 section, 0 items")
    void onlyRoot_okWithEmptyChildrenItems() {
        String json = "{\"rootSection\":{\"id\":1,\"sectionCode\":\"R\",\"sectionName\":\"Root\"}}";
        TemplateVersion v = TemplateVersion.reconstruct(
                1L, 1L, 1, json, null, 1L, LocalDateTime.now());
        Optional<SnapshotTree> tree = SnapshotTree.from(v, mapper);
        assertThat(tree).isPresent();
        assertThat(tree.get().getSections()).hasSize(1);
        assertThat(tree.get().getItems()).isEmpty();
    }

    @Test
    @DisplayName("toLiveSection — 关键字段保留")
    void toLiveSection_preservesKeyFields() {
        SnapshotSection s = new SnapshotSection();
        s.setId(101L);
        s.setTemplateId(100L);
        s.setParentSectionId(99L);
        s.setSectionCode("A");
        s.setSectionName("SA");
        s.setTargetType(TargetType.ORG);
        s.setTargetSourceMode("INDEPENDENT");
        s.setScoringConfig("{\"maxScore\":100}");

        var live = SnapshotTree.toLiveSection(s);
        assertThat(live).isNotNull();
        assertThat(live.getId()).isEqualTo(101L);
        assertThat(live.getSectionCode()).isEqualTo("A");
        assertThat(live.getSectionName()).isEqualTo("SA");
        assertThat(live.getTargetType()).isEqualTo(TargetType.ORG);
        assertThat(live.getTargetSourceMode()).isEqualTo("INDEPENDENT");
        assertThat(live.getScoringConfig()).isEqualTo("{\"maxScore\":100}");
    }

    @Test
    @DisplayName("toLiveItem — 关键字段保留")
    void toLiveItem_preservesKeyFields() {
        SnapshotItem it = new SnapshotItem();
        it.setId(201L);
        it.setSectionId(101L);
        it.setItemCode("I-1");
        it.setItemName("Item1");
        it.setItemType(ItemType.RATING);
        it.setScoringConfig("{\"maxScore\":10}");
        it.setValidationRules("[]");
        it.setConditionLogic(null);

        var live = SnapshotTree.toLiveItem(it);
        assertThat(live).isNotNull();
        assertThat(live.getId()).isEqualTo(201L);
        assertThat(live.getSectionId()).isEqualTo(101L);
        assertThat(live.getItemCode()).isEqualTo("I-1");
        assertThat(live.getItemType()).isEqualTo(ItemType.RATING);
    }

    @Test
    @DisplayName("toLiveSection(null) / toLiveItem(null) — 返回 null")
    void toLive_nullPassthrough() {
        assertThat(SnapshotTree.toLiveSection(null)).isNull();
        assertThat(SnapshotTree.toLiveItem(null)).isNull();
    }
}
