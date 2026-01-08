package com.school.management.service.impl;

import com.school.management.entity.RoleDataPermission;
import com.school.management.entity.Student;
import com.school.management.enums.DataScope;
import com.school.management.mapper.ClassMapper;
import com.school.management.mapper.RoleDataPermissionMapper;
import com.school.management.mapper.StudentMapper;
import com.school.management.mapper.UserMapper;
import com.school.management.security.CustomUserDetails;
import com.school.management.service.DataPermissionService;
import com.school.management.service.UserDataScopeService;
import com.school.management.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 数据权限服务实现 V3.0 (旧版本，Casbin禁用时使用)
 *
 * 核心设计原则：角色与数据范围正交分离
 * - 角色 = 功能权限（能做什么操作）
 * - 数据范围 = 数据权限（能看什么数据），存储在 user_data_scopes 表
 *
 * 权限计算流程：
 * 1. 检查用户是否有"全部数据"权限（通过角色配置或超级管理员）
 * 2. 如果没有，则从 user_data_scopes 表获取用户的数据范围
 * 3. 根据数据范围计算可访问的班级/年级/部门ID列表
 *
 * @author system
 * @version 3.0.0
 * @since 2024-01-01
 * @deprecated 此实现已被 {@link com.school.management.casbin.service.CasbinDataPermissionService} 取代。
 *             当 casbin.enabled=true 时，系统将使用基于 Casbin 的新实现。
 *             此类仅在 casbin.enabled=false 时作为回退方案使用。
 *             计划在 V5.0 版本中移除。
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "casbin.enabled", havingValue = "false", matchIfMissing = true)
public class DataPermissionServiceImpl implements DataPermissionService {

    private final UserDataScopeService userDataScopeService;
    private final RoleDataPermissionMapper roleDataPermissionMapper;
    private final UserMapper userMapper;
    private final ClassMapper classMapper;
    private final StudentMapper studentMapper;

    /**
     * 获取用户的数据范围类型
     *
     * V3.0 逻辑：
     * 1. 先检查角色是否配置了"全部数据"权限
     * 2. 如果有，返回 ALL
     * 3. 否则返回 CUSTOM（表示使用 user_data_scopes 表的配置）
     */
    @Override
    public DataScope getDataScope(String moduleCode) {
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return DataScope.SELF;
        }

        // 检查是否有"全部数据"权限（通过角色配置）
        if (hasAllDataPermissionByRole(userDetails.getUserId(), moduleCode)) {
            return DataScope.ALL;
        }

        // 检查用户是否有任何数据范围配置
        if (userDataScopeService.hasAnyScope(userDetails.getUserId())) {
            // 有数据范围配置，返回 DEPARTMENT（表示使用自定义范围）
            // 注意：这里返回 DEPARTMENT 只是为了兼容旧代码，实际权限由 user_data_scopes 决定
            return DataScope.DEPARTMENT;
        }

        // 没有任何数据范围配置，返回仅本人
        return DataScope.SELF;
    }

    /**
     * 获取用户可访问的班级ID列表
     *
     * V3.0 逻辑：直接从 UserDataScopeService 获取
     */
    @Override
    public List<Long> getAccessibleClassIds(String moduleCode) {
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return Collections.emptyList();
        }

        Long userId = userDetails.getUserId();

        // 检查是否有"全部数据"权限
        if (hasAllDataPermissionByRole(userId, moduleCode)) {
            return null; // null 表示不限制
        }

        // 从 user_data_scopes 获取可访问的班级
        Set<Long> classIds = userDataScopeService.getAccessibleClassIds(userId);

        if (classIds == null) {
            return null; // null 表示不限制
        }

        if (classIds.isEmpty()) {
            // 没有配置数据范围，检查用户自己的班级
            if (userDetails.getClassId() != null) {
                return Collections.singletonList(userDetails.getClassId());
            }
            return Collections.emptyList();
        }

        return new ArrayList<>(classIds);
    }

    /**
     * 获取用户可访问的部门ID列表
     */
    @Override
    public List<Long> getAccessibleDepartmentIds(String moduleCode) {
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return Collections.emptyList();
        }

        Long userId = userDetails.getUserId();

        // 检查是否有"全部数据"权限
        if (hasAllDataPermissionByRole(userId, moduleCode)) {
            return null; // null 表示不限制
        }

        // 从 user_data_scopes 获取可访问的部门
        Set<Long> deptIds = userDataScopeService.getAccessibleDeptIds(userId);

        if (deptIds == null) {
            return null;
        }

        if (deptIds.isEmpty()) {
            // 没有配置数据范围，返回用户所属部门
            if (userDetails.getDepartmentId() != null) {
                return Collections.singletonList(userDetails.getDepartmentId());
            }
            return Collections.emptyList();
        }

        return new ArrayList<>(deptIds);
    }

    /**
     * 检查用户是否可以访问指定班级
     */
    @Override
    public boolean canAccessClass(Long classId, String moduleCode) {
        if (classId == null) {
            return true;
        }

        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return false;
        }

        // 检查是否有"全部数据"权限
        if (hasAllDataPermissionByRole(userDetails.getUserId(), moduleCode)) {
            return true;
        }

        // 使用 UserDataScopeService 检查
        return userDataScopeService.canAccessClass(userDetails.getUserId(), classId);
    }

    /**
     * 检查用户是否可以访问指定学生
     */
    @Override
    public boolean canAccessStudent(Long studentId, String moduleCode) {
        if (studentId == null) {
            return true;
        }

        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return false;
        }

        // 检查是否有"全部数据"权限
        if (hasAllDataPermissionByRole(userDetails.getUserId(), moduleCode)) {
            return true;
        }

        // 获取学生信息
        Student student = studentMapper.selectById(studentId);
        if (student == null) {
            return false;
        }

        // 检查是否是学生本人
        if (student.getUserId() != null && student.getUserId().equals(userDetails.getUserId())) {
            return true;
        }

        // 通过班级判断
        return canAccessClass(student.getClassId(), moduleCode);
    }

    /**
     * 检查用户是否有全部数据权限
     */
    @Override
    public boolean hasAllDataPermission(String moduleCode) {
        CustomUserDetails userDetails = SecurityUtils.getCurrentUserDetails();
        if (userDetails == null) {
            return false;
        }

        return hasAllDataPermissionByRole(userDetails.getUserId(), moduleCode);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 检查用户是否通过角色配置拥有"全部数据"权限
     *
     * 这是为了兼容现有的角色数据权限配置
     * 如果角色配置了 dataScope = 1 (ALL)，则用户拥有全部数据权限
     */
    private boolean hasAllDataPermissionByRole(Long userId, String moduleCode) {
        // 获取用户角色ID列表
        List<Long> roleIds = userMapper.findRoleIdsByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return false;
        }

        // 查询角色对应模块的数据权限配置
        List<RoleDataPermission> permissions = roleDataPermissionMapper.selectByRoleIdsAndModule(roleIds, moduleCode);
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        // 检查是否有"全部数据"权限（dataScope = 1）
        return permissions.stream()
                .anyMatch(p -> p.getDataScope() != null && p.getDataScope() == 1);
    }
}
