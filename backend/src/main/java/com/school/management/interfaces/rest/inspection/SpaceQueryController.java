package com.school.management.interfaces.rest.inspection;

import com.school.management.common.result.Result;
import com.school.management.mapper.BuildingMapper;
import com.school.management.mapper.DormitoryMapper;
import com.school.management.entity.Building;
import com.school.management.entity.Dormitory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for querying physical spaces during inspection.
 */
@RestController
@RequestMapping("/v2/inspection/spaces")
@Tag(name = "Inspection Spaces", description = "Space query API for inspection workflows")
public class SpaceQueryController {

    private final BuildingMapper buildingMapper;
    private final DormitoryMapper dormitoryMapper;

    public SpaceQueryController(BuildingMapper buildingMapper, DormitoryMapper dormitoryMapper) {
        this.buildingMapper = buildingMapper;
        this.dormitoryMapper = dormitoryMapper;
    }

    @GetMapping("/buildings")
    @Operation(summary = "List all buildings for inspection")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<List<Building>> getBuildings() {
        List<Building> buildings = buildingMapper.selectList(null);
        return Result.success(buildings);
    }

    @GetMapping("/rooms")
    @Operation(summary = "List rooms in a building or floor")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<List<Dormitory>> getRooms(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Integer floor) {
        // Simple query - fetch dormitories with optional building/floor filter
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Dormitory>();
        if (buildingId != null) {
            wrapper.eq("building_id", buildingId);
        }
        if (floor != null) {
            wrapper.eq("floor", floor);
        }
        wrapper.eq("deleted", 0);
        wrapper.orderByAsc("room_no");
        List<Dormitory> rooms = dormitoryMapper.selectList(wrapper);
        return Result.success(rooms);
    }
}
