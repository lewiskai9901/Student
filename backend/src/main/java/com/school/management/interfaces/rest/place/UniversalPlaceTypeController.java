package com.school.management.interfaces.rest.space;

import com.school.management.application.space.UniversalSpaceTypeApplicationService;
import com.school.management.application.space.UniversalSpaceTypeApplicationService.*;
import com.school.management.common.result.Result;
import com.school.management.domain.space.model.entity.UniversalSpaceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通用空间类型管理控制器
 */
@RestController
@RequestMapping("/v9/space-types")
@Tag(name = "空间类型管理", description = "通用空间类型配置 API")
@RequiredArgsConstructor
public class UniversalSpaceTypeController {

    private final UniversalSpaceTypeApplicationService spaceTypeService;

    @GetMapping
    @Operation(summary = "获取所有空间类型")
    @PreAuthorize("hasAuthority('system:space-type:view')")
    public Result<List<UniversalSpaceType>> getAllSpaceTypes() {
        return Result.success(spaceTypeService.getAllSpaceTypes());
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的空间类型")
    @PreAuthorize("hasAuthority('system:space-type:view')")
    public Result<List<UniversalSpaceType>> getEnabledSpaceTypes() {
        return Result.success(spaceTypeService.getEnabledSpaceTypes());
    }

    @GetMapping("/root")
    @Operation(summary = "获取所有根类型")
    @PreAuthorize("hasAuthority('system:space-type:view')")
    public Result<List<UniversalSpaceType>> getRootTypes() {
        return Result.success(spaceTypeService.getRootTypes());
    }

    @GetMapping("/tree")
    @Operation(summary = "获取空间类型树")
    @PreAuthorize("hasAuthority('system:space-type:view')")
    public Result<List<SpaceTypeTreeNode>> getSpaceTypeTree() {
        return Result.success(spaceTypeService.getSpaceTypeTree());
    }

    @GetMapping("/{typeCode}/children")
    @Operation(summary = "获取允许的子类型")
    @PreAuthorize("hasAuthority('system:space-type:view')")
    public Result<List<UniversalSpaceType>> getAllowedChildTypes(@PathVariable String typeCode) {
        return Result.success(spaceTypeService.getAllowedChildTypes(typeCode));
    }

    @GetMapping("/id/{id}")
    @Operation(summary = "根据ID获取空间类型")
    @PreAuthorize("hasAuthority('system:space-type:view')")
    public Result<UniversalSpaceType> getSpaceTypeById(@PathVariable Long id) {
        return Result.success(spaceTypeService.getSpaceTypeById(id));
    }

    @GetMapping("/code/{typeCode}")
    @Operation(summary = "根据编码获取空间类型")
    @PreAuthorize("hasAuthority('system:space-type:view')")
    public Result<UniversalSpaceType> getSpaceTypeByCode(@PathVariable String typeCode) {
        return Result.success(spaceTypeService.getSpaceTypeByCode(typeCode));
    }

    @PostMapping
    @Operation(summary = "创建空间类型")
    @PreAuthorize("hasAuthority('system:space-type:add')")
    public Result<UniversalSpaceType> createSpaceType(@RequestBody CreateSpaceTypeRequest request) {
        CreateSpaceTypeCommand command = new CreateSpaceTypeCommand();
        command.setTypeName(request.getTypeName());
        command.setIcon(request.getIcon());
        command.setDescription(request.getDescription());
        command.setSortOrder(request.getSortOrder());
        command.setRootType(request.isRootType());
        command.setAllowedChildTypes(request.getAllowedChildTypes());
        command.setHasCapacity(request.isHasCapacity());
        command.setBookable(request.isBookable());
        command.setAssignable(request.isAssignable());
        command.setOccupiable(request.isOccupiable());
        command.setCapacityUnit(request.getCapacityUnit());
        command.setDefaultCapacity(request.getDefaultCapacity());

        return Result.success(spaceTypeService.createSpaceType(command));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新空间类型")
    @PreAuthorize("hasAuthority('system:space-type:edit')")
    public Result<UniversalSpaceType> updateSpaceType(@PathVariable Long id, @RequestBody UpdateSpaceTypeRequest request) {
        UpdateSpaceTypeCommand command = new UpdateSpaceTypeCommand();
        command.setTypeName(request.getTypeName());
        command.setIcon(request.getIcon());
        command.setDescription(request.getDescription());
        command.setSortOrder(request.getSortOrder());
        command.setAllowedChildTypes(request.getAllowedChildTypes());
        command.setHasCapacity(request.getHasCapacity());
        command.setBookable(request.getBookable());
        command.setAssignable(request.getAssignable());
        command.setOccupiable(request.getOccupiable());
        command.setCapacityUnit(request.getCapacityUnit());
        command.setDefaultCapacity(request.getDefaultCapacity());

        return Result.success(spaceTypeService.updateSpaceType(id, command));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除空间类型")
    @PreAuthorize("hasAuthority('system:space-type:delete')")
    public Result<Void> deleteSpaceType(@PathVariable Long id) {
        spaceTypeService.deleteSpaceType(id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用空间类型")
    @PreAuthorize("hasAuthority('system:space-type:edit')")
    public Result<UniversalSpaceType> enableSpaceType(@PathVariable Long id) {
        return Result.success(spaceTypeService.toggleStatus(id, true));
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用空间类型")
    @PreAuthorize("hasAuthority('system:space-type:edit')")
    public Result<UniversalSpaceType> disableSpaceType(@PathVariable Long id) {
        return Result.success(spaceTypeService.toggleStatus(id, false));
    }

    // ==================== 请求对象 ====================

    @Data
    public static class CreateSpaceTypeRequest {
        private String typeName;
        private String icon;
        private String description;
        private Integer sortOrder;
        private boolean rootType;
        private List<String> allowedChildTypes;
        private boolean hasCapacity;
        private boolean bookable;
        private boolean assignable;
        private boolean occupiable;
        private String capacityUnit;
        private Integer defaultCapacity;
    }

    @Data
    public static class UpdateSpaceTypeRequest {
        private String typeName;
        private String icon;
        private String description;
        private Integer sortOrder;
        private List<String> allowedChildTypes;
        private Boolean hasCapacity;
        private Boolean bookable;
        private Boolean assignable;
        private Boolean occupiable;
        private String capacityUnit;
        private Integer defaultCapacity;
    }
}
