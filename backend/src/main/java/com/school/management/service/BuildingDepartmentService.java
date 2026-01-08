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
     * 根据楼宇ID查询部门ID列表
     *
     * @param buildingId 楼宇ID
     * @return 部门ID列表
     */
    List<Long> getDepartmentIdsByBuildingId(Long buildingId);

    /**
     * 为楼宇分配部门
     *
     * @param buildingId 楼宇ID
     * @param departmentIds 部门ID列表
     */
    void assignDepartments(Long buildingId, List<Long> departmentIds);

    /**
     * 移除楼宇的部门
     *
     * @param buildingId 楼宇ID
     * @param departmentId 部门ID
     */
    void removeDepartment(Long buildingId, Long departmentId);
}
