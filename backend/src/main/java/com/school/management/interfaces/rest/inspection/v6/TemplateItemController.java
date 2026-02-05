package com.school.management.interfaces.rest.inspection.v6;

import com.school.management.application.inspection.v6.TemplateItemApplicationService;
import com.school.management.application.inspection.v6.TemplateItemApplicationService.*;
import com.school.management.common.result.Result;
import com.school.management.domain.inspection.model.v6.TemplateCategory;
import com.school.management.domain.inspection.model.v6.TemplateScoreItem;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * V6 模板扣分项管理API
 */
@RestController
@RequestMapping("/v6/template-items")
public class TemplateItemController {

    private final TemplateItemApplicationService templateItemService;

    public TemplateItemController(TemplateItemApplicationService templateItemService) {
        this.templateItemService = templateItemService;
    }

    /**
     * 获取模板的完整扣分项结构（类别+项目）
     */
    @GetMapping("/templates/{templateId}")
    public Result<List<TemplateCategory>> getTemplateItems(@PathVariable Long templateId) {
        List<TemplateCategory> items = templateItemService.getTemplateItems(templateId);
        return Result.success(items);
    }

    /**
     * 获取模板的所有类别
     */
    @GetMapping("/templates/{templateId}/categories")
    public Result<List<TemplateCategory>> getCategories(@PathVariable Long templateId) {
        List<TemplateCategory> categories = templateItemService.getCategories(templateId);
        return Result.success(categories);
    }

    /**
     * 创建类别
     */
    @PostMapping("/categories")
    @PreAuthorize("hasAnyAuthority('inspection:template:edit', 'SUPER_ADMIN')")
    public Result<TemplateCategory> createCategory(@RequestBody CreateCategoryCommand command) {
        TemplateCategory category = templateItemService.createCategory(command);
        return Result.success(category);
    }

    /**
     * 更新类别
     */
    @PutMapping("/categories/{categoryId}")
    @PreAuthorize("hasAnyAuthority('inspection:template:edit', 'SUPER_ADMIN')")
    public Result<Void> updateCategory(@PathVariable Long categoryId,
                                       @RequestBody UpdateCategoryCommand command) {
        templateItemService.updateCategory(categoryId, command);
        return Result.success();
    }

    /**
     * 删除类别
     */
    @DeleteMapping("/categories/{categoryId}")
    @PreAuthorize("hasAnyAuthority('inspection:template:edit', 'SUPER_ADMIN')")
    public Result<Void> deleteCategory(@PathVariable Long categoryId) {
        templateItemService.deleteCategory(categoryId);
        return Result.success();
    }

    /**
     * 获取类别下的扣分项
     */
    @GetMapping("/categories/{categoryId}/items")
    public Result<List<TemplateScoreItem>> getItemsByCategory(@PathVariable Long categoryId) {
        List<TemplateScoreItem> items = templateItemService.getItemsByCategory(categoryId);
        return Result.success(items);
    }

    /**
     * 创建扣分项
     */
    @PostMapping("/items")
    @PreAuthorize("hasAnyAuthority('inspection:template:edit', 'SUPER_ADMIN')")
    public Result<TemplateScoreItem> createItem(@RequestBody CreateItemCommand command) {
        TemplateScoreItem item = templateItemService.createItem(command);
        return Result.success(item);
    }

    /**
     * 更新扣分项
     */
    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAnyAuthority('inspection:template:edit', 'SUPER_ADMIN')")
    public Result<Void> updateItem(@PathVariable Long itemId,
                                   @RequestBody UpdateItemCommand command) {
        templateItemService.updateItem(itemId, command);
        return Result.success();
    }

    /**
     * 删除扣分项
     */
    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyAuthority('inspection:template:edit', 'SUPER_ADMIN')")
    public Result<Void> deleteItem(@PathVariable Long itemId) {
        templateItemService.deleteItem(itemId);
        return Result.success();
    }
}
