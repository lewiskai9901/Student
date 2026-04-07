package com.school.management.application.inspection.v7;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.domain.inspection.model.v7.execution.TargetType;
import com.school.management.domain.inspection.model.v7.template.*;
import com.school.management.domain.inspection.repository.v7.*;
import com.school.management.infrastructure.event.SpringDomainEventPublisher;
import com.school.management.common.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * V62 统一分区模型 — 根分区应用服务
 *
 * 根分区（parentSectionId=null, templateId=null）替代原来的 InspTemplate。
 * 所有"模板级"操作（创建、发布、版本管理、导出等）都通过根分区进行。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class InspTemplateApplicationService {

    private final TemplateSectionRepository sectionRepository;
    private final TemplateVersionRepository versionRepository;
    private final TemplateItemRepository itemRepository;
    private final SpringDomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    // ========== 根分区 CRUD ==========

    /**
     * 创建根分区（即创建新模板）
     */
    @Transactional
    public TemplateSection createRootSection(String name, String description,
                                              Long catalogId, String tags,
                                              Long createdBy) {
        String code = generateSectionCode();
        TemplateSection root = TemplateSection.createRoot(code, name, createdBy);

        // 保存先拿到ID，再设置可选字段
        root = sectionRepository.save(root);

        if (description != null || catalogId != null || tags != null) {
            root.updateInfo(name, description, tags, catalogId, createdBy);
            root = sectionRepository.save(root);
        }

        log.info("创建根分区: id={}, code={}, name={}", root.getId(), code, name);
        return root;
    }

    /**
     * 获取根分区详情
     */
    @Transactional(readOnly = true)
    public Optional<TemplateSection> getRootSection(Long id) {
        return sectionRepository.findById(id)
                .filter(TemplateSection::isRoot);
    }

    /**
     * 分页查询根分区列表
     */
    @Transactional(readOnly = true)
    public PageResult<TemplateSection> listRootSections(int page, int size,
                                                         TemplateStatus status,
                                                         Long catalogId,
                                                         String keyword) {
        int offset = (page - 1) * size;
        String statusStr = status != null ? status.name() : null;
        List<TemplateSection> records = sectionRepository.findRootSectionsPaged(
                offset, size, statusStr, catalogId, keyword);
        int total = sectionRepository.countRootSections(statusStr, catalogId, keyword);
        return PageResult.of(records, total, page, size);
    }

    /**
     * 更新根分区基本信息
     */
    @Transactional
    public TemplateSection updateRootSection(Long id, String name, String description,
                                              Long catalogId, String tags, Long updatedBy) {
        TemplateSection root = findRootOrThrow(id);
        root.updateInfo(name, description, tags, catalogId, updatedBy);
        return sectionRepository.save(root);
    }

    /**
     * 删除根分区 — 级联删除所有子孙分区及其检查项
     * 已发布的根分区不能直接删除，需先弃用/归档。
     */
    @Transactional
    public void deleteRootSection(Long id) {
        TemplateSection root = findRootOrThrow(id);

        if (root.getStatus() == TemplateStatus.PUBLISHED) {
            throw new IllegalStateException("已发布的模板需先弃用才能删除");
        }

        // 获取所有子孙，逆序删除（叶子节点优先）
        List<TemplateSection> descendants = sectionRepository.findDescendants(id);
        // 从叶子到根删除
        List<TemplateSection> reversed = new ArrayList<>(descendants);
        Collections.reverse(reversed);

        for (TemplateSection section : reversed) {
            itemRepository.deleteBySectionId(section.getId());
            sectionRepository.deleteById(section.getId());
        }

        // 最后删除根分区本身的 items 和根分区
        itemRepository.deleteBySectionId(id);
        sectionRepository.deleteById(id);

        log.info("删除根分区及所有子孙: rootId={}, descendantCount={}", id, descendants.size());
    }

    // ========== 生命周期管理 ==========

    /**
     * 发布根分区 — 创建不可变版本快照
     */
    @Transactional
    public TemplateVersion publishRootSection(Long id, Long operatorId) {
        TemplateSection root = findRootOrThrow(id);

        // 收集所有子孙分区
        List<TemplateSection> descendants = sectionRepository.findDescendants(id);
        if (descendants.isEmpty()) {
            throw new IllegalArgumentException("模板至少需要一个子分区才能发布");
        }

        // 收集所有检查项（根分区 + 所有子孙）
        List<TemplateItem> allItems = new ArrayList<>();
        allItems.addAll(itemRepository.findBySectionId(id));
        for (TemplateSection desc : descendants) {
            List<TemplateItem> sectionItems = itemRepository.findBySectionId(desc.getId());
            if (sectionItems.isEmpty()) {
                log.warn("分区 [{}](id={}) 没有检查项", desc.getSectionName(), desc.getId());
            }
            allItems.addAll(sectionItems);
        }

        // 校验：模板至少需要一个检查项
        if (allItems.isEmpty()) {
            throw new IllegalStateException("模板至少需要一个检查项才能发布");
        }

        // 构建结构快照 JSON
        String structureSnapshot;
        try {
            Map<String, Object> structure = new LinkedHashMap<>();
            structure.put("rootSection", root);
            structure.put("sections", descendants);
            structure.put("items", allItems);
            structureSnapshot = objectMapper.writeValueAsString(structure);
        } catch (Exception e) {
            throw new RuntimeException("序列化模板结构失败", e);
        }

        // 调用领域模型的 publish 方法（更新状态、版本号）
        TemplateVersion version = root.publish(descendants, allItems, structureSnapshot, null);
        sectionRepository.save(root);
        versionRepository.save(version);

        // 发布领域事件
        root.getDomainEvents().forEach(eventPublisher::publish);
        root.clearDomainEvents();

        log.info("发布根分区: id={}, version={}", id, version.getVersion());
        return version;
    }

    /**
     * 弃用根分区
     */
    @Transactional
    public void deprecateRootSection(Long id) {
        TemplateSection root = findRootOrThrow(id);
        root.deprecate();
        sectionRepository.save(root);
        log.info("弃用根分区: id={}", id);
    }

    /**
     * 归档根分区
     */
    @Transactional
    public void archiveRootSection(Long id) {
        TemplateSection root = findRootOrThrow(id);
        root.archive();
        sectionRepository.save(root);
        log.info("归档根分区: id={}", id);
    }

    // ========== 复制 ==========

    /**
     * 深度复制根分区（含完整分区树 + 检查项）
     */
    @Transactional
    public TemplateSection duplicateRootSection(Long id, Long operatorId) {
        TemplateSection source = findRootOrThrow(id);

        // 1. 创建新根分区
        String newCode = generateSectionCode();
        TemplateSection newRoot = TemplateSection.createRoot(newCode, source.getSectionName() + "(副本)", operatorId);
        newRoot = sectionRepository.save(newRoot);

        // 设置可选字段
        newRoot.updateInfo(
                newRoot.getSectionName(),
                source.getDescription(),
                source.getTags(),
                source.getCatalogId(),
                operatorId
        );
        if (source.getScoringConfig() != null) {
            newRoot.updateScoringConfig(source.getScoringConfig(), operatorId);
        }
        newRoot = sectionRepository.save(newRoot);

        // 2. 获取所有子孙分区，按拓扑排序（父节点在前）
        List<TemplateSection> descendants = sectionRepository.findDescendants(id);
        List<TemplateSection> sorted = topologicalSort(descendants);

        // 3. 逐个复制子孙分区，维护 oldId → newId 映射
        Map<Long, Long> idMap = new HashMap<>();
        idMap.put(id, newRoot.getId()); // 根分区映射

        for (TemplateSection src : sorted) {
            Long newParentId = idMap.get(src.getParentSectionId());
            if (newParentId == null) {
                log.warn("复制分区时找不到父节点映射: srcId={}, parentId={}", src.getId(), src.getParentSectionId());
                continue;
            }

            TemplateSection copy = TemplateSection.reconstruct(
                    TemplateSection.builder()
                            .parentSectionId(newParentId)
                            .refSectionId(src.getRefSectionId())
                            .sectionCode(src.getSectionCode() + "-COPY")
                            .sectionName(src.getSectionName())
                            .targetType(src.getTargetType())
                            .sortOrder(src.getSortOrder())
                            .isRepeatable(src.getIsRepeatable())
                            .conditionLogic(src.getConditionLogic())
                            .scoringConfig(src.getScoringConfig())
                            .createdBy(operatorId)
            );
            copy = sectionRepository.save(copy);
            idMap.put(src.getId(), copy.getId());
        }

        // 4. 复制所有检查项
        // 根分区自身的 items
        copyItemsForSection(id, newRoot.getId(), operatorId);
        // 子孙分区的 items
        for (TemplateSection src : sorted) {
            Long newSectionId = idMap.get(src.getId());
            if (newSectionId != null) {
                copyItemsForSection(src.getId(), newSectionId, operatorId);
            }
        }

        log.info("复制根分区完成: sourceId={}, newId={}, sectionCount={}",
                id, newRoot.getId(), idMap.size());
        return newRoot;
    }

    // ========== 版本管理 ==========

    /**
     * 查询根分区的所有版本
     */
    @Transactional(readOnly = true)
    public List<TemplateVersion> listVersions(Long rootSectionId) {
        // 确认是根分区
        findRootOrThrow(rootSectionId);
        return versionRepository.findByTemplateId(rootSectionId);
    }

    /**
     * 获取特定版本
     */
    @Transactional(readOnly = true)
    public Optional<TemplateVersion> getVersion(Long rootSectionId, Integer version) {
        findRootOrThrow(rootSectionId);
        return versionRepository.findByTemplateIdAndVersion(rootSectionId, version);
    }

    // ========== 导出 ==========

    /**
     * 导出根分区为 JSON（含完整分区树 + 检查项）
     */
    @Transactional(readOnly = true)
    public String exportRootSection(Long id) {
        TemplateSection root = findRootOrThrow(id);
        List<TemplateSection> descendants = sectionRepository.findDescendants(id);

        // 收集所有检查项
        List<TemplateItem> allItems = new ArrayList<>();
        allItems.addAll(itemRepository.findBySectionId(id));
        for (TemplateSection desc : descendants) {
            allItems.addAll(itemRepository.findBySectionId(desc.getId()));
        }

        try {
            Map<String, Object> export = new LinkedHashMap<>();
            export.put("rootSection", root);
            export.put("sections", descendants);
            export.put("items", allItems);
            export.put("exportedAt", LocalDateTime.now().toString());
            return objectMapper.writeValueAsString(export);
        } catch (Exception e) {
            throw new RuntimeException("导出模板失败", e);
        }
    }

    // ========== 私有方法 ==========

    private TemplateSection findRootOrThrow(Long id) {
        TemplateSection section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("根分区不存在: " + id));
        if (!section.isRoot()) {
            throw new IllegalArgumentException("ID " + id + " 不是根分区");
        }
        return section;
    }

    /**
     * 拓扑排序：保证父节点排在子节点之前
     */
    private List<TemplateSection> topologicalSort(List<TemplateSection> sections) {
        Map<Long, TemplateSection> byId = sections.stream()
                .collect(Collectors.toMap(TemplateSection::getId, s -> s));
        Map<Long, List<TemplateSection>> byParent = sections.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getParentSectionId() != null ? s.getParentSectionId() : -1L));

        List<TemplateSection> result = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        // BFS from roots of this sub-tree (sections whose parent is not in the set)
        Set<Long> idSet = byId.keySet();
        Queue<Long> queue = new LinkedList<>();
        for (TemplateSection s : sections) {
            if (s.getParentSectionId() == null || !idSet.contains(s.getParentSectionId())) {
                queue.add(s.getId());
            }
        }

        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            if (!visited.add(currentId)) continue;
            TemplateSection current = byId.get(currentId);
            if (current != null) {
                result.add(current);
            }
            List<TemplateSection> children = byParent.getOrDefault(currentId, Collections.emptyList());
            for (TemplateSection child : children) {
                queue.add(child.getId());
            }
        }

        return result;
    }

    /**
     * 复制某个分区下的所有检查项到新分区
     */
    private void copyItemsForSection(Long sourceSectionId, Long targetSectionId, Long operatorId) {
        List<TemplateItem> srcItems = itemRepository.findBySectionId(sourceSectionId);
        for (TemplateItem srcItem : srcItems) {
            TemplateItem newItem = TemplateItem.create(
                    targetSectionId, srcItem.getItemCode() + "-COPY",
                    srcItem.getItemName(), srcItem.getItemType(), operatorId);
            newItem.update(
                    srcItem.getItemName(), srcItem.getDescription(), srcItem.getItemType(),
                    srcItem.getConfig(), srcItem.getValidationRules(), srcItem.getResponseSetId(),
                    srcItem.getScoringConfig(), srcItem.getDimensionId(), srcItem.getHelpContent(),
                    srcItem.getIsRequired(), srcItem.getIsScored(), srcItem.getRequireEvidence(),
                    srcItem.getItemWeight(), srcItem.getConditionLogic(), srcItem.getInputMode(),
                    srcItem.getLinkedEventTypeCode(), operatorId);
            newItem.reorder(srcItem.getSortOrder());
            itemRepository.save(newItem);
        }
    }

    private String generateSectionCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "SEC-" + datePart + "-" + randomPart;
    }
}
