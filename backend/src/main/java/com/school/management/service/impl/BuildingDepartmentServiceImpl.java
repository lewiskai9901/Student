package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.school.management.entity.BuildingDepartment;
import com.school.management.mapper.BuildingDepartmentMapper;
import com.school.management.service.BuildingDepartmentService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 楼宇部门关联服务实现
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingDepartmentServiceImpl implements BuildingDepartmentService {

    private final BuildingDepartmentMapper buildingDepartmentMapper;

    @Override
    public List<BuildingDepartment> getByBuildingId(Long buildingId) {
        return buildingDepartmentMapper.selectByBuildingId(buildingId);
    }

    @Override
    public List<Long> getOrgUnitIdsByBuildingId(Long buildingId) {
        return buildingDepartmentMapper.selectOrgUnitIdsByBuildingId(buildingId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignOrgUnits(Long buildingId, List<Long> orgUnitIds) {
        // 如果组织单元列表为空或null，清空所有关联
        if (orgUnitIds == null || orgUnitIds.isEmpty()) {
            buildingDepartmentMapper.delete(new QueryWrapper<BuildingDepartment>()
                .eq("building_id", buildingId));
            log.info("清空楼宇 {} 的所有组织单元关联", buildingId);
            return;
        }

        // 删除现有关联
        buildingDepartmentMapper.delete(new QueryWrapper<BuildingDepartment>()
            .eq("building_id", buildingId));

        // 批量插入新关联
        Long currentUserId = SecurityUtils.getCurrentUserId();
        for (Long orgUnitId : orgUnitIds) {
            BuildingDepartment bd = new BuildingDepartment();
            bd.setBuildingId(buildingId);
            bd.setOrgUnitId(orgUnitId);
            bd.setCreatedBy(currentUserId);
            buildingDepartmentMapper.insert(bd);
        }

        log.info("为楼宇 {} 分配了 {} 个组织单元", buildingId, orgUnitIds.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeOrgUnit(Long buildingId, Long orgUnitId) {
        buildingDepartmentMapper.delete(new QueryWrapper<BuildingDepartment>()
            .eq("building_id", buildingId)
            .eq("org_unit_id", orgUnitId));

        log.info("移除楼宇 {} 的组织单元 {}", buildingId, orgUnitId);
    }
}
