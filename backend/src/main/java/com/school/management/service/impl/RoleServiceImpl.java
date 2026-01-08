package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.common.result.ResultCode;
import com.school.management.dto.RoleCreateRequest;
import com.school.management.dto.RoleQueryRequest;
import com.school.management.dto.RoleUpdateRequest;
import com.school.management.entity.Role;
import com.school.management.entity.RolePermission;
import com.school.management.entity.UserRole;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.RoleMapper;
import com.school.management.mapper.RolePermissionMapper;
import com.school.management.mapper.UserRoleMapper;
import com.school.management.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色管理服务实现类
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRole(RoleCreateRequest request) {
        log.info("创建角色: {}", request.getRoleCode());

        // 检查角色编码是否存在
        if (existsRoleCode(request.getRoleCode(), null)) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "角色编码已存在");
        }

        // 创建角色
        Role role = new Role();
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setRoleDesc(request.getRoleDesc());
        role.setSortOrder(request.getSortOrder());
        role.setStatus(request.getStatus());

        roleMapper.insert(role);

        // 分配权限
        if (!CollectionUtils.isEmpty(request.getPermissionIds())) {
            assignPermissions(role.getId(), request.getPermissionIds());
        }

        log.info("角色创建成功: {} - {}", role.getId(), request.getRoleCode());
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleUpdateRequest request) {
        log.info("更新角色: {}", request.getId());

        Role role = roleMapper.selectById(request.getId());
        if (role == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }

        // 检查角色编码是否重复
        if (!role.getRoleCode().equals(request.getRoleCode())
                && existsRoleCode(request.getRoleCode(), request.getId())) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "角色编码已存在");
        }

        // 更新角色信息
        role.setRoleName(request.getRoleName());
        role.setRoleCode(request.getRoleCode());
        role.setRoleDesc(request.getRoleDesc());
        role.setSortOrder(request.getSortOrder());
        role.setStatus(request.getStatus());
        role.setUpdatedAt(LocalDateTime.now());

        roleMapper.updateById(role);

        // 重新分配权限
        if (request.getPermissionIds() != null) {
            assignPermissions(role.getId(), request.getPermissionIds());
        }

        log.info("角色更新成功: {}", request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long id) {
        log.info("删除角色: {}", id);

        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }

        // 检查是否有用户使用该角色
        List<Long> userIds = userRoleMapper.selectUserIdsByRoleId(id);
        if (!CollectionUtils.isEmpty(userIds)) {
            throw new BusinessException(ResultCode.DATA_OPERATION_ERROR, "角色下还有用户，无法删除");
        }

        // 逻辑删除角色
        role.setDeleted(1);
        role.setUpdatedAt(LocalDateTime.now());
        roleMapper.updateById(role);

        // 删除角色权限关联
        rolePermissionMapper.deleteByRoleId(id);

        log.info("角色删除成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoles(List<Long> ids) {
        log.info("批量删除角色: {}", ids);

        for (Long id : ids) {
            deleteRole(id);
        }
    }

    @Override
    public Role getRoleById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public Role getRoleByCode(String roleCode) {
        return roleMapper.selectByRoleCode(roleCode);
    }

    @Override
    public IPage<Role> getRolePage(RoleQueryRequest request) {
        Page<Role> page = new Page<>(request.getPageNum(), request.getPageSize());
        return roleMapper.selectRolePage(page, request.getRoleName(),
                                       request.getRoleCode(), request.getStatus());
    }

    @Override
    public List<Role> getAllRoles() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Role::getStatus, 1);
        wrapper.eq(Role::getDeleted, 0);
        wrapper.orderByAsc(Role::getSortOrder);
        return roleMapper.selectList(wrapper);
    }

    @Override
    public List<Role> getRolesByUserId(Long userId) {
        return roleMapper.selectRolesByUserId(userId);
    }

    @Override
    public boolean existsRoleCode(String roleCode, Long excludeId) {
        return roleMapper.countByRoleCode(roleCode, excludeId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(Long roleId, List<Long> permissionIds) {
        log.info("为角色分配权限: {} -> {}", roleId, permissionIds);

        // 删除原有权限关联
        rolePermissionMapper.deleteByRoleId(roleId);

        // 添加新的权限关联
        if (!CollectionUtils.isEmpty(permissionIds)) {
            List<RolePermission> rolePermissions = permissionIds.stream()
                    .map(permissionId -> {
                        RolePermission rp = new RolePermission();
                        rp.setRoleId(roleId);
                        rp.setPermissionId(permissionId);
                        rp.setCreatedAt(LocalDateTime.now());
                        return rp;
                    })
                    .collect(Collectors.toList());

            rolePermissionMapper.insertBatch(rolePermissions);
        }

        log.info("角色权限分配成功: {}", roleId);
    }

    @Override
    public List<Long> getRolePermissionIds(Long roleId) {
        return rolePermissionMapper.selectPermissionIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignUsers(Long roleId, List<Long> userIds) {
        log.info("为角色分配用户: {} -> {}", roleId, userIds);

        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }

        // 获取已存在的用户ID
        List<Long> existingUserIds = userRoleMapper.selectUserIdsByRoleId(roleId);

        // 过滤出需要新增的用户ID
        List<Long> newUserIds = userIds.stream()
                .filter(userId -> !existingUserIds.contains(userId))
                .collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(newUserIds)) {
            List<UserRole> userRoles = newUserIds.stream()
                    .map(userId -> {
                        UserRole ur = new UserRole();
                        ur.setUserId(userId);
                        ur.setRoleId(roleId);
                        ur.setCreatedAt(LocalDateTime.now());
                        return ur;
                    })
                    .collect(Collectors.toList());

            userRoleMapper.insertBatch(userRoles);
        }

        log.info("角色用户分配成功: {}", roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeUsers(Long roleId, List<Long> userIds) {
        log.info("从角色中移除用户: {} -> {}", roleId, userIds);

        if (CollectionUtils.isEmpty(userIds)) {
            return;
        }

        // 删除用户角色关联
        LambdaQueryWrapper<UserRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRole::getRoleId, roleId);
        wrapper.in(UserRole::getUserId, userIds);
        userRoleMapper.delete(wrapper);

        log.info("角色用户移除成功: {}", roleId);
    }
}