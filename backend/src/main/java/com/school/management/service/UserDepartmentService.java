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
     * 根据用户ID查询部门ID列表
     *
     * @param userId 用户ID
     * @return 部门ID列表
     */
    List<Long> getDepartmentIdsByUserId(Long userId);

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
     * @param departmentIds 部门ID列表
     * @param primaryDeptId 主部门ID
     */
    void assignDepartments(Long userId, List<Long> departmentIds, Long primaryDeptId);

    /**
     * 移除用户的部门
     *
     * @param userId 用户ID
     * @param departmentId 部门ID
     */
    void removeDepartment(Long userId, Long departmentId);

    /**
     * 设置主部门
     *
     * @param userId 用户ID
     * @param departmentId 部门ID
     */
    void setPrimaryDepartment(Long userId, Long departmentId);
}
