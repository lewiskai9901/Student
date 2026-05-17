package com.school.management.domain.inspection.model.template.snapshot;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.template.TemplateVersion;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 模板结构快照树 — 反序列化 {@link TemplateVersion#getStructureSnapshot()} JSON,
 * 提供等价 live repository 的 lookup 接口让 populate 路径走快照.
 *
 * <p>J1 (2026-05-17): G 项"真重放"的核心实现. 之前 snapshot 写入完整但零业务读者,
 * populate 100% 走 live, 模板升级会污染已发布项目的新 task. 现在:
 * <pre>
 *   InspTaskApplicationService.populateSubmissions
 *     → SnapshotTree.from(templateVersion)   // 反序列化
 *     → tree.findByParentSectionId(rootId)   // 替代 sectionRepository
 *     → tree.findBySectionId(secId)          // 替代 itemRepository
 * </pre>
 *
 * <p>建索引一次, 后续 lookup O(1). snapshot=NULL 时 from() 返回 empty Optional, 调用方 fallback live.
 */
@Slf4j
@Getter
public class SnapshotTree {

    private final List<SnapshotSection> sections;
    private final List<SnapshotItem> items;

    // O(1) lookup indexes
    private final Map<Long, SnapshotSection> sectionsById;
    private final Map<Long, List<SnapshotSection>> sectionsByParentId;
    private final Map<Long, List<SnapshotItem>> itemsBySectionId;

    private SnapshotTree(List<SnapshotSection> sections, List<SnapshotItem> items) {
        this.sections = Collections.unmodifiableList(sections);
        this.items = Collections.unmodifiableList(items);

        this.sectionsById = new HashMap<>();
        this.sectionsByParentId = new HashMap<>();
        for (SnapshotSection s : sections) {
            if (s.getId() != null) sectionsById.put(s.getId(), s);
            if (s.getParentSectionId() != null) {
                sectionsByParentId.computeIfAbsent(s.getParentSectionId(), k -> new ArrayList<>()).add(s);
            }
        }

        this.itemsBySectionId = new HashMap<>();
        for (SnapshotItem it : items) {
            if (it.getSectionId() != null) {
                itemsBySectionId.computeIfAbsent(it.getSectionId(), k -> new ArrayList<>()).add(it);
            }
        }
    }

    /**
     * 反序列化 TemplateVersion.structureSnapshot 为 SnapshotTree.
     *
     * <p>JSON 结构 (由 InspTemplateApplicationService.publishRootSection 写入):
     * <pre>{@code
     * {
     *   "rootSection": <Section>,
     *   "sections": [<Section>...],  // descendants 不含 root
     *   "items": [<Item>...],
     *   "scoring": {...},
     *   "snapshotAt": "ISO datetime"
     * }
     * }</pre>
     *
     * @return empty 表示 snapshot 为空/损坏, 调用方应 fallback live
     */
    public static Optional<SnapshotTree> from(TemplateVersion version, ObjectMapper mapper) {
        if (version == null || version.getStructureSnapshot() == null
                || version.getStructureSnapshot().isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode root = mapper.readTree(version.getStructureSnapshot());
            List<SnapshotSection> sections = new ArrayList<>();

            // rootSection (顶层, parentSectionId 为 null)
            JsonNode rootSectionNode = root.get("rootSection");
            if (rootSectionNode != null && !rootSectionNode.isNull()) {
                SnapshotSection rootSec = mapper.treeToValue(rootSectionNode, SnapshotSection.class);
                if (rootSec != null) sections.add(rootSec);
            }

            // descendants
            JsonNode descendants = root.get("sections");
            if (descendants != null && descendants.isArray()) {
                List<SnapshotSection> descList = mapper.convertValue(
                        descendants, new TypeReference<List<SnapshotSection>>() {});
                if (descList != null) sections.addAll(descList);
            }

            // items
            List<SnapshotItem> items = new ArrayList<>();
            JsonNode itemsNode = root.get("items");
            if (itemsNode != null && itemsNode.isArray()) {
                List<SnapshotItem> itemList = mapper.convertValue(
                        itemsNode, new TypeReference<List<SnapshotItem>>() {});
                if (itemList != null) items.addAll(itemList);
            }

            if (sections.isEmpty() && items.isEmpty()) {
                log.warn("[SnapshotTree] templateVersion {} structureSnapshot 解析后为空", version.getId());
                return Optional.empty();
            }

            log.debug("[SnapshotTree] templateVersion {} 解析: {} sections + {} items",
                    version.getId(), sections.size(), items.size());
            return Optional.of(new SnapshotTree(sections, items));
        } catch (Exception e) {
            log.warn("[SnapshotTree] 反序列化 templateVersion {} structureSnapshot 失败: {}",
                    version.getId(), e.getMessage());
            return Optional.empty();
        }
    }

    /** 等价 sectionRepository.findById(id). */
    public Optional<SnapshotSection> findSectionById(Long id) {
        return Optional.ofNullable(sectionsById.get(id));
    }

    /** 等价 sectionRepository.findByParentSectionId(parentId). */
    public List<SnapshotSection> findByParentSectionId(Long parentId) {
        return sectionsByParentId.getOrDefault(parentId, Collections.emptyList());
    }

    /** 等价 itemRepository.findBySectionId(sectionId). */
    public List<SnapshotItem> findItemsBySectionId(Long sectionId) {
        return itemsBySectionId.getOrDefault(sectionId, Collections.emptyList());
    }

    /** 检查 snapshot 是否真实包含内容. */
    public boolean isEmpty() {
        return sections.isEmpty() && items.isEmpty();
    }

    // ========== 转换 → live entity (供 populate 路径平滑使用) ==========

    /**
     * SnapshotSection → TemplateSection 实例 (in-memory, 未持久化).
     *
     * <p>仅复刻 populate 路径需要的字段; 其余 (description/tags/sortOrder 等) 不携带,
     * populate 不读这些字段所以无影响.
     */
    public static com.school.management.domain.inspection.model.template.TemplateSection toLiveSection(SnapshotSection s) {
        if (s == null) return null;
        return com.school.management.domain.inspection.model.template.TemplateSection.reconstruct(
                com.school.management.domain.inspection.model.template.TemplateSection.builder()
                        .id(s.getId())
                        .templateId(s.getTemplateId())
                        .parentSectionId(s.getParentSectionId())
                        .sectionCode(s.getSectionCode())
                        .sectionName(s.getSectionName())
                        .targetType(s.getTargetType())
                        .targetSourceMode(s.getTargetSourceMode())
                        .targetTypeFilter(s.getTargetTypeFilter())
                        .scoringConfig(s.getScoringConfig()));
    }

    /** SnapshotItem → TemplateItem 实例 (in-memory, 未持久化). */
    public static com.school.management.domain.inspection.model.template.TemplateItem toLiveItem(SnapshotItem it) {
        if (it == null) return null;
        return com.school.management.domain.inspection.model.template.TemplateItem.reconstruct(
                com.school.management.domain.inspection.model.template.TemplateItem.builder()
                        .id(it.getId())
                        .sectionId(it.getSectionId())
                        .itemCode(it.getItemCode())
                        .itemName(it.getItemName())
                        .itemType(it.getItemType())
                        .scoringConfig(it.getScoringConfig())
                        .validationRules(it.getValidationRules())
                        .conditionLogic(it.getConditionLogic()));
    }
}
