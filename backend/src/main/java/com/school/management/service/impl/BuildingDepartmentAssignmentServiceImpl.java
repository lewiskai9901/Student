package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.request.BuildingDepartmentAssignmentCreateRequest;
import com.school.management.dto.request.BuildingDepartmentAssignmentUpdateRequest;
import com.school.management.entity.BuildingDepartmentAssignment;
import com.school.management.entity.DormitoryBuilding;
import com.school.management.entity.Department;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.BuildingDepartmentAssignmentMapper;
import com.school.management.mapper.DormitoryBuildingMapper;
import com.school.management.mapper.DepartmentMapper;
import com.school.management.service.BuildingDepartmentAssignmentService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 宿舍楼-院系分配服务实现
 *
 * @author system
 * @version 3.1.0
 * @since 2024-12-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingDepartmentAssignmentServiceImpl implements BuildingDepartmentAssignmentService {

    private final BuildingDepartmentAssignmentMapper assignmentMapper;
    private final DormitoryBuildingMapper buildingMapper;
    private final DepartmentMapper departmentMapper;

    @Override
    public IPage<BuildingDepartmentAssignment> page(Integer pageNum, Integer pageSize,
                                                     Long buildingId, Long departmentId, Integer status) {
        Page<BuildingDepartmentAssignment> page = new Page<>(pageNum, pageSize);
        return assignmentMapper.selectAssignmentPageWithDetails(page, buildingId, departmentId, status);
    }

    @Override
    public BuildingDepartmentAssignment getById(Long id) {
        BuildingDepartmentAssignment assignment = assignmentMapper.selectById(id);
        if (assignment == null || assignment.getDeleted() == 1) {
            throw new BusinessException("分配记录不存在");
        }
        return assignment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BuildingDepartmentAssignment create(BuildingDepartmentAssignmentCreateRequest request) {
        // 1. 验证楼层范围
        request.validateFloorRange();

        // 2. 验证宿舍楼是否存在
        DormitoryBuilding building = buildingMapper.selectById(request.getBuildingId());
        if (building == null || building.getDeleted() == 1) {
            throw new BusinessException("宿舍楼不存在");
        }

        // 3. 验证院系是否存在
        Department department = departmentMapper.selectById(request.getDepartmentId());
        if (department == null || department.getDeleted() == 1) {
            throw new BusinessException("院系不存在");
        }

        // 4. 检查楼层冲突（同一宿舍楼的同一院系，楼层范围不能重叠）
        if (hasFloorConflict(request.getBuildingId(), request.getDepartmentId(),
                request.getFloorStart(), request.getFloorEnd(), null)) {
            throw new BusinessException("该院系在此宿舍楼的楼层范围已存在分配，请检查楼层范围");
        }

        // 5. 创建分配记录
        BuildingDepartmentAssignment assignment = new BuildingDepartmentAssignment();
        BeanUtils.copyProperties(request, assignment);
        assignment.setStatus(1); // 默认启用
        if (assignment.getPriority() == null) {
            assignment.setPriority(0);
        }

        // 设置创建人
        Long currentUserId = SecurityUtils.getCurrentUserId();
        assignment.setCreatedBy(currentUserId);
        assignment.setUpdatedBy(currentUserId);

        assignmentMapper.insert(assignment);
        log.info("创建宿舍楼-院系分配成功: buildingId={}, departmentId={}, id={}",
                request.getBuildingId(), request.getDepartmentId(), assignment.getId());

        return assignment;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BuildingDepartmentAssignment update(BuildingDepartmentAssignmentUpdateRequest request) {
        // 1. 验证楼层范围
        request.validateFloorRange();

        // 2. 获取原记录
        BuildingDepartmentAssignment existing = getById(request.getId());

        // 3. 检查楼层冲突（排除自身）
        Integer floorStart = request.getFloorStart() != null ? request.getFloorStart() : existing.getFloorStart();
        Integer floorEnd = request.getFloorEnd() != null ? request.getFloorEnd() : existing.getFloorEnd();

        if (hasFloorConflict(existing.getBuildingId(), existing.getDepartmentId(),
                floorStart, floorEnd, request.getId())) {
            throw new BusinessException("该院系在此宿舍楼的楼层范围与其他分配冲突");
        }

        // 4. 更新记录
        if (request.getFloorStart() != null) {
            existing.setFloorStart(request.getFloorStart());
        }
        if (request.getFloorEnd() != null) {
            existing.setFloorEnd(request.getFloorEnd());
        }
        if (request.getAllocatedRooms() != null) {
            existing.setAllocatedRooms(request.getAllocatedRooms());
        }
        if (request.getAllocatedBeds() != null) {
            existing.setAllocatedBeds(request.getAllocatedBeds());
        }
        if (request.getPriority() != null) {
            existing.setPriority(request.getPriority());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            existing.setNotes(request.getNotes());
        }

        existing.setUpdatedBy(SecurityUtils.getCurrentUserId());
        assignmentMapper.updateById(existing);
        log.info("更新宿舍楼-院系分配成功: id={}", request.getId());

        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        BuildingDepartmentAssignment assignment = getById(id);

        // 使用MyBatis Plus的逻辑删除
        assignmentMapper.deleteById(id);

        log.info("删除宿舍楼-院系分配成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 使用MyBatis Plus的批量逻辑删除
        assignmentMapper.deleteBatchIds(ids);

        log.info("批量删除宿舍楼-院系分配成功: ids={}", ids);
    }

    @Override
    public List<BuildingDepartmentAssignment> getByBuildingId(Long buildingId) {
        return assignmentMapper.selectByBuildingId(buildingId);
    }

    @Override
    public List<BuildingDepartmentAssignment> getByDepartmentId(Long departmentId) {
        return assignmentMapper.selectByDepartmentId(departmentId);
    }

    @Override
    public List<BuildingDepartmentAssignment> getByBuildingAndFloor(Long buildingId, Integer floor) {
        return assignmentMapper.selectByBuildingAndFloor(buildingId, floor);
    }

    @Override
    public boolean hasFloorConflict(Long buildingId, Long departmentId,
                                    Integer floorStart, Integer floorEnd, Long excludeId) {
        int conflictCount = assignmentMapper.checkFloorConflict(
                buildingId, departmentId, floorStart, floorEnd, excludeId);
        return conflictCount > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enable(Long id) {
        BuildingDepartmentAssignment assignment = getById(id);
        if (Integer.valueOf(1).equals(assignment.getStatus())) {
            throw new BusinessException("该分配已处于启用状态");
        }

        assignment.setStatus(1);
        assignmentMapper.updateById(assignment);

        log.info("启用宿舍楼-院系分配成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disable(Long id) {
        BuildingDepartmentAssignment assignment = getById(id);
        if (Integer.valueOf(0).equals(assignment.getStatus())) {
            throw new BusinessException("该分配已处于禁用状态");
        }

        assignment.setStatus(0);
        assignmentMapper.updateById(assignment);

        log.info("禁用宿舍楼-院系分配成功: id={}", id);
    }
}
