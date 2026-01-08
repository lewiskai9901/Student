package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.school.management.common.result.ResultCode;
import com.school.management.dto.PermissionCreateRequest;
import com.school.management.dto.PermissionUpdateRequest;
import com.school.management.entity.Permission;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.PermissionMapper;
import com.school.management.mapper.RolePermissionMapper;
import com.school.management.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限管理服务实现类
 *
 * @author system
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionService {

    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPermission(PermissionCreateRequest request) {
        log.info("创建权限: {}", request.getPermissionCode());

        // 检查权限编码是否存在
        if (existsPermissionCode(request.getPermissionCode(), null)) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "权限编码已存在");
        }

        // 创建权限
        Permission permission = new Permission();
        permission.setPermissionName(request.getPermissionName());
        permission.setPermissionCode(request.getPermissionCode());
        permission.setPermissionDesc(request.getPermissionDesc());
        permission.setResourceType(request.getResourceType());
        permission.setParentId(request.getParentId());
        permission.setPath(request.getPath());
        permission.setComponent(request.getComponent());
        permission.setIcon(request.getIcon());
        permission.setSortOrder(request.getSortOrder());
        permission.setStatus(request.getStatus());

        permissionMapper.insert(permission);

        log.info("权限创建成功: {} - {}", permission.getId(), request.getPermissionCode());
        return permission.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermission(PermissionUpdateRequest request) {
        log.info("更新权限: {}", request.getId());

        Permission permission = permissionMapper.selectById(request.getId());
        if (permission == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "权限不存在");
        }

        // 检查权限编码是否重复
        if (!permission.getPermissionCode().equals(request.getPermissionCode())
                && existsPermissionCode(request.getPermissionCode(), request.getId())) {
            throw new BusinessException(ResultCode.DATA_EXISTS, "权限编码已存在");
        }

        // 检查是否设置自己为父权限
        if (request.getParentId() != null && request.getParentId().equals(request.getId())) {
            throw new BusinessException(ResultCode.DATA_INVALID, "不能设置自己为父权限");
        }

        // 更新权限信息
        permission.setPermissionName(request.getPermissionName());
        permission.setPermissionCode(request.getPermissionCode());
        permission.setPermissionDesc(request.getPermissionDesc());
        permission.setResourceType(request.getResourceType());
        permission.setParentId(request.getParentId());
        permission.setPath(request.getPath());
        permission.setComponent(request.getComponent());
        permission.setIcon(request.getIcon());
        permission.setSortOrder(request.getSortOrder());
        permission.setStatus(request.getStatus());
        permission.setUpdatedAt(LocalDateTime.now());

        permissionMapper.updateById(permission);

        log.info("权限更新成功: {}", request.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(Long id) {
        log.info("删除权限: {}", id);

        Permission permission = permissionMapper.selectById(id);
        if (permission == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "权限不存在");
        }

        // 检查是否有子权限
        List<Permission> children = getPermissionsByParentId(id);
        if (!CollectionUtils.isEmpty(children)) {
            throw new BusinessException(ResultCode.DATA_OPERATION_ERROR, "权限下还有子权限，无法删除");
        }

        // 逻辑删除权限
        permission.setDeleted(1);
        permission.setUpdatedAt(LocalDateTime.now());
        permissionMapper.updateById(permission);

        // 删除角色权限关联
        rolePermissionMapper.deleteByPermissionId(id);

        log.info("权限删除成功: {}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermissions(List<Long> ids) {
        log.info("批量删除权限: {}", ids);

        for (Long id : ids) {
            deletePermission(id);
        }
    }

    @Override
    public Permission getPermissionById(Long id) {
        return permissionMapper.selectById(id);
    }

    @Override
    public Permission getPermissionByCode(String permissionCode) {
        return permissionMapper.selectByPermissionCode(permissionCode);
    }

    @Override
    public List<Permission> getPermissionTree() {
        List<Permission> allPermissions = permissionMapper.selectPermissionTree();
        return buildPermissionTree(allPermissions, 0L);
    }

    @Override
    public List<Permission> getAllPermissions() {
        LambdaQueryWrapper<Permission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Permission::getDeleted, 0);
        wrapper.orderByAsc(Permission::getSortOrder);
        return permissionMapper.selectList(wrapper);
    }

    @Override
    public List<Permission> getPermissionsByUserId(Long userId) {
        return permissionMapper.selectPermissionsByUserId(userId);
    }

    @Override
    public List<Permission> getPermissionsByRoleId(Long roleId) {
        return permissionMapper.selectPermissionsByRoleId(roleId);
    }

    @Override
    public boolean existsPermissionCode(String permissionCode, Long excludeId) {
        return permissionMapper.countByPermissionCode(permissionCode, excludeId) > 0;
    }

    @Override
    public List<Permission> getPermissionsByParentId(Long parentId) {
        return permissionMapper.selectByParentId(parentId);
    }

    @Override
    public List<Permission> buildPermissionTree(List<Permission> permissions, Long parentId) {
        if (CollectionUtils.isEmpty(permissions)) {
            return new ArrayList<>();
        }

        List<Permission> result = new ArrayList<>();

        for (Permission permission : permissions) {
            Long currentParentId = permission.getParentId() == null ? 0L : permission.getParentId();
            if (currentParentId.equals(parentId)) {
                // 递归查找子权限
                List<Permission> children = buildPermissionTree(permissions, permission.getId());
                // 设置children字段
                if (!CollectionUtils.isEmpty(children)) {
                    permission.setChildren(children);
                }
                result.add(permission);
            }
        }

        return result;
    }

    @Override
    public List<String> getUserPermissionCodes(Long userId) {
        List<Permission> permissions = getPermissionsByUserId(userId);
        return permissions.stream()
                .map(Permission::getPermissionCode)
                .collect(Collectors.toList());
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        List<String> userPermissions = getUserPermissionCodes(userId);
        return userPermissions.contains(permissionCode);
    }
}