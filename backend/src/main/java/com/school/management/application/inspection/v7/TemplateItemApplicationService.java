package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.template.ItemType;
import com.school.management.domain.inspection.model.v7.template.TemplateItem;
import com.school.management.domain.inspection.repository.v7.TemplateItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service("v7TemplateItemApplicationService")
public class TemplateItemApplicationService {

    private final TemplateItemRepository itemRepository;

    @Transactional
    public TemplateItem createItem(Long sectionId, String itemCode, String itemName,
                                    ItemType itemType, String description, String config,
                                    String validationRules, Long responseSetId,
                                    String scoringConfig, Long dimensionId, String helpContent,
                                    Boolean isRequired, Boolean isScored, Boolean requireEvidence,
                                    BigDecimal itemWeight, String conditionLogic,
                                    Integer sortOrder, Long createdBy) {
        TemplateItem item = TemplateItem.create(sectionId, itemCode, itemName, itemType, createdBy);
        item.update(itemName, description, itemType, config, validationRules, responseSetId,
                scoringConfig, dimensionId, helpContent, isRequired, isScored, requireEvidence,
                itemWeight, conditionLogic, createdBy);
        if (sortOrder != null) {
            item.reorder(sortOrder);
        }
        return itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<TemplateItem> listItems(Long sectionId) {
        return itemRepository.findBySectionId(sectionId);
    }

    @Transactional
    public TemplateItem updateItem(Long id, String itemName, String description,
                                    ItemType itemType, String config, String validationRules,
                                    Long responseSetId, String scoringConfig, Long dimensionId,
                                    String helpContent,
                                    Boolean isRequired, Boolean isScored, Boolean requireEvidence,
                                    BigDecimal itemWeight, String conditionLogic, Long updatedBy) {
        TemplateItem item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("字段不存在: " + id));
        item.update(itemName, description, itemType, config, validationRules, responseSetId,
                scoringConfig, dimensionId, helpContent, isRequired, isScored, requireEvidence,
                itemWeight, conditionLogic, updatedBy);
        return itemRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long id) {
        itemRepository.deleteById(id);
    }

    @Transactional
    public void reorderItems(Long sectionId, List<Long> itemIds) {
        for (int i = 0; i < itemIds.size(); i++) {
            TemplateItem item = itemRepository.findById(itemIds.get(i))
                    .orElseThrow(() -> new IllegalArgumentException("字段不存在"));
            item.reorder(i);
            itemRepository.save(item);
        }
    }
}
