package com.school.management.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.service.CheckDictionaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 检查字典管理控制器 (V3.0)
 *
 * @author Claude
 * @since 2025-11-24
 */
@Slf4j
@RestController
@RequestMapping("/quantification/dictionaries")
@RequiredArgsConstructor
@Tag(name = "检查字典管理", description = "检查类别和检查项字典管理相关接口 (V3.0)")
public class CheckDictionaryController {

    private final CheckDictionaryService dictionaryService;

    // ==================== 检查类别管理 ====================

    /**
     * 创建检查类别
     */
    @PostMapping("/categories")
    @Operation(summary = "创建检查类别", description = "创建新的检查类别")
    @PreAuthorize("hasAuthority('quantification:dictionary:category')")
    public Result<Long> createCheckCategory(@RequestBody Map<String, Object> request) {
        log.info("创建检查类别: {}", request.get("categoryName"));
        Long categoryId = dictionaryService.createCheckCategory(request);
        return Result.success(categoryId);
    }

    /**
     * 更新检查类别
     */
    @PutMapping("/categories/{id}")
    @Operation(summary = "更新检查类别", description = "更新检查类别信息")
    @PreAuthorize("hasAuthority('quantification:dictionary:category')")
    public Result<Void> updateCheckCategory(
            @Parameter(description = "类别ID") @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        log.info("更新检查类别: {}", id);
        dictionaryService.updateCheckCategory(id, request);
        return Result.success();
    }

    /**
     * 删除检查类别
     */
    @DeleteMapping("/categories/{id}")
    @Operation(summary = "删除检查类别", description = "删除检查类别")
    @PreAuthorize("hasAuthority('quantification:dictionary:category')")
    public Result<Void> deleteCheckCategory(@Parameter(description = "类别ID") @PathVariable Long id) {
        log.info("删除检查类别: {}", id);
        dictionaryService.deleteCheckCategory(id);
        return Result.success();
    }

    /**
     * 获取检查类别详情
     */
    @GetMapping("/categories/{id}")
    @Operation(summary = "获取类别详情", description = "获取检查类别详情")
    @PreAuthorize("hasAuthority('quantification:dictionary:category')")
    public Result<Map<String, Object>> getCheckCategoryDetail(@Parameter(description = "类别ID") @PathVariable Long id) {
        log.info("获取检查类别详情: {}", id);
        Map<String, Object> detail = dictionaryService.getCheckCategoryDetail(id);
        return Result.success(detail);
    }

    /**
     * 分页查询检查类别
     */
    @GetMapping("/categories")
    @Operation(summary = "分页查询类别", description = "分页查询检查类别列表")
    @PreAuthorize("hasAuthority('quantification:dictionary:category')")
    public Result<PageResult<Map<String, Object>>> pageCheckCategories(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "类别名称") @RequestParam(required = false) String categoryName,
            @Parameter(description = "类别编码") @RequestParam(required = false) String categoryCode,
            @Parameter(description = "类别类型") @RequestParam(required = false) String categoryType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        log.info("分页查询检查类别: pageNum={}, pageSize={}", pageNum, pageSize);

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        Map<String, Object> query = Map.of(
                "categoryName", categoryName != null ? categoryName : "",
                "categoryCode", categoryCode != null ? categoryCode : "",
                "categoryType", categoryType != null ? categoryType : "",
                "status", status != null ? status : ""
        );

        var result = dictionaryService.pageCheckCategories(page, query);
        return Result.success(PageResult.from(result));
    }

    /**
     * 查询所有启用的检查类别
     */
    @GetMapping("/categories/enabled")
    @Operation(summary = "查询启用的类别", description = "查询所有启用的检查类别")
    public Result<List<Map<String, Object>>> getAllEnabledCategories() {
        log.info("查询所有启用的检查类别");
        List<Map<String, Object>> categories = dictionaryService.getAllEnabledCategories();
        return Result.success(categories);
    }

    /**
     * 根据类型查询类别
     */
    @GetMapping("/categories/by-type/{categoryType}")
    @Operation(summary = "按类型查询类别", description = "根据类别类型查询类别")
    public Result<List<Map<String, Object>>> getCategoriesByType(
            @Parameter(description = "类别类型") @PathVariable String categoryType) {
        log.info("根据类型查询类别: categoryType={}", categoryType);
        List<Map<String, Object>> categories = dictionaryService.getCategoriesByType(categoryType);
        return Result.success(categories);
    }

    // ==================== 检查项管理 ====================

    /**
     * 创建检查项
     */
    @PostMapping("/items")
    @Operation(summary = "创建检查项", description = "创建新的检查项")
    @PreAuthorize("hasAuthority('quantification:dictionary:item')")
    public Result<Long> createCheckItem(@RequestBody Map<String, Object> request) {
        log.info("创建检查项: {}", request.get("itemName"));
        Long itemId = dictionaryService.createCheckItem(request);
        return Result.success(itemId);
    }

    /**
     * 更新检查项
     */
    @PutMapping("/items/{id}")
    @Operation(summary = "更新检查项", description = "更新检查项信息")
    @PreAuthorize("hasAuthority('quantification:dictionary:item')")
    public Result<Void> updateCheckItem(
            @Parameter(description = "检查项ID") @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        log.info("更新检查项: {}", id);
        dictionaryService.updateCheckItem(id, request);
        return Result.success();
    }

    /**
     * 删除检查项
     */
    @DeleteMapping("/items/{id}")
    @Operation(summary = "删除检查项", description = "删除检查项")
    @PreAuthorize("hasAuthority('quantification:dictionary:item')")
    public Result<Void> deleteCheckItem(@Parameter(description = "检查项ID") @PathVariable Long id) {
        log.info("删除检查项: {}", id);
        dictionaryService.deleteCheckItem(id);
        return Result.success();
    }

    /**
     * 批量删除检查项
     */
    @DeleteMapping("/items/batch")
    @Operation(summary = "批量删除检查项", description = "批量删除多个检查项")
    @PreAuthorize("hasAuthority('quantification:dictionary:item')")
    public Result<Void> batchDeleteCheckItems(@RequestBody List<Long> ids) {
        log.info("批量删除检查项: {}", ids);
        dictionaryService.batchDeleteCheckItems(ids);
        return Result.success();
    }

    /**
     * 获取检查项详情
     */
    @GetMapping("/items/{id}")
    @Operation(summary = "获取检查项详情", description = "获取检查项详情")
    @PreAuthorize("hasAuthority('quantification:dictionary:item')")
    public Result<Map<String, Object>> getCheckItemDetail(@Parameter(description = "检查项ID") @PathVariable Long id) {
        log.info("获取检查项详情: {}", id);
        Map<String, Object> detail = dictionaryService.getCheckItemDetail(id);
        return Result.success(detail);
    }

    /**
     * 分页查询检查项
     */
    @GetMapping("/items")
    @Operation(summary = "分页查询检查项", description = "分页查询检查项列表")
    @PreAuthorize("hasAuthority('quantification:dictionary:item')")
    public Result<PageResult<Map<String, Object>>> pageCheckItems(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "检查项名称") @RequestParam(required = false) String itemName,
            @Parameter(description = "检查项编码") @RequestParam(required = false) String itemCode,
            @Parameter(description = "类别ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "扣分模式") @RequestParam(required = false) Integer deductMode,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status) {
        log.info("分页查询检查项: pageNum={}, pageSize={}", pageNum, pageSize);

        Page<Map<String, Object>> page = new Page<>(pageNum, pageSize);
        Map<String, Object> query = Map.of(
                "itemName", itemName != null ? itemName : "",
                "itemCode", itemCode != null ? itemCode : "",
                "categoryId", categoryId != null ? categoryId : "",
                "deductMode", deductMode != null ? deductMode : "",
                "status", status != null ? status : ""
        );

        var result = dictionaryService.pageCheckItems(page, query);
        return Result.success(PageResult.from(result));
    }

    /**
     * 根据类别ID查询检查项
     */
    @GetMapping("/items/by-category/{categoryId}")
    @Operation(summary = "查询类别的检查项", description = "根据类别ID查询检查项")
    public Result<List<Map<String, Object>>> getItemsByCategoryId(
            @Parameter(description = "类别ID") @PathVariable Long categoryId) {
        log.info("查询类别的检查项: categoryId={}", categoryId);
        List<Map<String, Object>> items = dictionaryService.getItemsByCategoryId(categoryId);
        return Result.success(items);
    }

    /**
     * 查询所有启用的检查项
     */
    @GetMapping("/items/enabled")
    @Operation(summary = "查询启用的检查项", description = "查询所有启用的检查项")
    public Result<List<Map<String, Object>>> getAllEnabledItems() {
        log.info("查询所有启用的检查项");
        List<Map<String, Object>> items = dictionaryService.getAllEnabledItems();
        return Result.success(items);
    }

    /**
     * 批量导入检查项
     */
    @PostMapping("/items/batch-import")
    @Operation(summary = "批量导入检查项", description = "批量导入检查项到指定类别")
    @PreAuthorize("hasAuthority('quantification:dictionary:item')")
    public Result<Integer> batchImportCheckItems(
            @Parameter(description = "类别ID") @RequestParam Long categoryId,
            @RequestBody List<Map<String, Object>> items) {
        log.info("批量导入检查项: categoryId={}, count={}", categoryId, items.size());
        int count = dictionaryService.batchImportCheckItems(categoryId, items);
        return Result.success(count);
    }
}
