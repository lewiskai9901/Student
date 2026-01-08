package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.dto.RoleDataPermissionDTO;
import com.school.management.entity.RoleDataPermission;
import com.school.management.mapper.RoleDataPermissionMapper;
import com.school.management.service.RoleDataPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色数据权限服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoleDataPermissionServiceImpl implements RoleDataPermissionService {

    private final RoleDataPermissionMapper roleDataPermissionMapper;

    @Override
    public List<RoleDataPermissionDTO> getRoleDataPermissions(Long roleId) {
        // 获取所有模块
        List<RoleDataPermissionDTO> result = getAllModules();

        // 查询角色已配置的数据权限
        List<RoleDataPermission> existingPermissions = roleDataPermissionMapper.selectByRoleId(roleId);
        Map<String, RoleDataPermission> permissionMap = existingPermissions.stream()
                .collect(Collectors.toMap(RoleDataPermission::getModuleCode, p -> p));

        // 合并配置
        for (RoleDataPermissionDTO dto : result) {
            RoleDataPermission existing = permissionMap.get(dto.getModuleCode());
            if (existing != null) {
                dto.setDataScope(existing.getDataScope());
                dto.setDataScopeName(RoleDataPermissionDTO.getDataScopeName(existing.getDataScope()));
                dto.setCustomDeptIds(existing.getCustomDeptIds());
                dto.setCustomClassIds(existing.getCustomClassIds());
            } else {
                // 默认为仅本人
                dto.setDataScope(5);
                dto.setDataScopeName(RoleDataPermissionDTO.getDataScopeName(5));
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleDataPermissions(Long roleId, List<RoleDataPermissionDTO> permissions) {
        log.info("保存角色数据权限: roleId={}, permissions={}", roleId, permissions.size());

        // 删除原有配置
        LambdaQueryWrapper<RoleDataPermission> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(RoleDataPermission::getRoleId, roleId);
        roleDataPermissionMapper.delete(deleteWrapper);

        // 插入新配置
        for (RoleDataPermissionDTO dto : permissions) {
            if (dto.getDataScope() == null) {
                continue;
            }

            RoleDataPermission entity = new RoleDataPermission();
            entity.setRoleId(roleId);
            entity.setModuleCode(dto.getModuleCode());
            entity.setDataScope(dto.getDataScope());
            entity.setCustomDeptIds(dto.getCustomDeptIds());
            entity.setCustomClassIds(dto.getCustomClassIds());

            roleDataPermissionMapper.insert(entity);
        }

        log.info("角色数据权限保存成功: roleId={}", roleId);
    }

    @Override
    public List<RoleDataPermissionDTO> getAllModules() {
        List<RoleDataPermissionDTO> modules = new ArrayList<>();

        for (RoleDataPermissionDTO.Module module : RoleDataPermissionDTO.Module.values()) {
            RoleDataPermissionDTO dto = RoleDataPermissionDTO.builder()
                    .moduleCode(module.getCode())
                    .moduleName(module.getName())
                    .dataScope(5)  // 默认仅本人
                    .dataScopeName(RoleDataPermissionDTO.getDataScopeName(5))
                    .build();
            modules.add(dto);
        }

        return modules;
    }
}
