package com.school.management.interfaces.rest.asset;

import com.school.management.application.asset.AssetApplicationService;
import com.school.management.application.asset.command.*;
import com.school.management.application.asset.query.DormitoryDTO;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.interfaces.rest.asset.dto.CreateDormitoryRequest;
import com.school.management.interfaces.rest.asset.dto.CheckInRequest;
import com.school.management.interfaces.rest.asset.dto.CheckOutRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 宿舍管理 REST API控制器 (DDD架构)
 */
@Tag(name = "Dormitory Management V2", description = "宿舍管理API - DDD架构")
@RestController("dormitoryControllerV2")
@RequestMapping("/v2/dormitory/rooms")
@RequiredArgsConstructor
public class DormitoryController {

    private final AssetApplicationService assetService;

    // ==================== 基础CRUD ====================

    @Operation(summary = "分页查询宿舍")
    @GetMapping
    @PreAuthorize("hasAuthority('dormitory:view')")
    public Result<PageResult<DormitoryDTO>> getDormitories(
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "楼宇ID") @RequestParam(required = false) Long buildingId,
            @Parameter(description = "部门ID") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "性别类型") @RequestParam(required = false) Integer genderType,
            @Parameter(description = "状态") @RequestParam(required = false) Integer status,
            @Parameter(description = "是否有空床") @RequestParam(required = false) Boolean hasAvailableBeds,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer pageSize) {

        PageResult<DormitoryDTO> result = assetService.findDormitoriesByPage(
                keyword, buildingId, departmentId, genderType, status, hasAvailableBeds, pageNum, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "获取宿舍详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('dormitory:view')")
    public Result<DormitoryDTO> getDormitory(
            @Parameter(description = "宿舍ID") @PathVariable Long id) {
        DormitoryDTO result = assetService.getDormitoryById(id);
        return Result.success(result);
    }

    @Operation(summary = "根据楼宇获取宿舍列表")
    @GetMapping("/by-building/{buildingId}")
    @PreAuthorize("hasAuthority('dormitory:view')")
    public Result<List<DormitoryDTO>> getDormitoriesByBuilding(
            @Parameter(description = "楼宇ID") @PathVariable Long buildingId) {
        List<DormitoryDTO> result = assetService.getDormitoriesByBuildingId(buildingId);
        return Result.success(result);
    }

    @Operation(summary = "创建宿舍")
    @PostMapping
    @PreAuthorize("hasAuthority('dormitory:create')")
    public Result<Long> createDormitory(@RequestBody CreateDormitoryRequest request) {
        CreateDormitoryCommand command = CreateDormitoryCommand.builder()
                .buildingId(request.getBuildingId())
                .dormitoryNo(request.getDormitoryNo())
                .floorNumber(request.getFloorNumber())
                .roomUsageType(request.getRoomUsageType())
                .bedCapacity(request.getBedCapacity())
                .genderType(request.getGenderType())
                .departmentId(request.getDepartmentId())
                .facilities(request.getFacilities())
                .notes(request.getNotes())
                .build();

        Long id = assetService.createDormitory(command);
        return Result.success(id);
    }

    @Operation(summary = "更新宿舍")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('dormitory:update')")
    public Result<Void> updateDormitory(
            @Parameter(description = "宿舍ID") @PathVariable Long id,
            @RequestBody CreateDormitoryRequest request) {
        CreateDormitoryCommand command = CreateDormitoryCommand.builder()
                .buildingId(request.getBuildingId())
                .dormitoryNo(request.getDormitoryNo())
                .floorNumber(request.getFloorNumber())
                .roomUsageType(request.getRoomUsageType())
                .bedCapacity(request.getBedCapacity())
                .genderType(request.getGenderType())
                .departmentId(request.getDepartmentId())
                .facilities(request.getFacilities())
                .notes(request.getNotes())
                .build();

        assetService.updateDormitory(id, command);
        return Result.success();
    }

    @Operation(summary = "删除宿舍")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('dormitory:delete')")
    public Result<Void> deleteDormitory(
            @Parameter(description = "宿舍ID") @PathVariable Long id) {
        assetService.deleteDormitory(id);
        return Result.success();
    }

    // ==================== 入住/退宿管理 ====================

    @Operation(summary = "学生入住")
    @PostMapping("/{id}/check-in")
    @PreAuthorize("hasAuthority('dormitory:update')")
    public Result<Void> checkIn(
            @Parameter(description = "宿舍ID") @PathVariable Long id,
            @RequestBody CheckInRequest request) {
        CheckInCommand command = CheckInCommand.builder()
                .dormitoryId(id)
                .studentId(request.getStudentId())
                .studentName(request.getStudentName())
                .bedNumber(request.getBedNumber())
                .build();

        assetService.checkIn(command);
        return Result.success();
    }

    @Operation(summary = "学生退宿")
    @PostMapping("/{id}/check-out")
    @PreAuthorize("hasAuthority('dormitory:update')")
    public Result<Void> checkOut(
            @Parameter(description = "宿舍ID") @PathVariable Long id,
            @RequestBody CheckOutRequest request) {
        CheckOutCommand command = CheckOutCommand.builder()
                .dormitoryId(id)
                .studentId(request.getStudentId())
                .studentName(request.getStudentName())
                .bedNumber(request.getBedNumber())
                .build();

        assetService.checkOut(command);
        return Result.success();
    }
}
