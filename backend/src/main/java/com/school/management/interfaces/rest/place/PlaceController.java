package com.school.management.interfaces.rest.place;

import com.school.management.application.place.PlaceApplicationService;
import com.school.management.application.place.command.CheckInCommand;
import com.school.management.application.place.command.CreatePlaceCommand;
import com.school.management.application.place.command.UpdatePlaceCommand;
import com.school.management.application.place.query.PlaceDTO;
import com.school.management.application.place.query.PlaceOccupantDTO;
import com.school.management.application.place.query.PlaceQueryCriteria;
import com.school.management.common.result.Result;
import com.school.management.domain.place.model.valueobject.BuildingType;
import com.school.management.domain.place.model.valueobject.RoomType;
import com.school.management.domain.place.model.valueobject.PlaceStatus;
import com.school.management.domain.place.model.valueobject.PlaceType;
import com.school.management.interfaces.rest.place.dto.BatchAssignClassRequest;
import com.school.management.interfaces.rest.place.dto.BatchAssignOrgUnitRequest;
import com.school.management.interfaces.rest.place.dto.CheckInRequest;
import com.school.management.interfaces.rest.place.dto.CreatePlaceRequest;
import com.school.management.interfaces.rest.place.dto.UpdatePlaceRequest;
import com.school.management.domain.place.model.valueobject.GenderType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.school.management.infrastructure.casbin.CasbinAccess;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 场所管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
@Tag(name = "场所管理", description = "统一场所管理API，支持多级场所树结构")
public class PlaceController {

    private final PlaceApplicationService placeService;

    // ========== 基础CRUD ==========

    @PostMapping
    @Operation(summary = "创建场所")
    @CasbinAccess(resource = "place", action = "add")
    public Result<Long> createPlace(@Valid @RequestBody CreatePlaceRequest request) {
        CreatePlaceCommand command = new CreatePlaceCommand();
        command.setPlaceType(request.getPlaceType());
        command.setPlaceCode(request.getPlaceCode());
        command.setPlaceName(request.getPlaceName());
        command.setRoomType(request.getRoomType());
        command.setBuildingType(request.getBuildingType());
        command.setBuildingNo(request.getBuildingNo());
        command.setRoomNo(request.getRoomNo());
        command.setParentId(request.getParentId());
        command.setFloorNumber(request.getFloorNumber());
        command.setCapacity(request.getCapacity());
        command.setOrgUnitId(request.getOrgUnitId());
        command.setOrgUnitId(request.getOrgUnitId());
        command.setResponsibleUserId(request.getResponsibleUserId());
        command.setGenderType(request.getGenderType());
        command.setDescription(request.getDescription());
        command.setAttributes(request.getAttributes());

        Long id = placeService.createPlace(command);
        return Result.success(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新场所")
    @CasbinAccess(resource = "place", action = "edit")
    public Result<Void> updatePlace(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePlaceRequest request) {
        UpdatePlaceCommand command = new UpdatePlaceCommand();
        command.setId(id);
        command.setPlaceName(request.getPlaceName());
        command.setDescription(request.getDescription());
        command.setBuildingNo(request.getBuildingNo());
        command.setRoomNo(request.getRoomNo());
        command.setCapacity(request.getCapacity());
        command.setOrgUnitId(request.getOrgUnitId());
        command.setOrgUnitId(request.getOrgUnitId());
        command.setResponsibleUserId(request.getResponsibleUserId());
        command.setGenderType(request.getGenderType());
        command.setAttributes(request.getAttributes());

        placeService.updatePlace(command);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除场所")
    @CasbinAccess(resource = "place", action = "delete")
    public Result<Void> deletePlace(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force) {
        placeService.deletePlace(id, force);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取场所详情")
    @CasbinAccess(resource = "place", action = "view")
    public Result<PlaceDTO> getById(@PathVariable Long id) {
        return Result.success(placeService.getById(id));
    }

    // ========== 状态管理 ==========

    @PutMapping("/{id}/status")
    @Operation(summary = "变更场所状态")
    @CasbinAccess(resource = "place", action = "edit")
    public Result<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam PlaceStatus status) {
        placeService.changeStatus(id, status);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用场所")
    @CasbinAccess(resource = "place", action = "edit")
    public Result<Void> enable(@PathVariable Long id) {
        placeService.changeStatus(id, PlaceStatus.NORMAL);
        return Result.success();
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用场所")
    @CasbinAccess(resource = "place", action = "edit")
    public Result<Void> disable(@PathVariable Long id) {
        placeService.changeStatus(id, PlaceStatus.DISABLED);
        return Result.success();
    }

    @PutMapping("/{id}/maintenance")
    @Operation(summary = "设置维护状态")
    @CasbinAccess(resource = "place", action = "edit")
    public Result<Void> startMaintenance(@PathVariable Long id) {
        placeService.changeStatus(id, PlaceStatus.MAINTENANCE);
        return Result.success();
    }

    // ========== 入住/退出 ==========

    @PostMapping("/{id}/check-in")
    @Operation(summary = "入住")
    @CasbinAccess(resource = "place", action = "occupancy")
    public Result<Long> checkIn(
            @PathVariable Long id,
            @Valid @RequestBody CheckInRequest request) {
        CheckInCommand command = new CheckInCommand();
        command.setPlaceId(id);
        command.setOccupantType(request.getOccupantType());
        command.setOccupantId(request.getOccupantId());
        command.setPositionNo(request.getPositionNo());
        command.setRemark(request.getRemark());

        Long occupantRecordId = placeService.checkIn(command);
        return Result.success(occupantRecordId);
    }

    @PostMapping("/{placeId}/check-out/{occupantRecordId}")
    @Operation(summary = "退出")
    @CasbinAccess(resource = "place", action = "occupancy")
    public Result<Void> checkOut(
            @PathVariable Long placeId,
            @PathVariable Long occupantRecordId) {
        placeService.checkOut(placeId, occupantRecordId);
        return Result.success();
    }

    @GetMapping("/{id}/occupants")
    @Operation(summary = "获取场所占用者列表")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<PlaceOccupantDTO>> getOccupants(@PathVariable Long id) {
        return Result.success(placeService.getOccupants(id));
    }

    @GetMapping("/{id}/occupant-history")
    @Operation(summary = "获取场所占用历史")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<PlaceOccupantDTO>> getOccupantHistory(@PathVariable Long id) {
        return Result.success(placeService.getOccupantHistory(id));
    }

    // ========== 批量操作 ==========

    @PostMapping("/batch/assign-org-unit")
    @Operation(summary = "批量分配组织单元")
    @CasbinAccess(resource = "place", action = "edit")
    public Result<Void> batchAssignOrgUnit(@Valid @RequestBody BatchAssignOrgUnitRequest request) {
        placeService.batchAssignOrgUnit(request.getPlaceIds(), request.getOrgUnitId());
        return Result.success();
    }

    @PostMapping("/batch/assign-class")
    @Operation(summary = "批量分配班级")
    @CasbinAccess(resource = "place", action = "edit")
    public Result<Void> batchAssignClass(@Valid @RequestBody BatchAssignClassRequest request) {
        placeService.batchAssignClass(request.getPlaceIds(), request.getOrgUnitId());
        return Result.success();
    }

    // ========== 班级分配管理 ==========

    @PutMapping("/{id}/class")
    @Operation(summary = "设置场所归属班级")
    @CasbinAccess(resource = "place", action = "edit")
    public Result<Void> assignClass(
            @PathVariable Long id,
            @RequestParam Long orgUnitId) {
        placeService.batchAssignClass(List.of(id), orgUnitId);
        return Result.success();
    }

    @DeleteMapping("/{id}/class")
    @Operation(summary = "取消场所班级分配")
    @CasbinAccess(resource = "place", action = "edit")
    public Result<Void> unassignClass(@PathVariable Long id) {
        placeService.unassignClass(id);
        return Result.success();
    }

    @PutMapping("/{id}/gender-type")
    @Operation(summary = "设置性别限制")
    @CasbinAccess(resource = "place", action = "edit")
    public Result<Void> setGenderType(
            @PathVariable Long id,
            @RequestParam Integer genderType) {
        placeService.setGenderRestriction(id, genderType);
        return Result.success();
    }

    // ========== 树形查询 ==========

    @GetMapping("/tree")
    @Operation(summary = "获取场所树", description = "获取完整的场所树结构，可按楼宇类型筛选")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<PlaceDTO>> getTree(
            @Parameter(description = "楼宇类型筛选")
            @RequestParam(required = false) BuildingType buildingType,
            @Parameter(description = "是否包含统计信息")
            @RequestParam(defaultValue = "false") boolean includeStatistics) {
        return Result.success(placeService.getTree(buildingType, includeStatistics));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "获取子节点")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<PlaceDTO>> getChildren(@PathVariable Long id) {
        return Result.success(placeService.getChildren(id));
    }

    @GetMapping("/{id}/ancestors")
    @Operation(summary = "获取祖先链")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<PlaceDTO>> getAncestors(@PathVariable Long id) {
        return Result.success(placeService.getAncestors(id));
    }

    // ========== 列表查询 ==========

    @GetMapping("/buildings")
    @Operation(summary = "获取楼宇列表")
    @CasbinAccess(resource = "place", action = "view")
    public Result<List<PlaceDTO>> getBuildings(
            @RequestParam(required = false) BuildingType buildingType,
            @RequestParam(required = false) PlaceStatus status) {
        return Result.success(placeService.getBuildings(buildingType, status));
    }

    @GetMapping
    @Operation(summary = "分页查询场所")
    @CasbinAccess(resource = "place", action = "view")
    public Result<Map<String, Object>> query(
            @RequestParam(required = false) PlaceType placeType,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) BuildingType buildingType,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Integer floorNumber,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) PlaceStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        PlaceQueryCriteria criteria = new PlaceQueryCriteria();
        criteria.setPlaceType(placeType);
        criteria.setRoomType(roomType);
        criteria.setBuildingType(buildingType);
        criteria.setBuildingId(buildingId);
        criteria.setFloorNumber(floorNumber);
        criteria.setOrgUnitId(orgUnitId);
        criteria.setStatus(status);
        criteria.setKeyword(keyword);
        criteria.setPage(page);
        criteria.setPageSize(pageSize);

        List<PlaceDTO> list = placeService.query(criteria);
        long total = placeService.count(criteria);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    // ========== 枚举查询 ==========

    @GetMapping("/enums/place-types")
    @Operation(summary = "获取场所类型枚举")
    public Result<List<Map<String, Object>>> getPlaceTypes() {
        List<Map<String, Object>> types = new java.util.ArrayList<>();
        for (PlaceType type : PlaceType.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", type.name());
            item.put("name", type.getDescription());
            item.put("level", type.getLevel());
            types.add(item);
        }
        return Result.success(types);
    }

    @GetMapping("/enums/room-types")
    @Operation(summary = "获取房间类型枚举")
    public Result<List<Map<String, Object>>> getRoomTypes() {
        List<Map<String, Object>> types = new java.util.ArrayList<>();
        for (RoomType type : RoomType.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", type.name());
            item.put("name", type.getDescription());
            item.put("hasOccupancy", type.isHasOccupancy());
            item.put("hasGender", type.isHasGender());
            types.add(item);
        }
        return Result.success(types);
    }

    @GetMapping("/enums/building-types")
    @Operation(summary = "获取楼宇类型枚举")
    public Result<List<Map<String, Object>>> getBuildingTypes() {
        List<Map<String, Object>> types = new java.util.ArrayList<>();
        for (BuildingType type : BuildingType.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", type.name());
            item.put("name", type.getDescription());
            types.add(item);
        }
        return Result.success(types);
    }

    @GetMapping("/enums/statuses")
    @Operation(summary = "获取场所状态枚举")
    public Result<List<Map<String, Object>>> getStatuses() {
        List<Map<String, Object>> statuses = new java.util.ArrayList<>();
        for (PlaceStatus status : PlaceStatus.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", status.getCode());
            item.put("name", status.getDescription());
            statuses.add(item);
        }
        return Result.success(statuses);
    }

    @GetMapping("/enums/gender-types")
    @Operation(summary = "获取性别类型枚举")
    public Result<List<Map<String, Object>>> getGenderTypes() {
        List<Map<String, Object>> types = new java.util.ArrayList<>();
        for (GenderType type : GenderType.values()) {
            Map<String, Object> item = new HashMap<>();
            item.put("code", type.getCode());
            item.put("name", type.getDescription());
            types.add(item);
        }
        return Result.success(types);
    }
}
