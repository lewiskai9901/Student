package com.school.management.infrastructure.persistence.inspection.v7.template;

import com.school.management.domain.inspection.model.v7.template.ItemType;
import com.school.management.domain.inspection.model.v7.template.TemplateItem;
import com.school.management.domain.inspection.repository.v7.TemplateItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class TemplateItemRepositoryImpl implements TemplateItemRepository {

    private final TemplateItemMapper mapper;

    public TemplateItemRepositoryImpl(TemplateItemMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TemplateItem save(TemplateItem item) {
        TemplateItemPO po = toPO(item);
        if (item.getId() == null) {
            mapper.insert(po);
            item.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return item;
    }

    @Override
    public Optional<TemplateItem> findById(Long id) {
        TemplateItemPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public List<TemplateItem> findBySectionId(Long sectionId) {
        return mapper.findBySectionId(sectionId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TemplateItem> findByTemplateId(Long templateId) {
        return mapper.findByTemplateId(templateId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteBySectionId(Long sectionId) {
        mapper.softDeleteBySectionId(sectionId);
    }

    @Override
    public List<TemplateItem> findByLibraryItemIdWithSync(Long libraryItemId) {
        return mapper.findByLibraryItemIdWithSync(libraryItemId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private TemplateItemPO toPO(TemplateItem domain) {
        TemplateItemPO po = new TemplateItemPO();
        po.setId(domain.getId());
        po.setTenantId(domain.getTenantId() != null ? domain.getTenantId() : 0L);
        po.setSectionId(domain.getSectionId());
        po.setItemCode(domain.getItemCode());
        po.setItemName(domain.getItemName());
        po.setDescription(domain.getDescription());
        po.setItemType(domain.getItemType() != null ? domain.getItemType().name() : null);
        po.setConfig(domain.getConfig());
        po.setValidationRules(domain.getValidationRules());
        po.setResponseSetId(domain.getResponseSetId());
        po.setScoringConfig(domain.getScoringConfig());
        po.setDimensionId(domain.getDimensionId());
        po.setHelpContent(domain.getHelpContent());
        po.setIsRequired(domain.getIsRequired());
        po.setIsScored(domain.getIsScored());
        po.setRequireEvidence(domain.getRequireEvidence());
        po.setItemWeight(domain.getItemWeight());
        po.setSortOrder(domain.getSortOrder());
        po.setConditionLogic(domain.getConditionLogic());
        po.setLibraryItemId(domain.getLibraryItemId());
        po.setSyncWithLibrary(domain.getSyncWithLibrary());
        po.setVisibilityLogic(domain.getVisibilityLogic());
        po.setScoringLogic(domain.getScoringLogic());
        po.setInputMode(domain.getInputMode());
        po.setLinkedEventTypeCode(domain.getLinkedEventTypeCode());
        po.setCreatedBy(domain.getCreatedBy());
        po.setCreatedAt(domain.getCreatedAt());
        po.setUpdatedBy(domain.getUpdatedBy());
        po.setUpdatedAt(domain.getUpdatedAt());
        return po;
    }

    private TemplateItem toDomain(TemplateItemPO po) {
        return TemplateItem.reconstruct(TemplateItem.builder()
                .id(po.getId())
                .sectionId(po.getSectionId())
                .itemCode(po.getItemCode())
                .itemName(po.getItemName())
                .description(po.getDescription())
                .itemType(po.getItemType() != null ? ItemType.valueOf(po.getItemType()) : null)
                .config(po.getConfig())
                .validationRules(po.getValidationRules())
                .responseSetId(po.getResponseSetId())
                .scoringConfig(po.getScoringConfig())
                .dimensionId(po.getDimensionId())
                .helpContent(po.getHelpContent())
                .isRequired(po.getIsRequired())
                .isScored(po.getIsScored())
                .requireEvidence(po.getRequireEvidence())
                .itemWeight(po.getItemWeight())
                .sortOrder(po.getSortOrder())
                .conditionLogic(po.getConditionLogic())
                .libraryItemId(po.getLibraryItemId())
                .syncWithLibrary(po.getSyncWithLibrary())
                .visibilityLogic(po.getVisibilityLogic())
                .scoringLogic(po.getScoringLogic())
                .inputMode(po.getInputMode())
                .linkedEventTypeCode(po.getLinkedEventTypeCode())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedBy(po.getUpdatedBy())
                .updatedAt(po.getUpdatedAt()));
    }
}
