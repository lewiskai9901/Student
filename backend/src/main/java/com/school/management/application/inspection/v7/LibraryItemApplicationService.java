package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.template.ItemType;
import com.school.management.domain.inspection.model.v7.template.LibraryItem;
import com.school.management.domain.inspection.model.v7.template.TemplateItem;
import com.school.management.domain.inspection.repository.v7.LibraryItemRepository;
import com.school.management.domain.inspection.repository.v7.TemplateItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class LibraryItemApplicationService {

    private final LibraryItemRepository libraryItemRepository;
    private final TemplateItemRepository templateItemRepository;

    @Transactional
    public LibraryItem createLibraryItem(String itemCode, String itemName, ItemType itemType,
                                          String description, String category, String tags,
                                          String defaultConfig, String defaultValidationRules,
                                          String defaultScoringConfig, String defaultHelpContent,
                                          Boolean isStandard, Long createdBy) {
        // Check code uniqueness
        libraryItemRepository.findByItemCode(itemCode).ifPresent(existing -> {
            throw new IllegalArgumentException("库项目编码已存在: " + itemCode);
        });

        LibraryItem item = LibraryItem.create(itemCode, itemName, itemType, createdBy);
        item.update(itemName, description, itemType, category, tags,
                defaultConfig, defaultValidationRules, defaultScoringConfig,
                defaultHelpContent, isStandard);
        return libraryItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<LibraryItem> listLibraryItems() {
        return libraryItemRepository.findAll();
    }

    @Transactional(readOnly = true)
    public LibraryItem getLibraryItem(Long id) {
        return libraryItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("库项目不存在: " + id));
    }

    @Transactional(readOnly = true)
    public List<LibraryItem> searchLibraryItems(String keyword, String category) {
        return libraryItemRepository.search(keyword, category);
    }

    @Transactional(readOnly = true)
    public List<String> getCategories() {
        return libraryItemRepository.findDistinctCategories();
    }

    @Transactional
    public LibraryItem updateLibraryItem(Long id, String itemName, String description,
                                          ItemType itemType, String category, String tags,
                                          String defaultConfig, String defaultValidationRules,
                                          String defaultScoringConfig, String defaultHelpContent,
                                          Boolean isStandard) {
        LibraryItem item = libraryItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("库项目不存在: " + id));
        item.update(itemName, description, itemType, category, tags,
                defaultConfig, defaultValidationRules, defaultScoringConfig,
                defaultHelpContent, isStandard);
        return libraryItemRepository.save(item);
    }

    @Transactional
    public void deleteLibraryItem(Long id) {
        LibraryItem item = libraryItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("库项目不存在: " + id));
        if (Boolean.TRUE.equals(item.getIsStandard())) {
            throw new IllegalStateException("标准项不可删除");
        }
        libraryItemRepository.deleteById(id);
    }

    /**
     * 从库中创建模板检查项
     */
    @Transactional
    public TemplateItem createItemFromLibrary(Long sectionId, Long libraryItemId,
                                               boolean syncWithLibrary, Long createdBy) {
        LibraryItem libraryItem = libraryItemRepository.findById(libraryItemId)
                .orElseThrow(() -> new IllegalArgumentException("库项目不存在: " + libraryItemId));

        TemplateItem item = TemplateItem.create(sectionId, libraryItem.getItemCode(),
                libraryItem.getItemName(), libraryItem.getItemType(), createdBy);
        item.update(libraryItem.getItemName(), libraryItem.getDescription(),
                libraryItem.getItemType(), libraryItem.getDefaultConfig(),
                libraryItem.getDefaultValidationRules(), null,
                libraryItem.getDefaultScoringConfig(), null,
                libraryItem.getDefaultHelpContent(), false, false, false,
                null, null, null, null, createdBy);
        item.linkToLibrary(libraryItemId, syncWithLibrary);

        TemplateItem saved = templateItemRepository.save(item);

        // Increment usage count
        libraryItem.incrementUsageCount();
        libraryItemRepository.save(libraryItem);

        return saved;
    }

    /**
     * 推送库项目更新到所有同步的模板项
     */
    @Transactional
    public int syncLibraryItemToTemplates(Long libraryItemId) {
        LibraryItem libraryItem = libraryItemRepository.findById(libraryItemId)
                .orElseThrow(() -> new IllegalArgumentException("库项目不存在: " + libraryItemId));

        List<TemplateItem> linkedItems = templateItemRepository.findByLibraryItemIdWithSync(libraryItemId);
        int count = 0;
        for (TemplateItem item : linkedItems) {
            item.update(libraryItem.getItemName(), libraryItem.getDescription(),
                    libraryItem.getItemType(), libraryItem.getDefaultConfig(),
                    libraryItem.getDefaultValidationRules(), item.getResponseSetId(),
                    libraryItem.getDefaultScoringConfig(), item.getDimensionId(),
                    libraryItem.getDefaultHelpContent(), item.getIsRequired(),
                    item.getIsScored(), item.getRequireEvidence(),
                    item.getItemWeight(), item.getConditionLogic(), null,
                    item.getLinkedEventTypeCode(), null);
            templateItemRepository.save(item);
            count++;
        }
        log.info("Synced library item {} to {} template items", libraryItemId, count);
        return count;
    }
}
