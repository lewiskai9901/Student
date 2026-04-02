package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.TemplateItemApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.template.ItemType;
import com.school.management.domain.inspection.model.v7.template.TemplateItem;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController("v7TemplateItemController")
@RequiredArgsConstructor
public class TemplateItemController {

    private final TemplateItemApplicationService itemService;

    @PostMapping("/v7/insp/sections/{sectionId}/items")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<TemplateItem> createItem(@PathVariable Long sectionId,
                                            @RequestBody CreateItemRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(itemService.createItem(
                sectionId, request.getItemCode(), request.getItemName(),
                request.getItemType(), request.getDescription(), request.getConfig(),
                request.getValidationRules(), request.getResponseSetId(),
                request.getScoringConfig(), request.getDimensionId(), request.getHelpContent(),
                request.getIsRequired(), request.getIsScored(), request.getRequireEvidence(),
                request.getItemWeight(), request.getConditionLogic(),
                request.getInputMode(),
                request.getSortOrder(), userId));
    }

    @GetMapping("/v7/insp/sections/{sectionId}/items")
    @CasbinAccess(resource = "insp:template", action = "view")
    public Result<List<TemplateItem>> listItems(@PathVariable Long sectionId) {
        return Result.success(itemService.listItems(sectionId));
    }

    @PutMapping("/v7/insp/items/{itemId}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<TemplateItem> updateItem(@PathVariable Long itemId,
                                            @RequestBody UpdateItemRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(itemService.updateItem(itemId,
                request.getItemName(), request.getDescription(),
                request.getItemType(), request.getConfig(), request.getValidationRules(),
                request.getResponseSetId(), request.getScoringConfig(), request.getDimensionId(),
                request.getHelpContent(),
                request.getIsRequired(), request.getIsScored(), request.getRequireEvidence(),
                request.getItemWeight(), request.getConditionLogic(),
                request.getInputMode(), userId));
    }

    @DeleteMapping("/v7/insp/items/{itemId}")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> deleteItem(@PathVariable Long itemId) {
        itemService.deleteItem(itemId);
        return Result.success();
    }

    @PutMapping("/v7/insp/sections/{sectionId}/items/reorder")
    @CasbinAccess(resource = "insp:template", action = "edit")
    public Result<Void> reorderItems(@PathVariable Long sectionId,
                                      @RequestBody List<Long> itemIds) {
        itemService.reorderItems(sectionId, itemIds);
        return Result.success();
    }

    @lombok.Data
    public static class CreateItemRequest {
        private String itemCode;
        private String itemName;
        private String description;
        private ItemType itemType;
        private String config;
        private String validationRules;
        private Long responseSetId;
        private String scoringConfig;
        private Long dimensionId;
        private String helpContent;
        private Boolean isRequired;
        private Boolean isScored;
        private Boolean requireEvidence;
        private BigDecimal itemWeight;
        private String conditionLogic;
        private String inputMode;
        private Integer sortOrder;
    }

    @lombok.Data
    public static class UpdateItemRequest {
        private String itemName;
        private String description;
        private ItemType itemType;
        private String config;
        private String validationRules;
        private Long responseSetId;
        private String scoringConfig;
        private Long dimensionId;
        private String helpContent;
        private Boolean isRequired;
        private Boolean isScored;
        private Boolean requireEvidence;
        private BigDecimal itemWeight;
        private String conditionLogic;
        private String inputMode;
    }
}
