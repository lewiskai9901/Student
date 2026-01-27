package com.school.management.service;

import com.school.management.dto.UserDataScopeDTO;
import com.school.management.dto.request.UserDataScopeRequest;
import com.school.management.entity.UserDataScope;
import com.school.management.casbin.enums.ScopeType;

import java.util.List;
import java.util.Set;

/**
 * 用户数据范围服务接口
 *
 * 核心设计原则：
 * 1. 角色 = 功能权限（能做什么操作）
 * 2. 数据范围 = 数据权限（能看什么数据）
 * 3. 两者正交分离，互不干扰
 *
 * @author system
 * @version 3.0.0
 * @deprecated 用户级数据范围已废弃，请使用角色级数据权限配置 {@link com.school.management.service.RoleDataPermissionService}
 *             迁移说明：数据权限现在通过 role_data_permissions 和 role_custom_scope 表在角色级别配置
 */
@Deprecated(since = "4.0.0", forRemoval = true)
public interface UserDataScopeService {

    // ==================== 基础CRUD ====================

    /**
     * 添加用户数据范围
     *
     * @param request 请求参数
     * @return 新增的数据范围
     */
    UserDataScopeDTO addScope(UserDataScopeRequest request);

    /**
     * 批量添加用户数据范围
     *
     * @param request 批量添加请求
     * @return 新增的数据范围列表
     */
    List<UserDataScopeDTO> batchAddScopes(UserDataScopeRequest.BatchAdd request);

    /**
     * 删除用户数据范围
     *
     * @param id 数据范围ID
     */
    void deleteScope(Long id);

    /**
     * 批量删除用户数据范围
     *
     * @param ids ID列表
     */
    void batchDeleteScopes(List<Long> ids);

    /**
     * 删除用户的所有数据范围
     *
     * @param userId 用户ID
     */
    void deleteAllByUserId(Long userId);

    /**
     * 删除用户指定类型的数据范围
     *
     * @param userId 用户ID
     * @param scopeType 范围类型
     */
    void deleteByUserIdAndType(Long userId, ScopeType scopeType);

    // ==================== 查询方法 ====================

    /**
     * 获取用户的数据范围列表
     *
     * @param userId 用户ID
     * @return 数据范围DTO列表
     */
    List<UserDataScopeDTO> getScopesByUserId(Long userId);

    /**
     * 获取拥有指定范围的用户列表
     *
     * @param scopeType 范围类型
     * @param scopeId 范围ID
     * @return 数据范围DTO列表（包含用户信息）
     */
    List<UserDataScopeDTO> getUsersByScopeTypeAndId(ScopeType scopeType, Long scopeId);

    /**
     * 检查用户是否有指定范围的权限
     *
     * @param userId 用户ID
     * @param scopeType 范围类型
     * @param scopeId 范围ID
     * @return 是否有权限
     */
    boolean hasScope(Long userId, ScopeType scopeType, Long scopeId);

    // ==================== 权限计算方法 ====================

    /**
     * 获取用户可访问的所有部门ID
     *
     * @param userId 用户ID
     * @return 部门ID集合
     */
    Set<Long> getAccessibleDeptIds(Long userId);

    /**
     * 获取用户可访问的所有年级ID
     *
     * @param userId 用户ID
     * @return 年级ID集合
     */
    Set<Long> getAccessibleGradeIds(Long userId);

    /**
     * 获取用户可访问的所有班级ID
     *
     * @param userId 用户ID
     * @return 班级ID集合（null表示有全部权限）
     */
    Set<Long> getAccessibleClassIds(Long userId);

    /**
     * 检查用户是否可以访问指定班级
     *
     * @param userId 用户ID
     * @param classId 班级ID
     * @return 是否可以访问
     */
    boolean canAccessClass(Long userId, Long classId);

    /**
     * 检查用户是否可以访问指定年级
     *
     * @param userId 用户ID
     * @param gradeId 年级ID
     * @return 是否可以访问
     */
    boolean canAccessGrade(Long userId, Long gradeId);

    /**
     * 检查用户是否可以访问指定部门
     *
     * @param userId 用户ID
     * @param deptId 部门ID
     * @return 是否可以访问
     */
    boolean canAccessDept(Long userId, Long deptId);

    /**
     * 检查用户是否有任何数据范围配置
     *
     * @param userId 用户ID
     * @return 是否有数据范围配置
     */
    boolean hasAnyScope(Long userId);

    // ==================== 辅助方法 ====================

    /**
     * 当范围被删除时清理关联的用户数据范围
     * 例如：当班级被删除时，清理所有用户对该班级的数据范围
     *
     * @param scopeType 范围类型
     * @param scopeId 范围ID
     */
    void cleanupOnScopeDeleted(ScopeType scopeType, Long scopeId);

    /**
     * 更新范围名称
     * 当部门/年级/班级名称变更时调用
     *
     * @param scopeType 范围类型
     * @param scopeId 范围ID
     * @param newName 新名称
     */
    void updateScopeName(ScopeType scopeType, Long scopeId, String newName);
}
