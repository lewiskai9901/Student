package com.school.management.interfaces.rest.space;

import com.school.management.application.space.UniversalSpaceApplicationService;
import com.school.management.application.space.UniversalSpaceApplicationService.*;
import com.school.management.common.result.Result;
import com.school.management.domain.space.model.entity.UniversalSpaceType;
import com.school.management.domain.space.model.valueobject.SpaceStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通用空间管理控制器
 */
@RestController
@RequestMapping("/v9/spaces")
@Tag(name = "空间管理", description = "通用空间管理 API")
@RequiredArgsConstructor
public class UniversalSpaceController {

    private final UniversalSpaceApplicationService spaceService;

    // ==================== 查询接口 ====================

    @GetMapping("/tree")
    @Operation(summary = "获取空间树")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<List<SpaceTreeNode>> getSpaceTree() {
        return Result.success(spaceService.getSpaceTree());
    }

    @GetMapping("/tree/type/{typeCode}")
    @Operation(summary = "获取指定类型的空间树")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<List<SpaceTreeNode>> getSpaceTreeByType(@PathVariable String typeCode) {
        return Result.success(spaceService.getSpaceTreeByType(typeCode));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取空间详情")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<SpaceDTO> getSpaceById(@PathVariable Long id) {
        return Result.success(spaceService.getSpaceById(id));
    }

    @GetMapping("/code/{spaceCode}")
    @Operation(summary = "根据编码获取空间")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<SpaceDTO> getSpaceByCode(@PathVariable String spaceCode) {
        return Result.success(spaceService.getSpaceByCode(spaceCode));
    }

    @GetMapping("/{parentId}/children")
    @Operation(summary = "获取子空间列表")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<List<SpaceDTO>> getChildren(@PathVariable Long parentId) {
        return Result.success(spaceService.getChildren(parentId));
    }

    @GetMapping("/roots/children")
    @Operation(summary = "获取根空间的子空间")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<List<SpaceDTO>> getRootChildren() {
        return Result.success(spaceService.getChildren(null));
    }

    @GetMapping("/allowed-child-types")
    @Operation(summary = "获取允许创建的子类型（根空间）")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<List<UniversalSpaceType>> getAllowedChildTypesForRoot() {
        return Result.success(spaceService.getAllowedChildTypes(null));
    }

    @GetMapping("/{parentId}/allowed-child-types")
    @Operation(summary = "获取允许创建的子类型")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<List<UniversalSpaceType>> getAllowedChildTypes(@PathVariable Long parentId) {
        return Result.success(spaceService.getAllowedChildTypes(parentId));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取统计数据")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<SpaceStatistics> getStatistics() {
        return Result.success(spaceService.getStatistics());
    }

    // ==================== 写入接口 ====================

    @PostMapping
    @Operation(summary = "创建空间")
    @PreAuthorize("hasAuthority('space:create')")
    public Result<SpaceDTO> createSpace(@RequestBody CreateSpaceRequest request) {
        CreateSpaceCommand command = new CreateSpaceCommand();
        command.setSpaceName(request.getSpaceName());
        command.setTypeCode(request.getTypeCode());
        command.setDescription(request.getDescription());
        command.setParentId(request.getParentId());
        command.setCapacity(request.getCapacity());
        command.setOrgUnitId(request.getOrgUnitId());
        command.setResponsibleUserId(request.getResponsibleUserId());
        command.setAttributes(request.getAttributes());

        return Result.success(spaceService.createSpace(command));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新空间")
    @PreAuthorize("hasAuthority('space:update')")
    public Result<SpaceDTO> updateSpace(@PathVariable Long id, @RequestBody UpdateSpaceRequest request) {
        UpdateSpaceCommand command = new UpdateSpaceCommand();
        command.setSpaceName(request.getSpaceName());
        command.setDescription(request.getDescription());
        command.setCapacity(request.getCapacity());
        command.setOrgUnitId(request.getOrgUnitId());
        command.setResponsibleUserId(request.getResponsibleUserId());
        command.setAttributes(request.getAttributes());

        return Result.success(spaceService.updateSpace(id, command));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更改空间状态")
    @PreAuthorize("hasAuthority('space:update')")
    public Result<SpaceDTO> changeStatus(@PathVariable Long id, @RequestBody ChangeStatusRequest request) {
        SpaceStatus status = SpaceStatus.fromValue(request.getStatus());
        return Result.success(spaceService.changeStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除空间")
    @PreAuthorize("hasAuthority('space:delete')")
    public Result<Void> deleteSpace(@PathVariable Long id) {
        spaceService.deleteSpace(id);
        return Result.success();
    }

    // ==================== 请求对象 ====================

    @Data
    public static class CreateSpaceRequest {
        private String spaceName;
        private String typeCode;
        private String description;
        private Long parentId;
        private Integer capacity;
        private Long orgUnitId;
        private Long responsibleUserId;
        private Map<String, Object> attributes;
    }

    @Data
    public static class UpdateSpaceRequest {
        private String spaceName;
        private String description;
        private Integer capacity;
        private Long orgUnitId;
        private Long responsibleUserId;
        private Map<String, Object> attributes;
    }

    @Data
    public static class ChangeStatusRequest {
        private Integer status;
    }
}
