package com.school.management.service;

import com.school.management.entity.UserDepartment;

import java.util.List;

/**
 * 用户部门关联服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface UserDepartmentService {

    /**
     * 根据用户ID查询组织单元ID列表
     *
     * @param userId 用户ID
     * @return 组织单元ID列表
     */
    List<Long> getOrgUnitIdsByUserId(Long userId);

    /**
     * 根据用户ID查询用户部门关联列表
     *
     * @param userId 用户ID
     * @return 用户部门关联列表
     */
    List<UserDepartment> getByUserId(Long userId);

    /**
     * 为用户分配部门
     *
     * @param userId 用户ID
     * @param orgUnitIds 组织单元ID列表
     * @param primaryDeptId 主组织单元ID
     */
    void assignDepartments(Long userId, List<Long> orgUnitIds, Long primaryDeptId);

    /**
     * 移除用户的部门
     *
     * @param userId 用户ID
     * @param orgUnitId 组织单元ID
     */
    void removeDepartment(Long userId, Long orgUnitId);

    /**
     * 设置主部门
     *
     * @param userId 用户ID
     * @param orgUnitId 组织单元ID
     */
    void setPrimaryDepartment(Long userId, Long orgUnitId);
}
