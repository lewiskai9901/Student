package com.school.management.interfaces.rest.space;

import com.school.management.application.space.SpaceTypeApplicationService;
import com.school.management.application.space.SpaceTypeApplicationService.*;
import com.school.management.common.result.Result;
import com.school.management.domain.space.model.entity.SpaceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 场所类型管理控制器
 */
@RestController
@RequestMapping("/v6/space-types")
@Tag(name = "场所类型管理", description = "V6 场所类型配置 API")
public class SpaceTypeController {

    private final SpaceTypeApplicationService spaceTypeService;

    public SpaceTypeController(SpaceTypeApplicationService spaceTypeService) {
        this.spaceTypeService = spaceTypeService;
    }

    @GetMapping
    @Operation(summary = "获取所有场所类型")
    @PreAuthorize("hasAuthority('system:space:view')")
    public Result<List<SpaceType>> getAllSpaceTypes() {
        return Result.success(spaceTypeService.getAllSpaceTypes());
    }

    @GetMapping("/enabled")
    @Operation(summary = "获取所有启用的场所类型")
    @PreAuthorize("hasAuthority('system:space:view')")
    public Result<List<SpaceType>> getEnabledSpaceTypes() {
        return Result.success(spaceTypeService.getEnabledSpaceTypes());
    }

    @GetMapping("/tree")
    @Operation(summary = "获取场所类型树")
    @PreAuthorize("hasAuthority('system:space:view')")
    public Result<List<SpaceTypeTreeNode>> getSpaceTypeTree() {
        return Result.success(spaceTypeService.getSpaceTypeTree());
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取场所类型详情")
    @PreAuthorize("hasAuthority('system:space:view')")
    public Result<SpaceType> getSpaceTypeById(@PathVariable Long id) {
        return Result.success(spaceTypeService.getSpaceTypeById(id));
    }

    @GetMapping("/code/{typeCode}")
    @Operation(summary = "根据编码获取场所类型")
    @PreAuthorize("hasAuthority('system:space:view')")
    public Result<SpaceType> getSpaceTypeByCode(@PathVariable String typeCode) {
        return Result.success(spaceTypeService.getSpaceTypeByCode(typeCode));
    }

    @PostMapping
    @Operation(summary = "创建场所类型")
    @PreAuthorize("hasAuthority('system:space:add')")
    public Result<SpaceType> createSpaceType(@RequestBody CreateSpaceTypeRequest request) {
        CreateSpaceTypeCommand command = new CreateSpaceTypeCommand();
        command.setTypeName(request.getTypeName());
        command.setParentTypeCode(request.getParentTypeCode());
        command.setLevelOrder(request.getLevelOrder());
        command.setIcon(request.getIcon());
        command.setDescription(request.getDescription());
        command.setSortOrder(request.getSortOrder());

        return Result.success(spaceTypeService.createSpaceType(command));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新场所类型")
    @PreAuthorize("hasAuthority('system:space:edit')")
    public Result<SpaceType> updateSpaceType(@PathVariable Long id, @RequestBody UpdateSpaceTypeRequest request) {
        UpdateSpaceTypeCommand command = new UpdateSpaceTypeCommand();
        command.setTypeName(request.getTypeName());
        command.setIcon(request.getIcon());
        command.setDescription(request.getDescription());
        command.setSortOrder(request.getSortOrder());

        return Result.success(spaceTypeService.updateSpaceType(id, command));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除场所类型")
    @PreAuthorize("hasAuthority('system:space:delete')")
    public Result<Void> deleteSpaceType(@PathVariable Long id) {
        spaceTypeService.deleteSpaceType(id);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用场所类型")
    @PreAuthorize("hasAuthority('system:space:edit')")
    public Result<SpaceType> enableSpaceType(@PathVariable Long id) {
        return Result.success(spaceTypeService.toggleStatus(id, true));
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用场所类型")
    @PreAuthorize("hasAuthority('system:space:edit')")
    public Result<SpaceType> disableSpaceType(@PathVariable Long id) {
        return Result.success(spaceTypeService.toggleStatus(id, false));
    }

    // ==================== 请求对象（简化版） ====================

    public static class CreateSpaceTypeRequest {
        private String typeName;
        private String parentTypeCode;
        private Integer levelOrder = 0;
        private String icon;
        private String description;
        private Integer sortOrder = 0;

        // Getters and Setters
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        public String getParentTypeCode() { return parentTypeCode; }
        public void setParentTypeCode(String parentTypeCode) { this.parentTypeCode = parentTypeCode; }
        public Integer getLevelOrder() { return levelOrder; }
        public void setLevelOrder(Integer levelOrder) { this.levelOrder = levelOrder; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }

    public static class UpdateSpaceTypeRequest {
        private String typeName;
        private String icon;
        private String description;
        private Integer sortOrder;

        // Getters and Setters
        public String getTypeName() { return typeName; }
        public void setTypeName(String typeName) { this.typeName = typeName; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }
}
