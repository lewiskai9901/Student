package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.BuildingDormitoryDTO;
import com.school.management.entity.*;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.*;
import com.school.management.service.BuildingDormitoryService;
import com.school.management.service.PermissionService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 宿舍楼管理服务实现
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingDormitoryServiceImpl implements BuildingDormitoryService {

    private final BuildingDormitoryMapper buildingDormitoryMapper;
    private final DormitoryBuildingManagerMapper managerMapper;
    private final BuildingDepartmentMapper buildingDepartmentMapper;
    private final UserDepartmentMapper userDepartmentMapper;
    private final DormitoryMapper dormitoryMapper;
    private final PermissionService permissionService;

    @Override
    public IPage<BuildingDormitory> page(Integer pageNum, Integer pageSize,
                                          String buildingName, Integer dormitoryType) {
        // 1. 获取当前用户
        Long userId = SecurityUtils.getCurrentUserId();

        // 2. 检查是否为超级管理员
        boolean isSuperAdmin = permissionService.hasPermission(userId, "system:admin");
        log.info("查询宿舍楼列表 - 用户ID: {}, 是否超管: {}", userId, isSuperAdmin);

        // 3. 构建数据权限过滤条件
        List<Long> orgUnitIds = null;
        List<Long> managedBuildingIds = null;

        if (!isSuperAdmin) {
            // 获取用户的所有组织单元
            orgUnitIds = userDepartmentMapper.selectOrgUnitIdsByUserId(userId);

            // 获取用户管理的楼宇
            managedBuildingIds = managerMapper.selectBuildingIdsByUserId(userId);

            log.info("非超管权限过滤 - 组织单元IDs: {}, 管理楼宇IDs: {}", orgUnitIds, managedBuildingIds);

            // 如果用户既没有组织单元也不是任何楼宇的管理员，返回空结果
            if ((orgUnitIds == null || orgUnitIds.isEmpty()) &&
                (managedBuildingIds == null || managedBuildingIds.isEmpty())) {
                log.warn("用户{}既无组织单元也不是楼宇管理员,返回空结果", userId);
                return new Page<>(pageNum, pageSize);
            }
        }

        // 4. 分页查询
        Page<BuildingDormitory> page = new Page<>(pageNum, pageSize);
        log.info("执行查询 - buildingName: {}, dormitoryType: {}, orgUnitIds: {}, managedBuildingIds: {}",
            buildingName, dormitoryType, orgUnitIds, managedBuildingIds);
        IPage<BuildingDormitory> result = buildingDormitoryMapper.selectDormitoryBuildingPage(
            page, buildingName, dormitoryType, orgUnitIds, managedBuildingIds
        );
        log.info("查询结果 - 总数: {}, 当前页记录数: {}", result.getTotal(), result.getRecords().size());

        // 5. 填充管理员信息
        for (BuildingDormitory item : result.getRecords()) {
            List<DormitoryBuildingManager> managers = managerMapper.selectByBuildingId(item.getBuildingId());
            item.setManagerIds(managers.stream()
                .map(DormitoryBuildingManager::getUserId)
                .collect(Collectors.toList()));
            item.setManagerNames(managers.stream()
                .map(DormitoryBuildingManager::getUserName)
                .collect(Collectors.toList()));
        }

        return result;
    }

    @Override
    public BuildingDormitory getById(Long id) {
        BuildingDormitory dormitory = buildingDormitoryMapper.selectById(id);
        if (dormitory == null) {
            throw new BusinessException("宿舍楼不存在");
        }

        // 填充管理员信息
        List<DormitoryBuildingManager> managers = managerMapper.selectByBuildingId(dormitory.getBuildingId());
        dormitory.setManagerIds(managers.stream()
            .map(DormitoryBuildingManager::getUserId)
            .collect(Collectors.toList()));
        dormitory.setManagerNames(managers.stream()
            .map(DormitoryBuildingManager::getUserName)
            .collect(Collectors.toList()));

        return dormitory;
    }

    @Override
    public BuildingDormitory getByBuildingId(Long buildingId) {
        BuildingDormitory dormitory = buildingDormitoryMapper.selectByBuildingId(buildingId);
        if (dormitory == null) {
            throw new BusinessException("该楼宇不是宿舍楼");
        }

        // 填充管理员信息
        List<DormitoryBuildingManager> managers = managerMapper.selectByBuildingId(buildingId);
        dormitory.setManagerIds(managers.stream()
            .map(DormitoryBuildingManager::getUserId)
            .collect(Collectors.toList()));
        dormitory.setManagerNames(managers.stream()
            .map(DormitoryBuildingManager::getUserName)
            .collect(Collectors.toList()));

        return dormitory;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BuildingDormitory update(Long id, BuildingDormitoryDTO dto) {
        BuildingDormitory dormitory = buildingDormitoryMapper.selectById(id);
        if (dormitory == null) {
            throw new BusinessException("宿舍楼不存在");
        }

        // 1. 验证编辑权限
        Long userId = SecurityUtils.getCurrentUserId();
        validateEditPermission(dormitory.getBuildingId(), userId);

        // 2. 验证宿舍楼类型修改
        if (dto.getDormitoryType() != null &&
            !dto.getDormitoryType().equals(dormitory.getDormitoryType())) {
            validateDormitoryTypeChange(dormitory.getBuildingId(), dto.getDormitoryType());
        }

        // 3. 更新基本信息
        BeanUtils.copyProperties(dto, dormitory, "id", "buildingId", "totalRooms", "occupiedRooms", "createdAt", "createdBy");
        dormitory.setUpdatedBy(userId);
        buildingDormitoryMapper.updateById(dormitory);

        // 4. 更新管理员
        if (dto.getManagerIds() != null) {
            assignManagers(dormitory.getBuildingId(), dto.getManagerIds());
        }

        log.info("更新宿舍楼成功: {}", id);
        return buildingDormitoryMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignManagers(Long buildingId, List<Long> managerIds) {
        // 1. 验证管理员数量
        if (managerIds == null || managerIds.isEmpty()) {
            // 允许清空管理员
            managerMapper.delete(new QueryWrapper<DormitoryBuildingManager>()
                .eq("building_id", buildingId));
            log.info("清空宿舍楼 {} 的管理员", buildingId);
            return;
        }

        if (managerIds.size() > 5) {
            throw new BusinessException("最多添加5个管理员");
        }

        // 2. 验证所有用户都有宿舍管理权限
        for (Long managerId : managerIds) {
            if (!hasDormitoryPermission(managerId)) {
                throw new BusinessException("用户ID " + managerId + " 没有宿舍管理权限，无法分配为管理员");
            }
        }

        // 3. 删除现有管理员
        managerMapper.delete(new QueryWrapper<DormitoryBuildingManager>()
            .eq("building_id", buildingId));

        // 4. 批量插入新管理员
        Long currentUserId = SecurityUtils.getCurrentUserId();
        for (Long managerId : managerIds) {
            DormitoryBuildingManager manager = new DormitoryBuildingManager();
            manager.setBuildingId(buildingId);
            manager.setUserId(managerId);
            manager.setCreatedBy(currentUserId);
            managerMapper.insert(manager);
        }

        log.info("为宿舍楼 {} 分配了 {} 个管理员", buildingId, managerIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeManager(Long buildingId, Long userId) {
        managerMapper.delete(new QueryWrapper<DormitoryBuildingManager>()
            .eq("building_id", buildingId)
            .eq("user_id", userId));

        log.info("移除宿舍楼 {} 的管理员 {}", buildingId, userId);
    }

    @Override
    public boolean hasDormitoryPermission(Long userId) {
        // 检查用户是否有 student:dormitory:view 和 student:dormitory:edit 权限
        return permissionService.hasPermission(userId, "student:dormitory:view") &&
               permissionService.hasPermission(userId, "student:dormitory:edit");
    }

    @Override
    public boolean isManager(Long buildingId, Long userId) {
        return managerMapper.isManager(buildingId, userId);
    }

    @Override
    public void validateDormitoryTypeChange(Long buildingId, Integer newDormitoryType) {
        // 检查是否有宿舍房间
        LambdaQueryWrapper<Dormitory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dormitory::getBuildingId, buildingId);
        long roomCount = dormitoryMapper.selectCount(wrapper);

        if (roomCount > 0) {
            throw new BusinessException("该楼宇下存在 " + roomCount + " 个宿舍房间，无法修改宿舍类型");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementRoomCount(Long buildingId) {
        buildingDormitoryMapper.incrementTotalRooms(buildingId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrementRoomCount(Long buildingId) {
        buildingDormitoryMapper.decrementTotalRooms(buildingId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementBedCount(Long buildingId, Integer bedCount) {
        if (bedCount != null && bedCount > 0) {
            buildingDormitoryMapper.incrementTotalBeds(buildingId, bedCount);
            log.info("宿舍楼 {} 增加了 {} 个床位", buildingId, bedCount);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrementBedCount(Long buildingId, Integer bedCount) {
        if (bedCount != null && bedCount > 0) {
            buildingDormitoryMapper.decrementTotalBeds(buildingId, bedCount);
            log.info("宿舍楼 {} 减少了 {} 个床位", buildingId, bedCount);
        }
    }

    /**
     * 验证编辑权限
     */
    private void validateEditPermission(Long buildingId, Long userId) {
        // 1. 超级管理员直接通过
        if (permissionService.hasPermission(userId, "system:admin")) {
            return;
        }

        // 2. 检查是否有 system:dormitory_building:edit 权限
        if (!permissionService.hasPermission(userId, "system:dormitory_building:edit")) {
            throw new BusinessException("无权限编辑宿舍楼");
        }

        // 3. 检查是否为该楼宇的管理员
        boolean isManager = managerMapper.isManager(buildingId, userId);
        if (!isManager) {
            throw new BusinessException("您不是该宿舍楼的管理员，无法编辑");
        }
    }
}
