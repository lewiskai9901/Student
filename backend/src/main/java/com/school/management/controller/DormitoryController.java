package com.school.management.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.common.PageResult;
import com.school.management.common.result.Result;
import com.school.management.dto.DormitoryCreateRequest;
import com.school.management.dto.DormitoryQueryRequest;
import com.school.management.dto.DormitoryResponse;
import com.school.management.dto.DormitoryUpdateRequest;
import com.school.management.service.DormitoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 宿舍管理控制器 (V1 - 已弃用)
 *
 * @author system
 * @since 1.0.0
 * @deprecated 使用 V2 API {@link com.school.management.interfaces.rest.asset.DormitoryController} 替代
 *             V2 API 路径: /api/v2/dormitories
 */
@Deprecated(since = "2.0.0", forRemoval = false)
@Slf4j
@RestController
@RequestMapping("/dormitory/rooms")
@RequiredArgsConstructor
@Tag(name = "宿舍管理 (已弃用)", description = "宿舍管理相关接口 - 请使用 /api/v2/dormitories")
public class DormitoryController {

    private final DormitoryService dormitoryService;
    private final com.school.management.service.StudentService studentService;

    /**
     * 创建宿舍
     */
    @PostMapping
    @Operation(summary = "创建宿舍", description = "创建新的宿舍")
    @PreAuthorize("hasAuthority('student:dormitory:add')")
    public Result<Long> createDormitory(@Valid @RequestBody DormitoryCreateRequest request) {
        log.info("创建宿舍请求: {}", request.getDormitoryNo());
        Long dormitoryId = dormitoryService.createDormitory(request);
        return Result.success(dormitoryId);
    }

    /**
     * 更新宿舍
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新宿舍", description = "更新指定宿舍信息")
    @PreAuthorize("hasAuthority('student:dormitory:edit')")
    public Result<Void> updateDormitory(
            @Parameter(description = "宿舍ID") @PathVariable Long id,
            @Valid @RequestBody DormitoryUpdateRequest request) {
        log.info("更新宿舍: {}", id);
        request.setId(id);
        dormitoryService.updateDormitory(request);
        return Result.success();
    }

    /**
     * 删除宿舍
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除宿舍", description = "删除指定的宿舍")
    @PreAuthorize("hasAuthority('student:dormitory:delete')")
    public Result<Void> deleteDormitory(
            @Parameter(description = "宿舍ID") @PathVariable Long id,
            @Parameter(description = "是否强制删除") @RequestParam(defaultValue = "false") boolean force) {
        log.info("删除宿舍: {}, 强制删除: {}", id, force);
        dormitoryService.deleteDormitory(id, force);
        return Result.success();
    }

    /**
     * 批量删除宿舍
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除宿舍", description = "批量删除多个宿舍")
    @PreAuthorize("hasAuthority('student:dormitory:delete')")
    public Result<Void> deleteDormitories(@RequestBody List<Long> ids) {
        log.info("批量删除宿舍: {}", ids);
        dormitoryService.deleteDormitories(ids);
        return Result.success();
    }

    /**
     * 根据ID获取宿舍
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取宿舍详情", description = "根据ID获取宿舍详细信息")
    @PreAuthorize("hasAuthority('student:dormitory:view')")
    public Result<DormitoryResponse> getDormitoryById(@Parameter(description = "宿舍ID") @PathVariable Long id) {
        DormitoryResponse dormitory = dormitoryService.getDormitoryById(id);
        return Result.success(dormitory);
    }

    /**
     * 分页查询宿舍
     */
    @GetMapping
    @Operation(summary = "分页查询宿舍", description = "根据条件分页查询宿舍")
    @PreAuthorize("hasAuthority('student:dormitory:view')")
    public Result<PageResult<DormitoryResponse>> getDormitoryPage(DormitoryQueryRequest request) {
        IPage<DormitoryResponse> page = dormitoryService.getDormitoryPage(request);
        PageResult<DormitoryResponse> result = PageResult.from(page);
        return Result.success(result);
    }

    /**
     * 根据楼栋获取宿舍列表
     */
    @GetMapping("/by-building")
    @Operation(summary = "根据楼宇ID获取宿舍", description = "根据楼宇ID获取宿舍列表")
    @PreAuthorize("hasAuthority('student:dormitory:view')")
    public Result<List<DormitoryResponse>> getDormitoriesByBuildingId(
            @Parameter(description = "楼宇ID") @RequestParam Long buildingId) {
        List<DormitoryResponse> dormitories = dormitoryService.getDormitoriesByBuildingId(buildingId);
        return Result.success(dormitories);
    }

    /**
     * 根据性别类型获取宿舍列表
     */
    @GetMapping("/by-gender/{genderType}")
    @Operation(summary = "根据性别类型获取宿舍", description = "根据性别类型获取宿舍列表")
    @PreAuthorize("hasAuthority('student:dormitory:view')")
    public Result<List<DormitoryResponse>> getDormitoriesByGenderType(
            @Parameter(description = "性别类型") @PathVariable Integer genderType) {
        List<DormitoryResponse> dormitories = dormitoryService.getDormitoriesByGenderType(genderType);
        return Result.success(dormitories);
    }

    /**
     * 根据部门ID获取宿舍列表
     */
    @GetMapping("/by-department")
    @Operation(summary = "根据部门ID获取宿舍", description = "根据部门ID获取宿舍列表")
    @PreAuthorize("hasAuthority('student:dormitory:view')")
    public Result<List<DormitoryResponse>> getDormitoriesByDepartmentId(
            @Parameter(description = "部门ID") @RequestParam Long departmentId) {
        List<DormitoryResponse> dormitories = dormitoryService.getDormitoriesByDepartmentId(departmentId);
        return Result.success(dormitories);
    }

    /**
     * 获取有空床位的宿舍
     */
    @GetMapping("/available")
    @Operation(summary = "获取有空床位的宿舍", description = "获取有空床位的宿舍列表")
    @PreAuthorize("hasAuthority('student:dormitory:view')")
    public Result<List<DormitoryResponse>> getAvailableDormitories(
            @Parameter(description = "性别类型") @RequestParam(required = false) Integer genderType) {
        List<DormitoryResponse> dormitories = dormitoryService.getAvailableDormitories(genderType);
        return Result.success(dormitories);
    }

    /**
     * 根据宿管员ID获取宿舍列表
     */
    @GetMapping("/by-supervisor/{supervisorId}")
    @Operation(summary = "根据宿管员获取宿舍", description = "根据宿管员ID获取宿舍列表")
    @PreAuthorize("hasAuthority('student:dormitory:view')")
    public Result<List<DormitoryResponse>> getDormitoriesBySupervisorId(
            @Parameter(description = "宿管员ID") @PathVariable Long supervisorId) {
        List<DormitoryResponse> dormitories = dormitoryService.getDormitoriesBySupervisorId(supervisorId);
        return Result.success(dormitories);
    }

    /**
     * 获取所有正常状态的宿舍
     */
    @GetMapping("/normal")
    @Operation(summary = "获取所有正常状态的宿舍", description = "获取所有状态为正常的宿舍")
    @PreAuthorize("hasAuthority('student:dormitory:view')")
    public Result<List<DormitoryResponse>> getAllNormalDormitories() {
        List<DormitoryResponse> dormitories = dormitoryService.getAllNormalDormitories();
        return Result.success(dormitories);
    }

    /**
     * 检查宿舍编号是否存在
     */
    @GetMapping("/exists")
    @Operation(summary = "检查宿舍编号", description = "检查宿舍编号是否已存在")
    @PreAuthorize("hasAuthority('student:dormitory:view')")
    public Result<Boolean> existsDormitoryNo(
            @Parameter(description = "楼宇ID") @RequestParam Long buildingId,
            @Parameter(description = "宿舍编号") @RequestParam String dormitoryNo,
            @Parameter(description = "排除的ID") @RequestParam(required = false) Long excludeId) {
        boolean exists = dormitoryService.existsDormitoryNo(buildingId, dormitoryNo, excludeId);
        return Result.success(exists);
    }

    /**
     * 更新宿舍已占用床位数
     */
    @PatchMapping("/{id}/occupied-beds")
    @Operation(summary = "更新宿舍已占用床位数", description = "更新宿舍的已占用床位数")
    @PreAuthorize("hasAuthority('student:dormitory:edit')")
    public Result<Void> updateOccupiedBeds(
            @Parameter(description = "宿舍ID") @PathVariable Long id,
            @Parameter(description = "已占用床位数") @RequestParam Integer occupiedBeds) {
        log.info("更新宿舍已占用床位数: {} -> {}", id, occupiedBeds);
        dormitoryService.updateOccupiedBeds(id, occupiedBeds);
        return Result.success();
    }

    /**
     * 更新宿舍状态
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "更新宿舍状态", description = "更新宿舍的状态")
    @PreAuthorize("hasAuthority('student:dormitory:edit')")
    public Result<Void> updateStatus(
            @Parameter(description = "宿舍ID") @PathVariable Long id,
            @Parameter(description = "状态") @RequestParam Integer status) {
        log.info("更新宿舍状态: {} -> {}", id, status);
        dormitoryService.updateStatus(id, status);
        return Result.success();
    }

    /**
     * 分配床位
     */
    @PostMapping("/{dormitoryId}/assign-bed")
    @Operation(summary = "分配床位", description = "为学生分配宿舍床位")
    @PreAuthorize("hasAuthority('student:dormitory:edit')")
    public Result<Void> assignBed(
            @Parameter(description = "宿舍ID") @PathVariable Long dormitoryId,
            @Parameter(description = "学生ID") @RequestParam Long studentId) {
        log.info("分配床位: 宿舍ID={}, 学生ID={}", dormitoryId, studentId);
        dormitoryService.assignBed(dormitoryId, studentId);
        return Result.success();
    }

    /**
     * 获取宿舍床位分配情况
     */
    @GetMapping("/{dormitoryId}/bed-allocations")
    @Operation(summary = "获取床位分配情况", description = "获取指定宿舍的床位分配详情")
    @PreAuthorize("hasAuthority('student:dormitory:view')")
    public Result<List<com.school.management.dto.BedAllocationResponse>> getBedAllocations(
            @Parameter(description = "宿舍ID") @PathVariable Long dormitoryId) {
        log.info("获取宿舍床位分配情况: {}", dormitoryId);
        List<com.school.management.dto.BedAllocationResponse> allocations = dormitoryService.getBedAllocations(dormitoryId);
        return Result.success(allocations);
    }

    @PostMapping("/{dormitoryId}/release-bed")
    @Operation(summary = "释放床位", description = "释放学生的宿舍床位")
    @PreAuthorize("hasAuthority('student:dormitory:edit')")
    public Result<Void> releaseBed(
            @Parameter(description = "宿舍ID") @PathVariable Long dormitoryId,
            @Parameter(description = "学生ID") @RequestParam Long studentId) {
        log.info("释放床位: 宿舍ID={}, 学生ID={}", dormitoryId, studentId);
        dormitoryService.releaseBed(dormitoryId, studentId);
        return Result.success();
    }

    /**
     * 添加学生到宿舍
     */
    @PostMapping("/assign-student")
    @Operation(summary = "添加学生到宿舍", description = "将学生分配到指定宿舍和床位")
    @PreAuthorize("hasAuthority('student:dormitory:edit')")
    public Result<Void> assignStudentToDormitory(@RequestBody java.util.Map<String, Object> request) {
        Long studentId = Long.valueOf(request.get("studentId").toString());
        Long dormitoryId = Long.valueOf(request.get("dormitoryId").toString());
        Object bedNumberObj = request.get("bedNumber");
        String bedNumber = bedNumberObj != null ? bedNumberObj.toString() : null;

        log.info("添加学生到宿舍: 学生ID={}, 宿舍ID={}, 床位号={}", studentId, dormitoryId, bedNumber);

        // 先进行所有验证(调用assignBed进行验证)
        dormitoryService.assignBed(dormitoryId, studentId);

        // 验证通过后,更新学生的宿舍信息
        com.school.management.entity.Student student = studentMapper.selectById(studentId);
        if (student == null) {
            throw new com.school.management.exception.BusinessException("学生不存在");
        }

        student.setDormitoryId(dormitoryId);
        student.setBedNumber(bedNumber);
        student.setUpdatedAt(java.time.LocalDateTime.now());
        studentMapper.updateById(student);

        return Result.success();
    }

    /**
     * 从宿舍移除学生
     */
    @DeleteMapping("/remove-student/{studentId}")
    @Operation(summary = "从宿舍移除学生", description = "将学生从当前宿舍移除")
    @PreAuthorize("hasAuthority('student:dormitory:edit')")
    public Result<Void> removeStudentFromDormitory(@PathVariable Long studentId) {
        log.info("从宿舍移除学生: 学生ID={}", studentId);

        // 查询学生信息，获取宿舍ID
        com.school.management.entity.Student student = studentMapper.selectById(studentId);
        if (student == null) {
            log.error("学生不存在: studentId={}", studentId);
            throw new com.school.management.exception.BusinessException("学生不存在");
        }

        Long dormitoryId = student.getDormitoryId();
        log.info("学生宿舍信息: studentId={}, dormitoryId={}, bedNumber={}",
            studentId, dormitoryId, student.getBedNumber());

        // 如果学生有分配宿舍，则释放床位
        if (dormitoryId != null) {
            com.school.management.entity.Dormitory dormitory = dormitoryMapper.selectById(dormitoryId);
            if (dormitory != null && dormitory.getOccupiedBeds() != null && dormitory.getOccupiedBeds() > 0) {
                try {
                    dormitoryService.releaseBed(dormitoryId, studentId);
                    log.info("释放床位成功: dormitoryId={}", dormitoryId);
                } catch (Exception e) {
                    log.error("释放床位失败: dormitoryId={}", dormitoryId, e);
                    throw e;
                }
            }
        }

        // 使用StudentService移除宿舍信息
        studentService.removeDormitory(studentId);

        log.info("移除学生操作完成: studentId={}", studentId);
        return Result.success();
    }

    /**
     * 交换学生宿舍
     */
    @PostMapping("/swap-students")
    @Operation(summary = "交换学生宿舍", description = "交换两个学生的宿舍和床位")
    @PreAuthorize("hasAuthority('student:dormitory:edit')")
    public Result<Void> swapStudentDormitory(@RequestBody java.util.Map<String, Object> request) {
        Long studentAId = Long.valueOf(request.get("studentAId").toString());
        Long studentBId = Long.valueOf(request.get("studentBId").toString());

        log.info("交换学生宿舍: 学生A ID={}, 学生B ID={}", studentAId, studentBId);

        com.school.management.entity.Student studentA = studentMapper.selectById(studentAId);
        com.school.management.entity.Student studentB = studentMapper.selectById(studentBId);

        if (studentA == null || studentB == null) {
            throw new com.school.management.exception.BusinessException("学生不存在");
        }

        // 交换宿舍信息
        Long tempDormitoryId = studentA.getDormitoryId();
        String tempBedNumber = studentA.getBedNumber();

        studentA.setDormitoryId(studentB.getDormitoryId());
        studentA.setBedNumber(studentB.getBedNumber());
        studentA.setUpdatedAt(java.time.LocalDateTime.now());

        studentB.setDormitoryId(tempDormitoryId);
        studentB.setBedNumber(tempBedNumber);
        studentB.setUpdatedAt(java.time.LocalDateTime.now());

        studentMapper.updateById(studentA);
        studentMapper.updateById(studentB);

        return Result.success();
    }

    /**
     * 批量生成宿舍
     */
    @PostMapping("/batch")
    @Operation(summary = "批量生成宿舍", description = "批量生成宿舍,支持多楼层多房间")
    @PreAuthorize("hasAuthority('student:dormitory:add')")
    public Result<Integer> batchCreateDormitories(@Valid @RequestBody com.school.management.dto.DormitoryBatchCreateRequest request) {
        log.info("批量生成宿舍请求: buildingId={}, floors={}-{}, roomsPerFloor={}",
            request.getBuildingId(), request.getStartFloor(), request.getEndFloor(), request.getRoomsPerFloor());
        int count = dormitoryService.batchCreateDormitories(request);
        return Result.success(count);
    }

    /**
     * 批量更新宿舍房间的院系分配
     */
    @PutMapping("/batch-department")
    @Operation(summary = "批量更新院系分配", description = "批量更新宿舍房间的院系分配")
    @PreAuthorize("hasAuthority('student:dormitory:edit')")
    public Result<Integer> batchUpdateDepartment(@RequestBody java.util.Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Long> dormitoryIds = ((List<?>) request.get("dormitoryIds")).stream()
            .map(id -> Long.valueOf(id.toString()))
            .collect(java.util.stream.Collectors.toList());
        Long departmentId = request.get("departmentId") != null
            ? Long.valueOf(request.get("departmentId").toString())
            : null;

        log.info("批量更新院系分配: dormitoryIds={}, departmentId={}", dormitoryIds, departmentId);
        int count = dormitoryService.batchUpdateDepartment(dormitoryIds, departmentId);
        return Result.success(count);
    }

    /**
     * 按楼层批量更新院系分配
     */
    @PutMapping("/batch-department-by-floor")
    @Operation(summary = "按楼层批量更新院系分配", description = "将指定楼宇指定楼层的所有宿舍分配给指定院系")
    @PreAuthorize("hasAuthority('student:dormitory:edit')")
    public Result<Integer> batchUpdateDepartmentByFloor(@RequestBody java.util.Map<String, Object> request) {
        Long buildingId = Long.valueOf(request.get("buildingId").toString());
        Integer floor = Integer.valueOf(request.get("floor").toString());
        Long departmentId = request.get("departmentId") != null
            ? Long.valueOf(request.get("departmentId").toString())
            : null;

        log.info("按楼层批量更新院系分配: buildingId={}, floor={}, departmentId={}", buildingId, floor, departmentId);
        int count = dormitoryService.batchUpdateDepartmentByFloor(buildingId, floor, departmentId);
        return Result.success(count);
    }

    // 注入依赖
    @org.springframework.beans.factory.annotation.Autowired
    private com.school.management.mapper.StudentMapper studentMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private com.school.management.mapper.DormitoryMapper dormitoryMapper;
}