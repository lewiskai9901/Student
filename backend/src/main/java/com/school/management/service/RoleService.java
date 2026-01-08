package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.school.management.dto.RoleCreateRequest;
import com.school.management.dto.RoleQueryRequest;
import com.school.management.dto.RoleUpdateRequest;
import com.school.management.entity.Role;

import java.util.List;

/**
 * 角色管理服务接口
 *
 * @author system
 * @since 1.0.0
 */
public interface RoleService {

    /**
     * 创建角色
     *
     * @param request 创建请求
     * @return 角色ID
     */
    Long createRole(RoleCreateRequest request);

    /**
     * 更新角色
     *
     * @param request 更新请求
     */
    void updateRole(RoleUpdateRequest request);

    /**
     * 删除角色
     *
     * @param id 角色ID
     */
    void deleteRole(Long id);

    /**
     * 批量删除角色
     *
     * @param ids 角色ID列表
     */
    void deleteRoles(List<Long> ids);

    /**
     * 根据ID获取角色
     *
     * @param id 角色ID
     * @return 角色信息
     */
    Role getRoleById(Long id);

    /**
     * 根据角色编码获取角色
     *
     * @param roleCode 角色编码
     * @return 角色信息
     */
    Role getRoleByCode(String roleCode);

    /**
     * 分页查询角色
     *
     * @param request 查询请求
     * @return 分页结果
     */
    IPage<Role> getRolePage(RoleQueryRequest request);

    /**
     * 获取所有角色列表
     *
     * @return 角色列表
     */
    List<Role> getAllRoles();

    /**
     * 根据用户ID获取角色列表
     *
     * @param userId 用户ID
     * @return 角色列表
     */
    List<Role> getRolesByUserId(Long userId);

    /**
     * 检查角色编码是否存在
     *
     * @param roleCode 角色编码
     * @param excludeId 排除的角色ID
     * @return 是否存在
     */
    boolean existsRoleCode(String roleCode, Long excludeId);

    /**
     * 分配权限给角色
     *
     * @param roleId 角色ID
     * @param permissionIds 权限ID列表
     */
    void assignPermissions(Long roleId, List<Long> permissionIds);

    /**
     * 获取角色的权限ID列表
     *
     * @param roleId 角色ID
     * @return 权限ID列表
     */
    List<Long> getRolePermissionIds(Long roleId);

    /**
     * 分配用户给角色
     *
     * @param roleId 角色ID
     * @param userIds 用户ID列表
     */
    void assignUsers(Long roleId, List<Long> userIds);

    /**
     * 从角色中移除用户
     *
     * @param roleId 角色ID
     * @param userIds 用户ID列表
     */
    void removeUsers(Long roleId, List<Long> userIds);
}