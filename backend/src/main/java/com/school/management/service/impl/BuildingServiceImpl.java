package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.dto.BuildingRequest;
import com.school.management.entity.*;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.*;
import com.school.management.service.BuildingService;
import com.school.management.service.BuildingDepartmentService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 楼宇服务实现
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingServiceImpl implements BuildingService {

    private final BuildingMapper buildingMapper;
    private final ClassroomMapper classroomMapper;
    private final DormitoryMapper dormitoryMapper;
    private final BuildingDormitoryMapper buildingDormitoryMapper;
    private final BuildingTeachingMapper buildingTeachingMapper;
    private final DormitoryBuildingManagerMapper managerMapper;
    private final BuildingDepartmentService buildingDepartmentService;

    @Override
    public IPage<Building> page(Integer pageNum, Integer pageSize, Integer buildingType,
                                String buildingNo, String buildingName, Integer status) {
        Page<Building> page = new Page<>(pageNum, pageSize);
        IPage<Building> result = buildingMapper.selectBuildingPage(page, buildingType, buildingNo, buildingName, status);

        // 为每个楼宇加载部门ID列表
        result.getRecords().forEach(building -> {
            List<Long> departmentIds = buildingDepartmentService.getDepartmentIdsByBuildingId(building.getId());
            building.setDepartmentIds(departmentIds);
        });

        return result;
    }

    @Override
    public Building getById(Long id) {
        Building building = buildingMapper.selectById(id);
        if (building == null) {
            throw new BusinessException("楼宇不存在");
        }

        // 加载部门关联信息
        List<Long> departmentIds = buildingDepartmentService.getDepartmentIdsByBuildingId(id);
        building.setDepartmentIds(departmentIds);

        return building;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Building create(BuildingRequest request) {
        // 检查楼号是否已存在
        if (existsBuildingNo(request.getBuildingNo(), null)) {
            throw new BusinessException("楼号已存在");
        }

        // 1. 创建楼宇基本信息
        Building building = new Building();
        BeanUtils.copyProperties(request, building);
        buildingMapper.insert(building);

        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 2. 根据楼宇类型自动创建扩展表记录
        if (building.getBuildingType() == 2) {
            // 宿舍楼 - 创建宿舍楼扩展记录
            BuildingDormitory dormitory = new BuildingDormitory();
            dormitory.setBuildingId(building.getId());
            dormitory.setCreatedBy(currentUserId);
            buildingDormitoryMapper.insert(dormitory);
            log.info("自动创建宿舍楼扩展记录: buildingId={}", building.getId());
        } else if (building.getBuildingType() == 1) {
            // 教学楼 - 创建教学楼扩展记录
            BuildingTeaching teaching = new BuildingTeaching();
            teaching.setBuildingId(building.getId());
            teaching.setCreatedBy(currentUserId);
            buildingTeachingMapper.insert(teaching);
            log.info("自动创建教学楼扩展记录: buildingId={}", building.getId());
        }

        // 3. 关联部门（如果有）
        if (request.getDepartmentIds() != null && !request.getDepartmentIds().isEmpty()) {
            buildingDepartmentService.assignDepartments(building.getId(), request.getDepartmentIds());
        }

        log.info("创建楼宇成功: {}", building.getId());
        return building;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Building update(Long id, BuildingRequest request) {
        Building building = getById(id);

        // 检查楼号是否已存在(排除当前记录)
        if (!building.getBuildingNo().equals(request.getBuildingNo()) &&
                existsBuildingNo(request.getBuildingNo(), id)) {
            throw new BusinessException("楼号已存在");
        }

        Integer oldType = building.getBuildingType();
        Integer newType = request.getBuildingType();

        // 验证楼宇类型是否可修改
        if (!oldType.equals(newType)) {
            validateAndHandleBuildingTypeChange(id, oldType, newType);
        }

        // 更新楼宇基本信息
        BeanUtils.copyProperties(request, building);
        building.setId(id);
        building.setUpdatedBy(SecurityUtils.getCurrentUserId());
        buildingMapper.updateById(building);

        // 更新部门关联
        if (request.getDepartmentIds() != null) {
            buildingDepartmentService.assignDepartments(id, request.getDepartmentIds());
        }

        log.info("更新楼宇成功: {}", id);
        return building;
    }

    /**
     * 验证并处理楼宇类型修改
     */
    private void validateAndHandleBuildingTypeChange(Long buildingId, Integer oldType, Integer newType) {
        Long currentUserId = SecurityUtils.getCurrentUserId();

        // 从宿舍楼改为其他类型
        if (oldType == 2) {
            // 检查是否有宿舍
            long dormCount = dormitoryMapper.selectCount(new LambdaQueryWrapper<Dormitory>()
                .eq(Dormitory::getBuildingId, buildingId));
            if (dormCount > 0) {
                throw new BusinessException("该楼宇下存在 " + dormCount + " 个宿舍，无法修改类型");
            }

            // 删除宿舍楼扩展记录和管理员
            buildingDormitoryMapper.delete(new QueryWrapper<BuildingDormitory>()
                .eq("building_id", buildingId));
            managerMapper.delete(new QueryWrapper<DormitoryBuildingManager>()
                .eq("building_id", buildingId));
            log.info("删除宿舍楼扩展记录: buildingId={}", buildingId);
        }

        // 从教学楼改为其他类型
        if (oldType == 1) {
            // 检查是否有教室
            long classroomCount = classroomMapper.selectCount(new LambdaQueryWrapper<Classroom>()
                .eq(Classroom::getBuildingId, buildingId));
            if (classroomCount > 0) {
                throw new BusinessException("该楼宇下存在 " + classroomCount + " 个教室，无法修改类型");
            }

            // 删除教学楼扩展记录
            buildingTeachingMapper.delete(new QueryWrapper<BuildingTeaching>()
                .eq("building_id", buildingId));
            log.info("删除教学楼扩展记录: buildingId={}", buildingId);
        }

        // 创建新类型的扩展记录
        if (newType == 2) {
            BuildingDormitory dormitory = new BuildingDormitory();
            dormitory.setBuildingId(buildingId);
            dormitory.setCreatedBy(currentUserId);
            buildingDormitoryMapper.insert(dormitory);
            log.info("创建宿舍楼扩展记录: buildingId={}", buildingId);
        } else if (newType == 1) {
            BuildingTeaching teaching = new BuildingTeaching();
            teaching.setBuildingId(buildingId);
            teaching.setCreatedBy(currentUserId);
            buildingTeachingMapper.insert(teaching);
            log.info("创建教学楼扩展记录: buildingId={}", buildingId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Building building = getById(id);

        // 检查是否有关联的房间
        if (building.getBuildingType() == 1) {
            // 教学楼 - 检查是否有教室
            LambdaQueryWrapper<Classroom> classroomWrapper = new LambdaQueryWrapper<>();
            classroomWrapper.eq(Classroom::getBuildingId, id);
            long classroomCount = classroomMapper.selectCount(classroomWrapper);
            if (classroomCount > 0) {
                throw new BusinessException("该楼宇下还有教室,无法删除");
            }
        } else if (building.getBuildingType() == 2) {
            // 宿舍楼 - 检查是否有宿舍
            LambdaQueryWrapper<Dormitory> dormitoryWrapper = new LambdaQueryWrapper<>();
            dormitoryWrapper.eq(Dormitory::getBuildingId, id);
            long dormitoryCount = dormitoryMapper.selectCount(dormitoryWrapper);
            if (dormitoryCount > 0) {
                throw new BusinessException("该楼宇下还有宿舍,无法删除");
            }
        }

        buildingMapper.deleteById(id);
        log.info("删除楼宇成功: {}", id);
    }

    @Override
    public List<Building> getAllEnabled(Integer buildingType) {
        LambdaQueryWrapper<Building> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Building::getStatus, 1);
        if (buildingType != null) {
            wrapper.eq(Building::getBuildingType, buildingType);
        }
        wrapper.orderByAsc(Building::getBuildingNo);
        return buildingMapper.selectList(wrapper);
    }

    @Override
    public boolean existsBuildingNo(String buildingNo, Long excludeId) {
        return buildingMapper.countByBuildingNo(buildingNo, excludeId) > 0;
    }
}
