package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetCategoryService;
import com.school.management.application.asset.command.CreateCategoryCommand;
import com.school.management.application.asset.query.AssetCategoryDTO;
import com.school.management.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资产分类管理接口
 */
@Tag(name = "资产分类管理", description = "资产分类的增删改查")
@RestController
@RequestMapping("/v2/asset/categories")
@RequiredArgsConstructor
public class AssetCategoryController {

    private final AssetCategoryService categoryService;

    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('asset:category:list')")
    public ApiResponse<List<AssetCategoryDTO>> getCategoryTree() {
        return ApiResponse.success(categoryService.getCategoryTree());
    }

    @Operation(summary = "获取所有分类(平铺)")
    @GetMapping
    @PreAuthorize("hasAuthority('asset:category:list')")
    public ApiResponse<List<AssetCategoryDTO>> getAllCategories() {
        return ApiResponse.success(categoryService.getAllCategories());
    }

    @Operation(summary = "获取分类详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:category:list')")
    public ApiResponse<AssetCategoryDTO> getCategory(@PathVariable Long id) {
        return ApiResponse.success(categoryService.getCategory(id));
    }

    @Operation(summary = "创建分类")
    @PostMapping
    @PreAuthorize("hasAuthority('asset:category:manage')")
    public ApiResponse<Long> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .parentId(request.getParentId())
                .categoryCode(request.getCategoryCode())
                .categoryName(request.getCategoryName())
                .categoryType(request.getCategoryType())
                .depreciationYears(request.getDepreciationYears())
                .unit(request.getUnit())
                .sortOrder(request.getSortOrder())
                .remark(request.getRemark())
                .build();
        return ApiResponse.success(categoryService.createCategory(command));
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:category:manage')")
    public ApiResponse<Void> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateCategoryRequest request) {
        CreateCategoryCommand command = CreateCategoryCommand.builder()
                .parentId(request.getParentId())
                .categoryCode(request.getCategoryCode())
                .categoryName(request.getCategoryName())
                .categoryType(request.getCategoryType())
                .depreciationYears(request.getDepreciationYears())
                .unit(request.getUnit())
                .sortOrder(request.getSortOrder())
                .remark(request.getRemark())
                .build();
        categoryService.updateCategory(id, command);
        return ApiResponse.success();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('asset:category:manage')")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ApiResponse.success();
    }

    /**
     * 创建/更新分类请求
     */
    @lombok.Data
    public static class CreateCategoryRequest {

        private Long parentId;

        @jakarta.validation.constraints.NotBlank(message = "分类编码不能为空")
        private String categoryCode;

        @jakarta.validation.constraints.NotBlank(message = "分类名称不能为空")
        private String categoryName;

        private Integer categoryType;

        private Integer depreciationYears;

        private String unit;

        private Integer sortOrder;

        private String remark;
    }
}
