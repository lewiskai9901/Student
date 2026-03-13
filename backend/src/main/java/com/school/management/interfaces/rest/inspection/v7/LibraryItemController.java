package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.LibraryItemApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.template.ItemType;
import com.school.management.domain.inspection.model.v7.template.LibraryItem;
import com.school.management.domain.inspection.model.v7.template.TemplateItem;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LibraryItemController {

    private final LibraryItemApplicationService libraryItemService;

    @GetMapping("/v7/insp/library-items")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<List<LibraryItem>> listLibraryItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        if (keyword != null || category != null) {
            return Result.success(libraryItemService.searchLibraryItems(keyword, category));
        }
        return Result.success(libraryItemService.listLibraryItems());
    }

    @GetMapping("/v7/insp/library-items/categories")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<List<String>> getCategories() {
        return Result.success(libraryItemService.getCategories());
    }

    @GetMapping("/v7/insp/library-items/{id}")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<LibraryItem> getLibraryItem(@PathVariable Long id) {
        return Result.success(libraryItemService.getLibraryItem(id));
    }

    @PostMapping("/v7/insp/library-items")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<LibraryItem> createLibraryItem(@RequestBody CreateLibraryItemRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(libraryItemService.createLibraryItem(
                request.getItemCode(), request.getItemName(), request.getItemType(),
                request.getDescription(), request.getCategory(), request.getTags(),
                request.getDefaultConfig(), request.getDefaultValidationRules(),
                request.getDefaultScoringConfig(), request.getDefaultHelpContent(),
                request.getIsStandard(), userId));
    }

    @PutMapping("/v7/insp/library-items/{id}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<LibraryItem> updateLibraryItem(@PathVariable Long id,
                                                  @RequestBody UpdateLibraryItemRequest request) {
        return Result.success(libraryItemService.updateLibraryItem(id,
                request.getItemName(), request.getDescription(), request.getItemType(),
                request.getCategory(), request.getTags(),
                request.getDefaultConfig(), request.getDefaultValidationRules(),
                request.getDefaultScoringConfig(), request.getDefaultHelpContent(),
                request.getIsStandard()));
    }

    @DeleteMapping("/v7/insp/library-items/{id}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> deleteLibraryItem(@PathVariable Long id) {
        libraryItemService.deleteLibraryItem(id);
        return Result.success();
    }

    @PostMapping("/v7/insp/library-items/{id}/sync")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Integer> syncToTemplates(@PathVariable Long id) {
        int count = libraryItemService.syncLibraryItemToTemplates(id);
        return Result.success(count);
    }

    @PostMapping("/v7/insp/sections/{sectionId}/items/from-library")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<TemplateItem> createItemFromLibrary(@PathVariable Long sectionId,
                                                       @RequestBody CreateFromLibraryRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(libraryItemService.createItemFromLibrary(
                sectionId, request.getLibraryItemId(), request.isSyncWithLibrary(), userId));
    }

    @lombok.Data
    public static class CreateLibraryItemRequest {
        private String itemCode;
        private String itemName;
        private ItemType itemType;
        private String description;
        private String category;
        private String tags;
        private String defaultConfig;
        private String defaultValidationRules;
        private String defaultScoringConfig;
        private String defaultHelpContent;
        private Boolean isStandard;
    }

    @lombok.Data
    public static class UpdateLibraryItemRequest {
        private String itemName;
        private ItemType itemType;
        private String description;
        private String category;
        private String tags;
        private String defaultConfig;
        private String defaultValidationRules;
        private String defaultScoringConfig;
        private String defaultHelpContent;
        private Boolean isStandard;
    }

    @lombok.Data
    public static class CreateFromLibraryRequest {
        private Long libraryItemId;
        private boolean syncWithLibrary;
    }
}
