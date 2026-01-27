package com.school.management.interfaces.rest.inspection;

import com.school.management.common.result.Result;
import com.school.management.domain.asset.model.aggregate.Building;
import com.school.management.domain.asset.model.aggregate.Dormitory;
import com.school.management.domain.asset.repository.BuildingRepository;
import com.school.management.domain.asset.repository.DormitoryRepository;
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

    private final BuildingRepository buildingRepository;
    private final DormitoryRepository dormitoryRepository;

    public SpaceQueryController(BuildingRepository buildingRepository, DormitoryRepository dormitoryRepository) {
        this.buildingRepository = buildingRepository;
        this.dormitoryRepository = dormitoryRepository;
    }

    @GetMapping("/buildings")
    @Operation(summary = "List all buildings for inspection")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<List<Building>> getBuildings() {
        List<Building> buildings = buildingRepository.findAllActive();
        return Result.success(buildings);
    }

    @GetMapping("/rooms")
    @Operation(summary = "List rooms in a building or floor")
    @PreAuthorize("hasAuthority('inspection:record:view')")
    public Result<List<Dormitory>> getRooms(
            @RequestParam(required = false) Long buildingId,
            @RequestParam(required = false) Integer floor) {
        List<Dormitory> rooms;
        if (buildingId != null && floor != null) {
            rooms = dormitoryRepository.findByBuildingIdAndFloor(buildingId, floor);
        } else if (buildingId != null) {
            rooms = dormitoryRepository.findByBuildingId(buildingId);
        } else {
            // No building filter - use page query with criteria
            DormitoryRepository.DormitoryQueryCriteria criteria = new DormitoryRepository.DormitoryQueryCriteria();
            if (floor != null) {
                criteria.setFloorNumber(floor);
            }
            rooms = dormitoryRepository.findByPage(criteria, 1, 1000);
        }
        return Result.success(rooms);
    }
}
