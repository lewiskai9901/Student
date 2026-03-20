package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.execution.TargetType;
import com.school.management.domain.inspection.model.v7.template.TemplateItem;
import com.school.management.domain.inspection.model.v7.template.TemplateSection;
import com.school.management.domain.inspection.repository.v7.TemplateItemRepository;
import com.school.management.domain.inspection.repository.v7.TemplateSectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * V62 统一分区模型 — 子分区应用服务
 *
 * 处理根分区树内的子分区 CRUD。不再有 templateId 参数，
 * 所有操作通过 parentSectionId 定位在分区树中的位置。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class TemplateSectionApplicationService {

    private final TemplateSectionRepository sectionRepository;
    private final TemplateItemRepository itemRepository;

    // ========== 子分区 CRUD ==========

    /**
     * 创建子分区
     *
     * @param parentSectionId 父分区ID（必须存在）
     * @param sectionCode     分区编码
     * @param sectionName     分区名称
     * @param targetType      目标类型（仅根分区的一级子分区可设）
     * @param weight          权重 0-100
     * @param isRepeatable    是否可重复
     * @param conditionLogic  条件逻辑 JSON
     * @param sortOrder       排序
     * @param createdBy       创建人
     */
    @Transactional
    public TemplateSection createChildSection(Long parentSectionId, String sectionCode,
                                               String sectionName, TargetType targetType,
                                               Integer weight, Boolean isRepeatable,
                                               String conditionLogic, Integer sortOrder,
                                               Long createdBy) {
        TemplateSection parent = sectionRepository.findById(parentSectionId)
                .orElseThrow(() -> new IllegalArgumentException("父分区不存在: " + parentSectionId));

        // Design B: 任何非根分区都可以设置 targetType

        TemplateSection child = TemplateSection.createChild(parentSectionId, sectionCode, sectionName, createdBy);

        // 设置可选属性
        child.update(sectionName,
                weight != null ? weight : 100,
                isRepeatable != null ? isRepeatable : false,
                conditionLogic, createdBy);
        if (sortOrder != null) {
            child.reorder(sortOrder);
        }
        if (targetType != null) {
            child.setTargetType(targetType);
        }

        TemplateSection saved = sectionRepository.save(child);
        log.info("创建子分区: id={}, parentId={}, name={}", saved.getId(), parentSectionId, sectionName);
        return saved;
    }

    /**
     * 创建引用分区 — 引用另一个分区（只读快捷方式）
     *
     * @param parentSectionId 父分区ID
     * @param refSectionId    被引用的分区ID
     * @param weight          权重
     * @param sortOrder       排序
     * @param createdBy       创建人
     */
    @Transactional
    public TemplateSection createRefSection(Long parentSectionId, Long refSectionId,
                                             Integer weight, Integer sortOrder,
                                             Long createdBy) {
        TemplateSection parent = sectionRepository.findById(parentSectionId)
                .orElseThrow(() -> new IllegalArgumentException("父分区不存在: " + parentSectionId));

        TemplateSection refTarget = sectionRepository.findById(refSectionId)
                .orElseThrow(() -> new IllegalArgumentException("引用目标分区不存在: " + refSectionId));

        // 循环引用检测：从 refSectionId 出发 BFS，如果能回到 parentSectionId 的根，说明循环
        if (hasCircularReference(parentSectionId, refSectionId)) {
            throw new IllegalArgumentException("检测到循环引用");
        }

        String sectionCode = "REF-" + System.currentTimeMillis() + "-"
                + ThreadLocalRandom.current().nextInt(1000, 9999);

        TemplateSection refSection = TemplateSection.createRef(
                parentSectionId, sectionCode, refTarget.getSectionName(), refSectionId, createdBy);

        if (weight != null) {
            refSection.update(refSection.getSectionName(), weight, false, null, createdBy);
        }
        if (sortOrder != null) {
            refSection.reorder(sortOrder);
        }

        TemplateSection saved = sectionRepository.save(refSection);
        log.info("创建引用分区: id={}, parentId={}, refSectionId={}", saved.getId(), parentSectionId, refSectionId);
        return saved;
    }

    /**
     * 循环引用检测：BFS 从 refSectionId 往上走（通过子孙的 refSectionId），
     * 检查是否能到达 parentSectionId 所在根分区。
     */
    private boolean hasCircularReference(Long parentSectionId, Long refSectionId) {
        // 找到 parentSectionId 所在的根分区
        Long rootId = findRootId(parentSectionId);
        if (rootId == null) return false;

        // BFS: 从 refSectionId 出发，检查它的子孙中是否有引用指向 rootId 或其子孙
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new LinkedList<>();
        queue.add(refSectionId);

        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            if (currentId.equals(rootId)) return true;
            if (!visited.add(currentId)) continue;

            // 检查 currentId 的子分区中是否有引用
            List<TemplateSection> children = sectionRepository.findByParentSectionId(currentId);
            for (TemplateSection child : children) {
                queue.add(child.getId());
                if (child.getRefSectionId() != null) {
                    queue.add(child.getRefSectionId());
                }
            }

            // 如果 currentId 本身也是根分区，检查它的后代
            TemplateSection current = sectionRepository.findById(currentId).orElse(null);
            if (current != null && current.getRefSectionId() != null) {
                queue.add(current.getRefSectionId());
            }
        }

        return false;
    }

    /**
     * 向上追溯找到根分区ID
     */
    private Long findRootId(Long sectionId) {
        Set<Long> visited = new HashSet<>();
        Long currentId = sectionId;
        while (currentId != null) {
            if (!visited.add(currentId)) return null; // 防止无限循环
            TemplateSection section = sectionRepository.findById(currentId).orElse(null);
            if (section == null) return null;
            if (section.isRoot()) return section.getId();
            currentId = section.getParentSectionId();
        }
        return null;
    }

    /**
     * 将引用分区转为本地副本（深度复制被引用的分区树）
     */
    @Transactional
    public TemplateSection cloneRefSection(Long sectionId, Long operatorId) {
        TemplateSection refSection = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("分区不存在: " + sectionId));
        if (refSection.getRefSectionId() == null) {
            throw new IllegalArgumentException("非引用分区不需要克隆");
        }

        Long refTargetId = refSection.getRefSectionId();
        TemplateSection refTarget = sectionRepository.findById(refTargetId)
                .orElseThrow(() -> new IllegalArgumentException("引用目标分区不存在: " + refTargetId));

        // 将当前引用分区转为本地分区（清除 refSectionId）
        TemplateSection cloned = TemplateSection.reconstruct(
                TemplateSection.builder()
                        .id(refSection.getId())
                        .parentSectionId(refSection.getParentSectionId())
                        .refSectionId(null) // 清除引用
                        .sectionCode(refSection.getSectionCode())
                        .sectionName(refSection.getSectionName())
                        .sortOrder(refSection.getSortOrder())
                        .weight(refSection.getWeight())
                        .isRepeatable(refSection.getIsRepeatable())
                        .conditionLogic(refSection.getConditionLogic())
                        .scoringConfig(refTarget.getScoringConfig())
                        .createdBy(refSection.getCreatedBy())
                        .createdAt(refSection.getCreatedAt())
                        .updatedBy(operatorId)
                        .updatedAt(LocalDateTime.now())
        );
        sectionRepository.save(cloned);

        // 深度复制被引用分区的子树
        List<TemplateSection> refDescendants = sectionRepository.findDescendants(refTargetId);
        List<TemplateSection> refDirectChildren = refDescendants.stream()
                .filter(s -> refTargetId.equals(s.getParentSectionId()))
                .sorted(Comparator.comparingInt(TemplateSection::getSortOrder))
                .collect(Collectors.toList());

        Map<Long, Long> idMap = new HashMap<>();
        idMap.put(refTargetId, refSection.getId());
        copySubTree(refDirectChildren, refDescendants, refSection.getId(), idMap, operatorId);

        // 复制被引用分区自身的 items 到当前分区
        copyItemsForSection(refTargetId, refSection.getId(), operatorId);

        // 复制子孙分区的 items
        for (TemplateSection desc : refDescendants) {
            Long newSectionId = idMap.get(desc.getId());
            if (newSectionId != null) {
                copyItemsForSection(desc.getId(), newSectionId, operatorId);
            }
        }

        log.info("克隆引用分区: sectionId={}, refTargetId={}, copiedSections={}",
                sectionId, refTargetId, idMap.size());
        return cloned;
    }

    /**
     * 更新子分区信息
     *
     * @param targetType 仅一级子分区（父分区为根分区）可编辑 targetType
     */
    @Transactional
    public TemplateSection updateSection(Long id, String sectionName, TargetType targetType,
                                          String targetSourceMode, String targetTypeFilter,
                                          Integer weight, Boolean isRepeatable,
                                          String conditionLogic, Long updatedBy) {
        TemplateSection section = sectionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分区不存在: " + id));

        // Design B: 任何非根分区都可以设置 targetType
        if (targetType != null) {
            section.setTargetType(targetType);
        } else {
            section.setTargetType(null);
        }

        section.setTargetSourceMode(targetSourceMode);
        section.setTargetTypeFilter(targetTypeFilter);
        section.update(sectionName, weight, isRepeatable, conditionLogic, updatedBy);
        return sectionRepository.save(section);
    }

    /**
     * 删除分区 — 递归删除所有子孙分区及其检查项
     */
    @Transactional
    public void deleteSection(Long id) {
        TemplateSection section = sectionRepository.findById(id).orElse(null);
        if (section == null) return;

        // 递归删除子分区
        List<TemplateSection> children = sectionRepository.findByParentSectionId(id);
        for (TemplateSection child : children) {
            deleteSection(child.getId());
        }

        // 删除当前分区的检查项
        itemRepository.deleteBySectionId(id);
        sectionRepository.deleteById(id);

        log.debug("删除分区: id={}", id);
    }

    /**
     * 重新排序同级分区
     *
     * @param parentSectionId  父分区ID
     * @param sectionIdsInOrder 排序后的分区ID列表
     */
    @Transactional
    public void reorderSections(Long parentSectionId, List<Long> sectionIdsInOrder) {
        for (int i = 0; i < sectionIdsInOrder.size(); i++) {
            TemplateSection section = sectionRepository.findById(sectionIdsInOrder.get(i))
                    .orElseThrow(() -> new IllegalArgumentException("分区不存在"));

            // 验证分区确实属于指定的父分区
            if (!Objects.equals(section.getParentSectionId(), parentSectionId)) {
                throw new IllegalArgumentException(
                        "分区 " + section.getId() + " 不属于父分区 " + parentSectionId);
            }

            section.reorder(i);
            sectionRepository.save(section);
        }
        log.info("重排分区: parentId={}, count={}", parentSectionId, sectionIdsInOrder.size());
    }

    /**
     * 更新分区评分配置（引用分区不可修改）
     */
    @Transactional
    public TemplateSection updateScoringConfig(Long sectionId, String scoringConfig, Long updatedBy) {
        TemplateSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new IllegalArgumentException("分区不存在: " + sectionId));
        if (section.isRef()) {
            throw new IllegalArgumentException("引用分区不可修改评分配置");
        }
        section.updateScoringConfig(scoringConfig, updatedBy);
        return sectionRepository.save(section);
    }

    // ========== 查询 ==========

    /**
     * 列出某分区的直接子分区
     */
    @Transactional(readOnly = true)
    public List<TemplateSection> listChildren(Long parentSectionId) {
        return sectionRepository.findByParentSectionId(parentSectionId);
    }

    /**
     * 获取根分区下完整的分区树（所有子孙）
     */
    @Transactional(readOnly = true)
    public List<TemplateSection> getSectionTree(Long rootSectionId) {
        // 验证是根分区
        TemplateSection root = sectionRepository.findById(rootSectionId)
                .orElseThrow(() -> new IllegalArgumentException("根分区不存在: " + rootSectionId));
        if (!root.isRoot()) {
            throw new IllegalArgumentException("ID " + rootSectionId + " 不是根分区");
        }
        return sectionRepository.findDescendants(rootSectionId);
    }

    // ========== 私有方法 ==========

    /**
     * 递归复制子树
     */
    private void copySubTree(List<TemplateSection> currentLevel,
                              List<TemplateSection> allSections,
                              Long targetParentId,
                              Map<Long, Long> idMap,
                              Long operatorId) {
        for (TemplateSection src : currentLevel) {
            String code = "S-" + System.currentTimeMillis() + "-"
                    + ThreadLocalRandom.current().nextInt(1000, 9999);
            TemplateSection copy = TemplateSection.reconstruct(
                    TemplateSection.builder()
                            .parentSectionId(targetParentId)
                            .sectionCode(code)
                            .sectionName(src.getSectionName())
                            .targetType(src.getTargetType())
                            .sortOrder(src.getSortOrder())
                            .weight(src.getWeight())
                            .isRepeatable(src.getIsRepeatable())
                            .conditionLogic(src.getConditionLogic())
                            .scoringConfig(src.getScoringConfig())
                            .createdBy(operatorId)
            );
            copy = sectionRepository.save(copy);
            idMap.put(src.getId(), copy.getId());

            // 递归处理子分区
            List<TemplateSection> children = allSections.stream()
                    .filter(s -> src.getId().equals(s.getParentSectionId()))
                    .sorted(Comparator.comparingInt(TemplateSection::getSortOrder))
                    .collect(Collectors.toList());
            if (!children.isEmpty()) {
                copySubTree(children, allSections, copy.getId(), idMap, operatorId);
            }
        }
    }

    /**
     * 复制某分区下的所有检查项到新分区
     */
    private void copyItemsForSection(Long sourceSectionId, Long targetSectionId, Long operatorId) {
        List<TemplateItem> srcItems = itemRepository.findBySectionId(sourceSectionId);
        for (TemplateItem srcItem : srcItems) {
            String itemCode = "I-" + System.currentTimeMillis() + "-"
                    + ThreadLocalRandom.current().nextInt(1000, 9999);
            TemplateItem newItem = TemplateItem.create(
                    targetSectionId, itemCode, srcItem.getItemName(),
                    srcItem.getItemType(), operatorId);
            newItem.update(
                    srcItem.getItemName(), srcItem.getDescription(), srcItem.getItemType(),
                    srcItem.getConfig(), srcItem.getValidationRules(), srcItem.getResponseSetId(),
                    srcItem.getScoringConfig(), srcItem.getDimensionId(), srcItem.getHelpContent(),
                    srcItem.getIsRequired(), srcItem.getIsScored(), srcItem.getRequireEvidence(),
                    srcItem.getItemWeight(), srcItem.getConditionLogic(), operatorId);
            newItem.reorder(srcItem.getSortOrder());
            itemRepository.save(newItem);
        }
    }
}
