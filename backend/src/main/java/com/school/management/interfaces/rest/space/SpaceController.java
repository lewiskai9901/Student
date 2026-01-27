package com.school.management.interfaces.rest.space;

import com.school.management.application.space.SpaceApplicationService;
import com.school.management.application.space.command.CheckInCommand;
import com.school.management.application.space.command.CreateSpaceCommand;
import com.school.management.application.space.command.UpdateSpaceCommand;
import com.school.management.application.space.query.SpaceDTO;
import com.school.management.application.space.query.SpaceOccupantDTO;
import com.school.management.application.space.query.SpaceQueryCriteria;
import com.school.management.common.result.Result;
import com.school.management.domain.space.model.valueobject.BuildingType;
import com.school.management.domain.space.model.valueobject.RoomType;
import com.school.management.domain.space.model.valueobject.SpaceStatus;
import com.school.management.domain.space.model.valueobject.SpaceType;
import com.school.management.interfaces.rest.space.dto.BatchAssignClassRequest;
import com.school.management.interfaces.rest.space.dto.BatchAssignOrgUnitRequest;
import com.school.management.interfaces.rest.space.dto.CheckInRequest;
import com.school.management.interfaces.rest.space.dto.CreateSpaceRequest;
import com.school.management.interfaces.rest.space.dto.UpdateSpaceRequest;
import com.school.management.domain.space.model.valueobject.GenderType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 场所管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/v2/spaces")
@RequiredArgsConstructor
@Tag(name = "场所管理", description = "统一场所管理API，包括校区、楼宇、楼层、房间等")
public class SpaceController {

    private final SpaceApplicationService spaceService;

    // ========== 基础CRUD ==========

    @PostMapping
    @Operation(summary = "创建场所")
    @PreAuthorize("hasAuthority('space:create')")
    public Result<Long> createSpace(@Valid @RequestBody CreateSpaceRequest request) {
        CreateSpaceCommand command = new CreateSpaceCommand();
        command.setSpaceType(request.getSpaceType());
        command.setSpaceCode(request.getSpaceCode());
        command.setSpaceName(request.getSpaceName());
        command.setRoomType(request.getRoomType());
        command.setBuildingType(request.getBuildingType());
        command.setBuildingNo(request.getBuildingNo());
        command.setRoomNo(request.getRoomNo());
        command.setParentId(request.getParentId());
        command.setFloorNumber(request.getFloorNumber());
        command.setCapacity(request.getCapacity());
        command.setOrgUnitId(request.getOrgUnitId());
        command.setClassId(request.getClassId());
        command.setResponsibleUserId(request.getResponsibleUserId());
        command.setGenderType(request.getGenderType());
        command.setDescription(request.getDescription());
        command.setAttributes(request.getAttributes());

        Long id = spaceService.createSpace(command);
        return Result.success(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新场所")
    @PreAuthorize("hasAuthority('space:update')")
    public Result<Void> updateSpace(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSpaceRequest request) {
        UpdateSpaceCommand command = new UpdateSpaceCommand();
        command.setId(id);
        command.setSpaceName(request.getSpaceName());
        command.setDescription(request.getDescription());
        command.setBuildingNo(request.getBuildingNo());
        command.setRoomNo(request.getRoomNo());
        command.setCapacity(request.getCapacity());
        command.setOrgUnitId(request.getOrgUnitId());
        command.setClassId(request.getClassId());
        command.setResponsibleUserId(request.getResponsibleUserId());
        command.setGenderType(request.getGenderType());
        command.setAttributes(request.getAttributes());

        spaceService.updateSpace(command);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除场所")
    @PreAuthorize("hasAuthority('space:delete')")
    public Result<Void> deleteSpace(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force) {
        spaceService.deleteSpace(id, force);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取场所详情")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<SpaceDTO> getById(@PathVariable Long id) {
        return Result.success(spaceService.getById(id));
    }

    // ========== 状态管理 ==========

    @PutMapping("/{id}/status")
    @Operation(summary = "变更场所状态")
    @PreAuthorize("hasAuthority('space:update')")
    public Result<Void> changeStatus(
            @PathVariable Long id,
            @RequestParam SpaceStatus status) {
        spaceService.changeStatus(id, status);
        return Result.success();
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用场所")
    @PreAuthorize("hasAuthority('space:update')")
    public Result<Void> enable(@PathVariable Long id) {
        spaceService.changeStatus(id, SpaceStatus.NORMAL);
        return Result.success();
    }

    @PutMapping("/{id}/disable")
    @Operation(summary = "禁用场所")
    @PreAuthorize("hasAuthority('space:update')")
    public Result<Void> disable(@PathVariable Long id) {
        spaceService.changeStatus(id, SpaceStatus.DISABLED);
        return Result.success();
    }

    @PutMapping("/{id}/maintenance")
    @Operation(summary = "设置维护状态")
    @PreAuthorize("hasAuthority('space:update')")
    public Result<Void> startMaintenance(@PathVariable Long id) {
        spaceService.changeStatus(id, SpaceStatus.MAINTENANCE);
        return Result.success();
    }

    // ========== 入住/退出 ==========

    @PostMapping("/{id}/check-in")
    @Operation(summary = "入住")
    @PreAuthorize("hasAuthority('space:occupant:manage')")
    public Result<Long> checkIn(
            @PathVariable Long id,
            @Valid @RequestBody CheckInRequest request) {
        CheckInCommand command = new CheckInCommand();
        command.setSpaceId(id);
        command.setOccupantType(request.getOccupantType());
        command.setOccupantId(request.getOccupantId());
        command.setPositionNo(request.getPositionNo());
        command.setRemark(request.getRemark());

        Long occupantRecordId = spaceService.checkIn(command);
        return Result.success(occupantRecordId);
    }

    @PostMapping("/{spaceId}/check-out/{occupantRecordId}")
    @Operation(summary = "退出")
    @PreAuthorize("hasAuthority('space:occupant:manage')")
    public Result<Void> checkOut(
            @PathVariable Long spaceId,
            @PathVariable Long occupantRecordId) {
        spaceService.checkOut(spaceId, occupantRecordId);
        return Result.success();
    }

    @GetMapping("/{id}/occupants")
    @Operation(summary = "获取场所占用者列表")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<List<SpaceOccupantDTO>> getOccupants(@PathVariable Long id) {
        return Result.success(spaceService.getOccupants(id));
    }

    @GetMapping("/{id}/occupant-history")
    @Operation(summary = "获取场所占用历史")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<List<SpaceOccupantDTO>> getOccupantHistory(@PathVariable Long id) {
        return Result.success(spaceService.getOccupantHistory(id));
    }

    // ========== 批量操作 ==========

    @PostMapping("/batch/assign-org-unit")
    @Operation(summary = "批量分配组织单元")
    @PreAuthorize("hasAuthority('space:update')")
    public Result<Void> batchAssignOrgUnit(@Valid @RequestBody BatchAssignOrgUnitRequest request) {
        spaceService.batchAssignOrgUnit(request.getSpaceIds(), request.getOrgUnitId());
        return Result.success();
    }

    @PostMapping("/batch/assign-class")
    @Operation(summary = "批量分配班级")
    @PreAuthorize("hasAuthority('space:update')")
    public Result<Void> batchAssignClass(@Valid @RequestBody BatchAssignClassRequest request) {
        spaceService.batchAssignClass(request.getSpaceIds(), request.getClassId());
        return Result.success();
    }

    // ========== 班级分配管理 ==========

    @PutMapping("/{id}/class")
    @Operation(summary = "设置场所归属班级")
    @PreAuthorize("hasAuthority('space:update')")
    public Result<Void> assignClass(
            @PathVariable Long id,
            @RequestParam Long classId) {
        spaceService.batchAssignClass(List.of(id), classId);
        return Result.success();
    }

    @DeleteMapping("/{id}/class")
    @Operation(summary = "取消场所班级分配")
    @PreAuthorize("hasAuthority('space:update')")
    public Result<Void> unassignClass(@PathVariable Long id) {
        spaceService.unassignClass(id);
        return Result.success();
    }

    @PutMapping("/{id}/gender-type")
    @Operation(summary = "设置性别限制")
    @PreAuthorize("hasAuthority('space:update')")
    public Result<Void> setGenderType(
            @PathVariable Long id,
            @RequestParam Integer genderType) {
        spaceService.setGenderRestriction(id, genderType);
        return Result.success();
    }

    // ========== 树形查询 ==========

    @GetMapping("/tree")
    @Operation(summary = "获取场所树", description = "获取完整的场所树结构，可按楼宇类型筛选")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<List<SpaceDTO>> getTree(
            @Parameter(description = "楼宇类型筛选")
            @RequestParam(required = false) BuildingType buildingType,
            @Parameter(description = "是否包含统计信息")
            @RequestParam(defaultValue = "false") boolean includeStatistics) {
        return Result.success(spaceService.getTree(buildingType, includeStatistics));
    }

    @GetMapping("/{id}/children")
    @Operation(summary = "获取子节点")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<List<SpaceDTO>> getChildren(@PathVariable Long id) {
        return Result.success(spaceService.getChildren(id));
    }

    @GetMapping("/{id}/ancestors")
    @Operation(summary = "获取祖先链")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<List<SpaceDTO>> getAncestors(@PathVariable Long id) {
        return Result.success(spaceService.getAncestors(id));
    }

    // ========== 列表查询 ==========

    @GetMapping("/buildings")
    @Operation(summary = "获取楼宇列表")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<List<SpaceDTO>> getBuildings(
            @RequestParam(required = false) BuildingType buildingType,
            @RequestParam(required = false) SpaceStatus status) {
        return Result.success(spaceService.getBuildings(buildingType, status));
    }

    @GetMapping
    @Operation(summary = "分页查询场所")
    @PreAuthorize("hasAuthority('space:view')")
    public Result<Map<String, Object>> query(
            @RequestParam(required = false) SpaceType spaceType,
            @RequestParam(required = false) RoomType roomType,
            @RequestParam(required = false) BuildingType buildingType,
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Integer floorNumber,
            @RequestParam(required = false) Long orgUnitId,
            @RequestParam(required = false) SpaceStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        SpaceQueryCriteria criteria = new SpaceQueryCriteria();
        criteria.setSpaceType(spaceType);
        criteria.setRoomType(roomType);
        criteria.setBuildingType(buildingType);
        criteria.setBuildingId(buildingId);
        criteria.setFloorNumber(floorNumber);
        criteria.setOrgUnitId(orgUnitId);
        criteria.setStatus(status);
        criteria.setKeyword(keyword);
        criteria.setPage(page);
        criteria.setPageSize(pageSize);

        List<SpaceDTO> list = spaceService.query(criteria);
        long total = spaceService.count(criteria);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("pageSize", pageSize);

        return Result.success(result);
    }

    // ========== 枚举查询 ==========

    @GetMapping("/enums/space-types")
    @Operation(summary = "获取场所类型枚举")
    public Result<List<Map<String, Object>>> getSpaceTypes() {
        List<Map<String, Object>> types = new java.util.ArrayList<>();
        for (SpaceType type : SpaceType.values()) {
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
        for (SpaceStatus status : SpaceStatus.values()) {
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
