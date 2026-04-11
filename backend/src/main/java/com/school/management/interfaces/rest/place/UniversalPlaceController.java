package com.school.management.interfaces.rest.place;

import com.school.management.application.place.UniversalPlaceApplicationService;
import com.school.management.application.place.UniversalPlaceApplicationService.*;
import com.school.management.common.result.Result;
import com.school.management.domain.place.model.entity.UniversalPlaceType;
import com.school.management.domain.place.model.valueobject.PlaceStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import com.school.management.infrastructure.activity.annotation.AuditEvent;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通用空间管理控制器
 */
@RestController
@RequestMapping("/v9/places")
@Tag(name = "空间管理", description = "通用空间管理 API")
@RequiredArgsConstructor
public class UniversalPlaceController {

    private final UniversalPlaceApplicationService placeService;

    // ==================== 查询接口 ====================

    @GetMapping("/tree")
    @Operation(summary = "获取空间树")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<PlaceTreeNode>> getPlaceTree(
            @RequestParam(defaultValue = "0") int maxDepth) {
        return Result.success(placeService.getPlaceTree(maxDepth));
    }

    @GetMapping("/tree/type/{typeCode}")
    @Operation(summary = "获取指定类型的空间树")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<PlaceTreeNode>> getPlaceTreeByType(@PathVariable String typeCode) {
        return Result.success(placeService.getPlaceTreeByType(typeCode));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取空间详情")
    @CasbinAccess(resource = "place", action = "view")
    public Result<PlaceDTO> getPlaceById(@PathVariable Long id) {
        return Result.success(placeService.getPlaceById(id));
    }

    @GetMapping("/code/{placeCode}")
    @Operation(summary = "根据编码获取空间")
    @CasbinAccess(resource = "place", action = "view")
    public Result<PlaceDTO> getPlaceByCode(@PathVariable String placeCode) {
        return Result.success(placeService.getPlaceByCode(placeCode));
    }

    @GetMapping("/{parentId}/children")
    @Operation(summary = "获取子空间列表")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<PlaceDTO>> getChildren(@PathVariable Long parentId) {
        return Result.success(placeService.getChildren(parentId));
    }

    @GetMapping("/roots/children")
    @Operation(summary = "获取根空间的子空间")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<PlaceDTO>> getRootChildren() {
        return Result.success(placeService.getChildren(null));
    }

    @GetMapping("/allowed-child-types")
    @Operation(summary = "获取允许创建的子类型（根空间）")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<UniversalPlaceType>> getAllowedChildTypesForRoot() {
        return Result.success(placeService.getAllowedChildTypes(null));
    }

    @GetMapping("/{parentId}/allowed-child-types")
    @Operation(summary = "获取允许创建的子类型")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<UniversalPlaceType>> getAllowedChildTypes(@PathVariable Long parentId) {
        return Result.success(placeService.getAllowedChildTypes(parentId));
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取统计数据")
    @CasbinAccess(resource = "place", action = "view")
    public Result<PlaceStatistics> getStatistics() {
        return Result.success(placeService.getStatistics());
    }

    // TODO: 待实现 - 获取有效性别
    // @GetMapping("/{id}/effective-gender")
    // @Operation(summary = "获取场所有效性别")
    // @CasbinAccess(resource = "place", action = "view")
    // public Result<String> getEffectiveGender(@PathVariable Long id) {
    //     return Result.success(placeService.getEffectiveGender(id));
    // }

    // ==================== 写入接口 ====================

    @PostMapping
    @Operation(summary = "创建空间")
    @CasbinAccess(resource = "place", action = "add")
    @AuditEvent(module = "place", action = "CREATE", resourceType = "PLACE", label = "创建场所")
    public Result<PlaceDTO> createPlace(@RequestBody CreatePlaceRequest request) {
        CreatePlaceCommand command = new CreatePlaceCommand();
        command.setPlaceCode(request.getPlaceCode());
        command.setPlaceName(request.getPlaceName());
        command.setTypeCode(request.getTypeCode());
        command.setDescription(request.getDescription());
        command.setParentId(request.getParentId());
        command.setStatus(request.getStatus());
        command.setCapacity(request.getCapacity());
        command.setGender(request.getGender());
        command.setOrgUnitId(request.getOrgUnitId());
        command.setResponsibleUserId(request.getResponsibleUserId());
        command.setAttributes(request.getAttributes());

        return Result.success(placeService.createPlace(command));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新空间")
    @CasbinAccess(resource = "place", action = "edit")
    @AuditEvent(module = "place", action = "UPDATE", resourceType = "PLACE", resourceId = "#id", label = "更新场所")
    public Result<PlaceDTO> updatePlace(@PathVariable Long id, @RequestBody UpdatePlaceRequest request) {
        UpdatePlaceCommand command = new UpdatePlaceCommand();
        command.setPlaceCode(request.getPlaceCode());
        command.setPlaceName(request.getPlaceName());
        command.setDescription(request.getDescription());
        command.setStatus(request.getStatus());
        command.setCapacity(request.getCapacity());
        command.setGender(request.getGender());
        command.setOrgUnitId(request.getOrgUnitId());
        command.setClearOrgOverride(request.getClearOrgOverride());  // V23: 支持清除覆盖
        command.setResponsibleUserId(request.getResponsibleUserId());
        command.setAttributes(request.getAttributes());
        command.setReason(request.getReason());  // V20: 审计原因

        return Result.success(placeService.updatePlace(id, command));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更改空间状态")
    @CasbinAccess(resource = "place", action = "edit")
    @AuditEvent(module = "place", action = "UPDATE", resourceType = "PLACE", resourceId = "#id", label = "变更场所状态")
    public Result<PlaceDTO> changeStatus(@PathVariable Long id, @RequestBody ChangeStatusRequest request) {
        PlaceStatus status = PlaceStatus.fromValue(request.getStatus());
        return Result.success(placeService.changeStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除空间")
    @CasbinAccess(resource = "place", action = "delete")
    @AuditEvent(module = "place", action = "DELETE", resourceType = "PLACE", resourceId = "#id", label = "删除场所")
    public Result<Void> deletePlace(@PathVariable Long id) {
        placeService.deletePlace(id);
        return Result.success();
    }

    // ==================== 入住管理 ====================

    @GetMapping("/{id}/occupants")
    @Operation(summary = "获取当前入住列表")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<UniversalPlaceApplicationService.OccupantDTO>> getOccupants(@PathVariable Long id) {
        return Result.success(placeService.getOccupants(id));
    }

    @GetMapping("/{id}/occupant-history")
    @Operation(summary = "获取入住历史")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<UniversalPlaceApplicationService.OccupantDTO>> getOccupantHistory(@PathVariable Long id) {
        return Result.success(placeService.getOccupantHistory(id));
    }

    @PostMapping("/{id}/check-in")
    @Operation(summary = "入住")
    @CasbinAccess(resource = "place", action = "edit")
    public Result<UniversalPlaceApplicationService.OccupantDTO> checkIn(
            @PathVariable Long id, @RequestBody CheckInRequest request) {
        UniversalPlaceApplicationService.CheckInCommand cmd = new UniversalPlaceApplicationService.CheckInCommand();
        cmd.setOccupantType(request.getOccupantType());
        cmd.setOccupantId(request.getOccupantId());
        cmd.setOccupantName(request.getOccupantName());
        cmd.setUsername(request.getUsername());
        cmd.setOrgUnitName(request.getOrgUnitName());
        cmd.setGender(request.getGender());
        cmd.setPositionNo(request.getPositionNo());
        cmd.setRemark(request.getRemark());
        return Result.success(placeService.checkIn(id, cmd));
    }

    @PostMapping("/{id}/batch-check-in")
    @Operation(summary = "批量入住")
    @CasbinAccess(resource = "place", action = "edit")
    public Result<List<UniversalPlaceApplicationService.OccupantDTO>> batchCheckIn(
            @PathVariable Long id, @RequestBody List<CheckInRequest> requests) {
        List<UniversalPlaceApplicationService.CheckInCommand> commands = requests.stream().map(r -> {
            UniversalPlaceApplicationService.CheckInCommand cmd = new UniversalPlaceApplicationService.CheckInCommand();
            cmd.setOccupantType(r.getOccupantType());
            cmd.setOccupantId(r.getOccupantId());
            cmd.setOccupantName(r.getOccupantName());
            cmd.setUsername(r.getUsername());
            cmd.setOrgUnitName(r.getOrgUnitName());
            cmd.setGender(r.getGender());
            cmd.setPositionNo(r.getPositionNo());
            cmd.setRemark(r.getRemark());
            return cmd;
        }).collect(java.util.stream.Collectors.toList());
        return Result.success(placeService.batchCheckIn(id, commands));
    }

    @PostMapping("/{id}/check-out/{recordId}")
    @Operation(summary = "退出")
    @CasbinAccess(resource = "place", action = "edit")
    public Result<Void> checkOut(@PathVariable Long id, @PathVariable Long recordId) {
        placeService.checkOut(id, recordId);
        return Result.success();
    }

    @PostMapping("/{id}/swap-positions")
    @Operation(summary = "交换位置")
    @CasbinAccess(resource = "place", action = "edit")
    public Result<Void> swapPositions(@PathVariable Long id, @RequestBody SwapPositionRequest request) {
        placeService.swapPositions(id, request.getRecordId1(), request.getRecordId2());
        return Result.success();
    }

    @GetMapping("/batch-occupants")
    @Operation(summary = "查询指定场所列表中的所有活跃占用记录")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<UniversalPlaceApplicationService.OccupantWithPlaceDTO>> getOccupantsForPlaces(
            @RequestParam(required = false) List<Long> placeIds,
            @RequestParam(required = false) String occupantType) {
        return Result.success(placeService.getOccupantsForPlaces(placeIds, occupantType));
    }

    @GetMapping("/occupant-history")
    @Operation(summary = "查询占用者的所有占用历史")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<UniversalPlaceApplicationService.OccupantWithPlaceDTO>> getOccupantHistoryByOccupant(
            @RequestParam String occupantType,
            @RequestParam Long occupantId) {
        return Result.success(placeService.getOccupantHistoryByOccupant(occupantType, occupantId));
    }

    // ==================== 请求对象 ====================

    @Data
    public static class CheckInRequest {
        private String occupantType;
        private Long occupantId;
        private String occupantName;
        private String username;
        private String orgUnitName;
        private Integer gender;
        private String positionNo;
        private String remark;
    }

    @Data
    public static class SwapPositionRequest {
        private Long recordId1;
        private Long recordId2;
    }

    @Data
    public static class CreatePlaceRequest {
        private String placeCode;
        private String placeName;
        private String typeCode;
        private String description;
        private Long parentId;
        private Integer status;
        private Integer capacity;
        private String gender;
        private Long orgUnitId;
        private Long responsibleUserId;
        private Map<String, Object> attributes;
    }

    @Data
    public static class UpdatePlaceRequest {
        private String placeCode;
        private String placeName;
        private String description;
        private Integer status;
        private Integer capacity;
        private String gender;
        private Long orgUnitId;

        /**
         * 是否清除组织单元覆盖（V23: NULL继承模型）
         * true: 将 orgUnitId 设为 NULL，恢复从父级继承
         */
        private Boolean clearOrgOverride;

        private Long responsibleUserId;
        private Map<String, Object> attributes;

        /**
         * 更新原因（用于审计日志）
         */
        private String reason;
    }

    @Data
    public static class ChangeStatusRequest {
        private Integer status;
    }
}
