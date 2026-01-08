package com.school.management.service;

import com.school.management.dto.DepartmentCreateRequest;
import com.school.management.dto.DepartmentResponse;
import com.school.management.entity.Department;

import java.util.List;

/**
 * 部门服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface DepartmentService {

    /**
     * 创建部门
     *
     * @param request 创建请求
     * @return 部门ID
     */
    Long createDepartment(DepartmentCreateRequest request);

    /**
     * 更新部门
     *
     * @param id 部门ID
     * @param request 更新请求
     */
    void updateDepartment(Long id, DepartmentCreateRequest request);

    /**
     * 删除部门
     *
     * @param id 部门ID
     */
    void deleteDepartment(Long id);

    /**
     * 根据ID获取部门
     *
     * @param id 部门ID
     * @return 部门信息
     */
    DepartmentResponse getDepartmentById(Long id);

    /**
     * 根据部门编码获取部门
     *
     * @param deptCode 部门编码
     * @return 部门
     */
    Department getDepartmentByCode(String deptCode);

    /**
     * 分页查询部门
     *
     * @param pageNum 页码
     * @param pageSize 每页数量
     * @param deptName 部门名称
     * @param deptCode 部门编码
     * @param status 状态
     * @return 分页结果
     */
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<DepartmentResponse> page(
            Integer pageNum, Integer pageSize, String deptName, String deptCode, Integer status);

    /**
     * 获取部门树形结构
     *
     * @return 部门树
     */
    List<DepartmentResponse> getDepartmentTree();

    /**
     * 获取所有启用的部门
     *
     * @return 部门列表
     */
    List<DepartmentResponse> getAllEnabledDepartments();

    /**
     * 根据父部门ID查询子部门
     *
     * @param parentId 父部门ID
     * @return 部门列表
     */
    List<DepartmentResponse> getDepartmentsByParentId(Long parentId);

    /**
     * 检查部门编码是否存在
     *
     * @param deptCode 部门编码
     * @param excludeId 排除的ID
     * @return 是否存在
     */
    boolean existsDeptCode(String deptCode, Long excludeId);

    /**
     * 更新部门状态
     *
     * @param id 部门ID
     * @param status 状态
     */
    void updateStatus(Long id, Integer status);
}