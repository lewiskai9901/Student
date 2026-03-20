package com.school.management.interfaces.rest.place;

import com.school.management.application.place.PlaceCategoryApplicationService;
import com.school.management.application.place.command.CreatePlaceCategoryCommand;
import com.school.management.application.place.command.UpdatePlaceCategoryCommand;
import com.school.management.application.place.query.PlaceCategoryDTO;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 空间分类管理控制器
 * V10 版本 - 固定层级 + 可配置分类
 */
@RestController
@RequestMapping("/place-categories")
@Tag(name = "空间分类管理", description = "V10 空间分类配置 API")
@RequiredArgsConstructor
public class PlaceCategoryController {

    private final PlaceCategoryApplicationService categoryService;

    @GetMapping
    @Operation(summary = "获取所有空间分类")
    @CasbinAccess(resource = "system:place-category", action = "view")
    public Result<List<PlaceCategoryDTO>> getAllCategories() {
        return Result.success(categoryService.getAllCategories());
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的空间分类")
    @CasbinAccess(resource = "system:place-category", action = "view")
    public Result<List<PlaceCategoryDTO>> getEnabledCategories() {
        return Result.success(categoryService.getEnabledCategories());
    }

    @GetMapping("/level/{level}")
    @Operation(summary = "按层级获取空间分类")
    @CasbinAccess(resource = "system:place-category", action = "view")
    public Result<List<PlaceCategoryDTO>> getCategoriesByLevel(@PathVariable String level) {
        return Result.success(categoryService.getCategoriesByLevel(level.toUpperCase()));
    }

    @GetMapping("/level/{level}/enabled")
    @Operation(summary = "按层级获取启用的空间分类")
    @CasbinAccess(resource = "system:place-category", action = "view")
    public Result<List<PlaceCategoryDTO>> getEnabledCategoriesByLevel(@PathVariable String level) {
        return Result.success(categoryService.getEnabledCategoriesByLevel(level.toUpperCase()));
    }

    @GetMapping("/building")
    @Operation(summary = "获取楼栋分类")
    @CasbinAccess(resource = "system:place-category", action = "view")
    public Result<List<PlaceCategoryDTO>> getBuildingCategories() {
        return Result.success(categoryService.getBuildingCategories());
    }

    @GetMapping("/room")
    @Operation(summary = "获取房间分类")
    @CasbinAccess(resource = "system:place-category", action = "view")
    public Result<List<PlaceCategoryDTO>> getRoomCategories() {
        return Result.success(categoryService.getRoomCategories());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取空间分类详情")
    @CasbinAccess(resource = "system:place-category", action = "view")
    public Result<PlaceCategoryDTO> getCategory(@PathVariable Long id) {
        return Result.success(categoryService.getCategory(id));
    }

    @PostMapping
    @Operation(summary = "创建空间分类")
    @CasbinAccess(resource = "system:place-category", action = "add")
    public Result<PlaceCategoryDTO> createCategory(@RequestBody CreatePlaceCategoryCommand command) {
        Long operatorId = SecurityUtils.requireCurrentUserId();
        return Result.success(categoryService.createCategory(command, operatorId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新空间分类")
    @CasbinAccess(resource = "system:place-category", action = "edit")
    public Result<PlaceCategoryDTO> updateCategory(@PathVariable Long id,
                                                   @RequestBody UpdatePlaceCategoryCommand command) {
        command.setId(id);
        Long operatorId = SecurityUtils.requireCurrentUserId();
        return Result.success(categoryService.updateCategory(command, operatorId));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用空间分类")
    @CasbinAccess(resource = "system:place-category", action = "edit")
    public Result<Void> enableCategory(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireCurrentUserId();
        categoryService.enableCategory(id, operatorId);
        return Result.success();
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "停用空间分类")
    @CasbinAccess(resource = "system:place-category", action = "edit")
    public Result<Void> disableCategory(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireCurrentUserId();
        categoryService.disableCategory(id, operatorId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除空间分类")
    @CasbinAccess(resource = "system:place-category", action = "delete")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        Long operatorId = SecurityUtils.requireCurrentUserId();
        categoryService.deleteCategory(id, operatorId);
        return Result.success();
    }
}
