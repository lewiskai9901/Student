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

import java.util.List;
import java.util.Objects;

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

}
