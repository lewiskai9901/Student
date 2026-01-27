package com.school.management.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.school.management.common.result.Result;
import com.school.management.dto.ClassResponse;
import com.school.management.dto.DormitoryResponse;
import com.school.management.entity.ClassDormitory;
import com.school.management.entity.Dormitory;
import com.school.management.entity.Class;
import com.school.management.entity.Building;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.BuildingMapper;
import com.school.management.mapper.ClassDormitoryMapper;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.DormitoryMapper;
import com.school.management.security.CustomUserDetails;
import com.school.management.service.ClassService;
import com.school.management.service.DormitoryService;
import com.school.management.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 部门宿舍管理控制器
 *
 * 用于部门管理员管理本部门宿舍分配给班级的功能（Level 2）
 *
 * @author system
 * @since 2.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/department-dormitory")
@RequiredArgsConstructor
@Tag(name = "部门宿舍管理", description = "部门管理员管理宿舍分配给班级的接口")
public class DepartmentDormitoryController {

    private final DormitoryService dormitoryService;
    private final ClassService classService;
    private final DormitoryMapper dormitoryMapper;
    private final ClassMapper classMapper;
    private final ClassDormitoryMapper classDormitoryMapper;
    private final BuildingMapper buildingMapper;

    /**
     * 获取当前用户部门下的宿舍列表
     */
    @GetMapping("/dormitories")
    @Operation(summary = "获取部门宿舍", description = "获取当前用户所属部门下的宿舍列表")
    @PreAuthorize("hasAnyAuthority('dormitory:department:view', 'dormitory:department:manage')")
    public Result<List<DormitoryResponse>> getMyDepartmentDormitories() {
        Long orgUnitId = getCurrentUserOrgUnitId();
        log.info("获取部门宿舍列表: orgUnitId={}", orgUnitId);

        List<DormitoryResponse> dormitories = dormitoryService.getDormitoriesByOrgUnitId(orgUnitId);
        return Result.success(dormitories);
    }

    /**
     * 获取当前用户部门下的班级列表
     */
    @GetMapping("/classes")
    @Operation(summary = "获取部门班级", description = "获取当前用户所属部门下的班级列表")
    @PreAuthorize("hasAnyAuthority('dormitory:department:view', 'dormitory:department:manage')")
    public Result<List<ClassResponse>> getMyDepartmentClasses() {
        Long orgUnitId = getCurrentUserOrgUnitId();
        log.info("获取部门班级列表: orgUnitId={}", orgUnitId);

        List<ClassResponse> classes = classService.getClassesByOrgUnitId(orgUnitId);
        return Result.success(classes);
    }

    /**
     * 获取班级-宿舍绑定关系列表
     */
    @GetMapping("/bindings")
    @Operation(summary = "获取绑定关系", description = "获取当前部门下所有班级-宿舍绑定关系")
    @PreAuthorize("hasAnyAuthority('dormitory:department:view', 'dormitory:department:manage')")
    public Result<List<ClassDormitoryBindingDTO>> getBindings() {
        Long orgUnitId = getCurrentUserOrgUnitId();
        log.info("获取班级-宿舍绑定关系: orgUnitId={}", orgUnitId);

        // 获取部门下所有宿舍
        QueryWrapper<Dormitory> dormitoryWrapper = new QueryWrapper<>();
        dormitoryWrapper.eq("org_unit_id", orgUnitId);
        List<Dormitory> dormitories = dormitoryMapper.selectList(dormitoryWrapper);

        if (dormitories.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<Long> dormitoryIds = dormitories.stream()
                .map(Dormitory::getId)
                .collect(Collectors.toList());

        // 查询这些宿舍的班级绑定关系
        QueryWrapper<ClassDormitory> bindingWrapper = new QueryWrapper<>();
        bindingWrapper.in("dormitory_id", dormitoryIds);
        List<ClassDormitory> bindings = classDormitoryMapper.selectList(bindingWrapper);

        // 组装返回数据
        List<ClassDormitoryBindingDTO> result = bindings.stream().map(binding -> {
            ClassDormitoryBindingDTO dto = new ClassDormitoryBindingDTO();
            dto.setId(binding.getId());
            dto.setClassId(binding.getClassId());
            dto.setDormitoryId(binding.getDormitoryId());
            dto.setStudentCount(binding.getStudentCount());
            dto.setCreatedTime(binding.getCreatedTime());

            // 填充班级名称
            Class clazz = classMapper.selectById(binding.getClassId());
            if (clazz != null) {
                dto.setClassName(clazz.getClassName());
                dto.setClassCode(clazz.getClassCode());
            }

            // 填充宿舍信息
            Dormitory dormitory = dormitories.stream()
                    .filter(d -> d.getId().equals(binding.getDormitoryId()))
                    .findFirst()
                    .orElse(null);
            if (dormitory != null) {
                dto.setDormitoryNo(dormitory.getDormitoryNo());
                dto.setBuildingId(dormitory.getBuildingId());
                dto.setBedCapacity(dormitory.getBedCapacity());
                dto.setOccupiedBeds(dormitory.getOccupiedBeds());
                dto.setFloorNumber(dormitory.getFloorNumber());

                // 填充楼栋名称
                if (dormitory.getBuildingId() != null) {
                    Building building = buildingMapper.selectById(dormitory.getBuildingId());
                    if (building != null) {
                        dto.setBuildingName(building.getBuildingName());
                    }
                }
            }

            return dto;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 按班级获取绑定关系
     */
    @GetMapping("/bindings/by-class/{classId}")
    @Operation(summary = "获取班级的宿舍绑定", description = "获取指定班级的所有宿舍绑定关系")
    @PreAuthorize("hasAnyAuthority('dormitory:department:view', 'dormitory:department:manage')")
    public Result<List<ClassDormitoryBindingDTO>> getBindingsByClass(
            @Parameter(description = "班级ID") @PathVariable Long classId) {
        log.info("获取班级宿舍绑定: classId={}", classId);

        // 验证班级属于当前部门
        validateClassBelongsToDepartment(classId);

        QueryWrapper<ClassDormitory> wrapper = new QueryWrapper<>();
        wrapper.eq("class_id", classId);
        List<ClassDormitory> bindings = classDormitoryMapper.selectList(wrapper);

        List<ClassDormitoryBindingDTO> result = bindings.stream().map(binding -> {
            ClassDormitoryBindingDTO dto = new ClassDormitoryBindingDTO();
            dto.setId(binding.getId());
            dto.setClassId(binding.getClassId());
            dto.setDormitoryId(binding.getDormitoryId());
            dto.setStudentCount(binding.getStudentCount());
            dto.setCreatedTime(binding.getCreatedTime());

            // 填充宿舍信息
            Dormitory dormitory = dormitoryMapper.selectById(binding.getDormitoryId());
            if (dormitory != null) {
                dto.setDormitoryNo(dormitory.getDormitoryNo());
                dto.setBuildingId(dormitory.getBuildingId());
                dto.setBedCapacity(dormitory.getBedCapacity());
                dto.setOccupiedBeds(dormitory.getOccupiedBeds());
                dto.setFloorNumber(dormitory.getFloorNumber());

                if (dormitory.getBuildingId() != null) {
                    Building building = buildingMapper.selectById(dormitory.getBuildingId());
                    if (building != null) {
                        dto.setBuildingName(building.getBuildingName());
                    }
                }
            }

            return dto;
        }).collect(Collectors.toList());

        return Result.success(result);
    }

    /**
     * 将宿舍分配给班级
     */
    @PostMapping("/assign")
    @Operation(summary = "分配宿舍给班级", description = "将宿舍分配给指定班级")
    @PreAuthorize("hasAuthority('dormitory:department:manage')")
    public Result<Void> assignDormitoryToClass(@RequestBody AssignDormitoryRequest request) {
        log.info("分配宿舍给班级: dormitoryId={}, classId={}, allocatedBeds={}",
                request.getDormitoryId(), request.getClassId(), request.getAllocatedBeds());

        // 验证宿舍属于当前部门
        validateDormitoryBelongsToDepartment(request.getDormitoryId());

        // 验证班级属于当前部门
        validateClassBelongsToDepartment(request.getClassId());

        // 调用现有服务进行分配
        Integer allocatedBeds = request.getAllocatedBeds();
        if (allocatedBeds == null || allocatedBeds <= 0) {
            // 默认分配全部床位
            Dormitory dormitory = dormitoryMapper.selectById(request.getDormitoryId());
            allocatedBeds = dormitory.getBedCapacity();
        }

        classService.addDormitory(request.getClassId(), request.getDormitoryId(), allocatedBeds);

        return Result.success();
    }

    /**
     * 批量分配宿舍给班级
     */
    @PostMapping("/assign/batch")
    @Operation(summary = "批量分配宿舍给班级", description = "批量将多个宿舍分配给指定班级")
    @PreAuthorize("hasAuthority('dormitory:department:manage')")
    public Result<Integer> batchAssignDormitoriesToClass(@RequestBody BatchAssignRequest request) {
        log.info("批量分配宿舍给班级: classId={}, dormitoryIds={}",
                request.getClassId(), request.getDormitoryIds());

        // 验证班级属于当前部门
        validateClassBelongsToDepartment(request.getClassId());

        int count = 0;
        for (Long dormitoryId : request.getDormitoryIds()) {
            try {
                // 验证宿舍属于当前部门
                validateDormitoryBelongsToDepartment(dormitoryId);

                // 获取宿舍床位数作为默认分配数
                Dormitory dormitory = dormitoryMapper.selectById(dormitoryId);
                Integer allocatedBeds = dormitory.getBedCapacity();

                classService.addDormitory(request.getClassId(), dormitoryId, allocatedBeds);
                count++;
            } catch (Exception e) {
                log.warn("分配宿舍失败: dormitoryId={}, error={}", dormitoryId, e.getMessage());
            }
        }

        return Result.success(count);
    }

    /**
     * 取消宿舍-班级绑定
     */
    @DeleteMapping("/unassign")
    @Operation(summary = "取消宿舍分配", description = "取消宿舍与班级的绑定关系")
    @PreAuthorize("hasAuthority('dormitory:department:manage')")
    public Result<Void> unassignDormitoryFromClass(
            @Parameter(description = "宿舍ID") @RequestParam Long dormitoryId,
            @Parameter(description = "班级ID") @RequestParam Long classId) {
        log.info("取消宿舍-班级绑定: dormitoryId={}, classId={}", dormitoryId, classId);

        // 验证宿舍属于当前部门
        validateDormitoryBelongsToDepartment(dormitoryId);

        // 验证班级属于当前部门
        validateClassBelongsToDepartment(classId);

        classService.removeDormitory(classId, dormitoryId);

        return Result.success();
    }

    /**
     * 获取部门宿舍统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取部门宿舍统计", description = "获取当前部门的宿舍统计信息")
    @PreAuthorize("hasAnyAuthority('dormitory:department:view', 'dormitory:department:manage')")
    public Result<DepartmentDormitoryStatistics> getStatistics() {
        Long orgUnitId = getCurrentUserOrgUnitId();
        log.info("获取部门宿舍统计: orgUnitId={}", orgUnitId);

        // 获取部门下所有宿舍
        QueryWrapper<Dormitory> dormitoryWrapper = new QueryWrapper<>();
        dormitoryWrapper.eq("org_unit_id", orgUnitId);
        List<Dormitory> dormitories = dormitoryMapper.selectList(dormitoryWrapper);

        // 获取部门下所有班级
        List<ClassResponse> classes = classService.getClassesByOrgUnitId(orgUnitId);

        // 计算统计数据
        DepartmentDormitoryStatistics stats = new DepartmentDormitoryStatistics();
        stats.setTotalDormitories(dormitories.size());
        stats.setTotalBeds(dormitories.stream()
                .mapToInt(d -> d.getBedCapacity() != null ? d.getBedCapacity() : 0)
                .sum());
        stats.setOccupiedBeds(dormitories.stream()
                .mapToInt(d -> d.getOccupiedBeds() != null ? d.getOccupiedBeds() : 0)
                .sum());
        stats.setTotalClasses(classes.size());

        // 计算已分配的宿舍数量
        if (!dormitories.isEmpty()) {
            List<Long> dormitoryIds = dormitories.stream()
                    .map(Dormitory::getId)
                    .collect(Collectors.toList());
            QueryWrapper<ClassDormitory> bindingWrapper = new QueryWrapper<>();
            bindingWrapper.in("dormitory_id", dormitoryIds);
            long assignedCount = classDormitoryMapper.selectCount(bindingWrapper);
            stats.setAssignedDormitories((int) assignedCount);
        } else {
            stats.setAssignedDormitories(0);
        }

        return Result.success(stats);
    }

    /**
     * 获取未分配给任何班级的宿舍
     */
    @GetMapping("/dormitories/unassigned")
    @Operation(summary = "获取未分配的宿舍", description = "获取当前部门下未分配给任何班级的宿舍")
    @PreAuthorize("hasAnyAuthority('dormitory:department:view', 'dormitory:department:manage')")
    public Result<List<DormitoryResponse>> getUnassignedDormitories() {
        Long orgUnitId = getCurrentUserOrgUnitId();
        log.info("获取未分配宿舍: orgUnitId={}", orgUnitId);

        // 获取部门下所有宿舍
        List<DormitoryResponse> allDormitories = dormitoryService.getDormitoriesByOrgUnitId(orgUnitId);

        if (allDormitories.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 获取已分配的宿舍ID
        List<Long> dormitoryIds = allDormitories.stream()
                .map(DormitoryResponse::getId)
                .collect(Collectors.toList());

        QueryWrapper<ClassDormitory> bindingWrapper = new QueryWrapper<>();
        bindingWrapper.in("dormitory_id", dormitoryIds);
        List<ClassDormitory> bindings = classDormitoryMapper.selectList(bindingWrapper);

        List<Long> assignedDormitoryIds = bindings.stream()
                .map(ClassDormitory::getDormitoryId)
                .distinct()
                .collect(Collectors.toList());

        // 过滤出未分配的宿舍
        List<DormitoryResponse> unassignedDormitories = allDormitories.stream()
                .filter(d -> !assignedDormitoryIds.contains(d.getId()))
                .collect(Collectors.toList());

        return Result.success(unassignedDormitories);
    }

    // ==================== 私有方法 ====================

    /**
     * 获取当前用户的组织单元ID
     */
    private Long getCurrentUserOrgUnitId() {
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            throw new BusinessException("用户未登录");
        }
        Long orgUnitId = userDetails.getOrgUnitId();
        if (orgUnitId == null) {
            throw new BusinessException("当前用户未分配到任何部门");
        }
        return orgUnitId;
    }

    /**
     * 验证宿舍是否属于当前用户的部门
     */
    private void validateDormitoryBelongsToDepartment(Long dormitoryId) {
        Long orgUnitId = getCurrentUserOrgUnitId();
        Dormitory dormitory = dormitoryMapper.selectById(dormitoryId);
        if (dormitory == null) {
            throw new BusinessException("宿舍不存在");
        }
        if (!orgUnitId.equals(dormitory.getOrgUnitId())) {
            throw new BusinessException("无权操作该宿舍，该宿舍不属于您的部门");
        }
    }

    /**
     * 验证班级是否属于当前用户的部门
     */
    private void validateClassBelongsToDepartment(Long classId) {
        Long orgUnitId = getCurrentUserOrgUnitId();
        Class clazz = classMapper.selectById(classId);
        if (clazz == null) {
            throw new BusinessException("班级不存在");
        }
        if (!orgUnitId.equals(clazz.getOrgUnitId())) {
            throw new BusinessException("无权操作该班级，该班级不属于您的部门");
        }
    }

    // ==================== DTO 类 ====================

    /**
     * 班级-宿舍绑定DTO
     */
    @Data
    public static class ClassDormitoryBindingDTO {
        private Long id;
        private Long classId;
        private String className;
        private String classCode;
        private Long dormitoryId;
        private String dormitoryNo;
        private Long buildingId;
        private String buildingName;
        private Integer bedCapacity;
        private Integer occupiedBeds;
        private Integer floorNumber;
        private Integer studentCount;
        private LocalDateTime createdTime;
    }

    /**
     * 分配宿舍请求
     */
    @Data
    public static class AssignDormitoryRequest {
        private Long dormitoryId;
        private Long classId;
        private Integer allocatedBeds;
    }

    /**
     * 批量分配请求
     */
    @Data
    public static class BatchAssignRequest {
        private Long classId;
        private List<Long> dormitoryIds;
    }

    /**
     * 部门宿舍统计
     */
    @Data
    public static class DepartmentDormitoryStatistics {
        private Integer totalDormitories;
        private Integer totalBeds;
        private Integer occupiedBeds;
        private Integer assignedDormitories;
        private Integer totalClasses;
    }
}
