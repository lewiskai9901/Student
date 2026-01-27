package com.school.management.service;

import com.school.management.entity.BuildingDepartment;

import java.util.List;

/**
 * 楼宇部门关联服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface BuildingDepartmentService {

    /**
     * 根据楼宇ID查询部门列表
     *
     * @param buildingId 楼宇ID
     * @return 部门列表
     */
    List<BuildingDepartment> getByBuildingId(Long buildingId);

    /**
     * 根据楼宇ID查询组织单元ID列表
     *
     * @param buildingId 楼宇ID
     * @return 组织单元ID列表
     */
    List<Long> getOrgUnitIdsByBuildingId(Long buildingId);

    /**
     * 为楼宇分配组织单元
     *
     * @param buildingId 楼宇ID
     * @param orgUnitIds 组织单元ID列表
     */
    void assignOrgUnits(Long buildingId, List<Long> orgUnitIds);

    /**
     * 移除楼宇的组织单元
     *
     * @param buildingId 楼宇ID
     * @param orgUnitId 组织单元ID
     */
    void removeOrgUnit(Long buildingId, Long orgUnitId);
}
