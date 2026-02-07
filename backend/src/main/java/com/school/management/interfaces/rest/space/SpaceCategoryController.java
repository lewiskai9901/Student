package com.school.management.interfaces.rest.space;

import com.school.management.application.space.SpaceCategoryApplicationService;
import com.school.management.application.space.command.CreateSpaceCategoryCommand;
import com.school.management.application.space.command.UpdateSpaceCategoryCommand;
import com.school.management.application.space.query.SpaceCategoryDTO;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 空间分类管理控制器
 * V10 版本 - 固定层级 + 可配置分类
 */
@RestController
@RequestMapping("/space-categories")
@Tag(name = "空间分类管理", description = "V10 空间分类配置 API")
@RequiredArgsConstructor
public class SpaceCategoryController {

    private final SpaceCategoryApplicationService categoryService;

    @GetMapping
    @Operation(summary = "获取所有空间分类")
    @PreAuthorize("hasAuthority('system:space-category:view') or hasAuthority('system:space:view')")
    public Result<List<SpaceCategoryDTO>> getAllCategories() {
        return Result.success(categoryService.getAllCategories());
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的空间分类")
    @PreAuthorize("hasAuthority('system:space-category:view') or hasAuthority('system:space:view')")
    public Result<List<SpaceCategoryDTO>> getEnabledCategories() {
        return Result.success(categoryService.getEnabledCategories());
    }

    @GetMapping("/level/{level}")
    @Operation(summary = "按层级获取空间分类")
    @PreAuthorize("hasAuthority('system:space-category:view') or hasAuthority('system:space:view')")
    public Result<List<SpaceCategoryDTO>> getCategoriesByLevel(@PathVariable String level) {
        return Result.success(categoryService.getCategoriesByLevel(level.toUpperCase()));
    }

    @GetMapping("/level/{level}/enabled")
    @Operation(summary = "按层级获取启用的空间分类")
    @PreAuthorize("hasAuthority('system:space-category:view') or hasAuthority('system:space:view')")
    public Result<List<SpaceCategoryDTO>> getEnabledCategoriesByLevel(@PathVariable String level) {
        return Result.success(categoryService.getEnabledCategoriesByLevel(level.toUpperCase()));
    }

    @GetMapping("/building")
    @Operation(summary = "获取楼栋分类")
    @PreAuthorize("hasAuthority('system:space-category:view') or hasAuthority('system:space:view')")
    public Result<List<SpaceCategoryDTO>> getBuildingCategories() {
        return Result.success(categoryService.getBuildingCategories());
    }

    @GetMapping("/room")
    @Operation(summary = "获取房间分类")
    @PreAuthorize("hasAuthority('system:space-category:view') or hasAuthority('system:space:view')")
    public Result<List<SpaceCategoryDTO>> getRoomCategories() {
        return Result.success(categoryService.getRoomCategories());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取空间分类详情")
    @PreAuthorize("hasAuthority('system:space-category:view') or hasAuthority('system:space:view')")
    public Result<SpaceCategoryDTO> getCategory(@PathVariable Long id) {
        return Result.success(categoryService.getCategory(id));
    }

    @PostMapping
    @Operation(summary = "创建空间分类")
    @PreAuthorize("hasAuthority('system:space-category:add')")
    public Result<SpaceCategoryDTO> createCategory(@RequestBody CreateSpaceCategoryCommand command) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        return Result.success(categoryService.createCategory(command, operatorId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新空间分类")
    @PreAuthorize("hasAuthority('system:space-category:edit')")
    public Result<SpaceCategoryDTO> updateCategory(@PathVariable Long id,
                                                   @RequestBody UpdateSpaceCategoryCommand command) {
        command.setId(id);
        Long operatorId = SecurityUtils.getCurrentUserId();
        return Result.success(categoryService.updateCategory(command, operatorId));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "启用空间分类")
    @PreAuthorize("hasAuthority('system:space-category:edit')")
    public Result<Void> enableCategory(@PathVariable Long id) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        categoryService.enableCategory(id, operatorId);
        return Result.success();
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "停用空间分类")
    @PreAuthorize("hasAuthority('system:space-category:edit')")
    public Result<Void> disableCategory(@PathVariable Long id) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        categoryService.disableCategory(id, operatorId);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除空间分类")
    @PreAuthorize("hasAuthority('system:space-category:delete')")
    public Result<Void> deleteCategory(@PathVariable Long id) {
        Long operatorId = SecurityUtils.getCurrentUserId();
        categoryService.deleteCategory(id, operatorId);
        return Result.success();
    }
}
