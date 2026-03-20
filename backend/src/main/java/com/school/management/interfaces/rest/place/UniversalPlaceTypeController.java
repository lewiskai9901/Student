package com.school.management.interfaces.rest.place;

import com.school.management.application.place.UniversalPlaceTypeApplicationService;
import com.school.management.application.place.UniversalPlaceTypeApplicationService.*;
import com.school.management.application.shared.TypeTreeBuilder.TypeTreeNode;
import com.school.management.common.result.Result;
import com.school.management.domain.place.model.entity.UniversalPlaceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通用场所类型管理控制器（统一类型系统 Phase 3）
 * API 路径: /place-types
 */
@RestController
@RequestMapping("/place-types")
@Tag(name = "场所类型管理", description = "通用场所类型配置 API")
@RequiredArgsConstructor
public class UniversalPlaceTypeController {

    private final UniversalPlaceTypeApplicationService placeTypeService;

    // ==================== Category 枚举 ====================

    @GetMapping("/categories")
    @Operation(summary = "获取所有内置分类")
    @CasbinAccess(resource = "system:place-type", action = "view")
    public Result<List<PlaceCategoryDTO>> getCategories() {
        return Result.success(placeTypeService.getCategories());
    }

    // ==================== 查询 ====================

    @GetMapping
    @Operation(summary = "获取所有场所类型")
    @CasbinAccess(resource = "system:place-type", action = "view")
    public Result<List<UniversalPlaceType>> getAllPlaceTypes() {
        return Result.success(placeTypeService.getAllPlaceTypes());
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的场所类型")
    @CasbinAccess(resource = "system:place-type", action = "view")
    public Result<List<UniversalPlaceType>> getEnabledPlaceTypes() {
        return Result.success(placeTypeService.getEnabledPlaceTypes());
    }

    @GetMapping("/root")
    @Operation(summary = "获取所有根类型")
    @CasbinAccess(resource = "system:place-type", action = "view")
    public Result<List<UniversalPlaceType>> getRootTypes() {
        return Result.success(placeTypeService.getRootTypes());
    }

    @GetMapping("/tree")
    @Operation(summary = "获取类型树")
    @CasbinAccess(resource = "system:place-type", action = "view")
    public Result<List<TypeTreeNode<UniversalPlaceType>>> getPlaceTypeTree() {
        return Result.success(placeTypeService.getPlaceTypeTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取场所类型详情")
    @CasbinAccess(resource = "system:place-type", action = "view")
    public Result<UniversalPlaceType> getPlaceTypeById(@PathVariable Long id) {
        return Result.success(placeTypeService.getPlaceTypeById(id));
    }

    @GetMapping("/code/{typeCode}")
    @Operation(summary = "根据编码获取场所类型")
    @CasbinAccess(resource = "system:place-type", action = "view")
    public Result<UniversalPlaceType> getPlaceTypeByCode(@PathVariable String typeCode) {
        return Result.success(placeTypeService.getPlaceTypeByCode(typeCode));
    }

    // ==================== 写操作 ====================

    @PostMapping
    @Operation(summary = "创建场所类型")
    @CasbinAccess(resource = "system:place-type", action = "add")
    public Result<UniversalPlaceType> createPlaceType(@RequestBody CreatePlaceTypeRequest request) {
        CreatePlaceTypeCommand command = new CreatePlaceTypeCommand();
        command.setTypeCode(request.getTypeCode());
        command.setTypeName(request.getTypeName());
        command.setCategory(request.getCategory());
        command.setParentTypeCode(request.getParentTypeCode());
        command.setDescription(request.getDescription());
        command.setFeatures(request.getFeatures());
        command.setMetadataSchema(request.getMetadataSchema());
        command.setAllowedChildTypeCodes(request.getAllowedChildTypeCodes());
        command.setMaxDepth(request.getMaxDepth());
        command.setDefaultUserTypeCodes(request.getDefaultUserTypeCodes());
        command.setDefaultOrgTypeCodes(request.getDefaultOrgTypeCodes());
        command.setCapacityUnit(request.getCapacityUnit());
        command.setDefaultCapacity(request.getDefaultCapacity());
        command.setSortOrder(request.getSortOrder());

        return Result.success(placeTypeService.createPlaceType(command));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新场所类型")
    @CasbinAccess(resource = "system:place-type", action = "edit")
    public Result<UniversalPlaceType> updatePlaceType(@PathVariable Long id, @RequestBody UpdatePlaceTypeRequest request) {
        UpdatePlaceTypeCommand command = new UpdatePlaceTypeCommand();
        command.setTypeName(request.getTypeName());
        command.setCategory(request.getCategory());
        command.setDescription(request.getDescription());
        command.setFeatures(request.getFeatures());
        command.setMetadataSchema(request.getMetadataSchema());
        command.setAllowedChildTypeCodes(request.getAllowedChildTypeCodes());
        command.setMaxDepth(request.getMaxDepth());
        command.setDefaultUserTypeCodes(request.getDefaultUserTypeCodes());
        command.setDefaultOrgTypeCodes(request.getDefaultOrgTypeCodes());
        command.setCapacityUnit(request.getCapacityUnit());
        command.setDefaultCapacity(request.getDefaultCapacity());
        command.setSortOrder(request.getSortOrder());

        return Result.success(placeTypeService.updatePlaceType(id, command));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除场所类型")
    @CasbinAccess(resource = "system:place-type", action = "delete")
    public Result<Void> deletePlaceType(@PathVariable Long id) {
        placeTypeService.deletePlaceType(id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用场所类型")
    @CasbinAccess(resource = "system:place-type", action = "edit")
    public Result<UniversalPlaceType> enablePlaceType(@PathVariable Long id) {
        return Result.success(placeTypeService.toggleStatus(id, true));
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用场所类型")
    @CasbinAccess(resource = "system:place-type", action = "edit")
    public Result<UniversalPlaceType> disablePlaceType(@PathVariable Long id) {
        return Result.success(placeTypeService.toggleStatus(id, false));
    }

    // ==================== 请求对象 ====================

    @Data
    public static class CreatePlaceTypeRequest {
        private String typeCode;
        private String typeName;
        private String category;
        private String parentTypeCode;
        private String description;
        private Map<String, Boolean> features;
        private String metadataSchema;
        private List<String> allowedChildTypeCodes;
        private Integer maxDepth;
        private List<String> defaultUserTypeCodes;
        private List<String> defaultOrgTypeCodes;
        private String capacityUnit;
        private Integer defaultCapacity;
        private Integer sortOrder = 0;
    }

    @Data
    public static class UpdatePlaceTypeRequest {
        private String typeName;
        private String category;
        private String description;
        private Map<String, Boolean> features;
        private String metadataSchema;
        private List<String> allowedChildTypeCodes;
        private Integer maxDepth;
        private List<String> defaultUserTypeCodes;
        private List<String> defaultOrgTypeCodes;
        private String capacityUnit;
        private Integer defaultCapacity;
        private Integer sortOrder;
    }
}
