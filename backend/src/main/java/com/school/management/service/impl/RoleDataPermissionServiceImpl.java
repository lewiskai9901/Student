package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.access.model.DataModule;
import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.RoleCustomScope;
import com.school.management.domain.access.repository.RoleCustomScopeRepository;
import com.school.management.dto.RoleDataPermissionDTO;
import com.school.management.entity.Role;
import com.school.management.entity.RoleDataPermission;
import com.school.management.mapper.RoleDataPermissionMapper;
import com.school.management.mapper.RoleMapper;
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
    private final RoleMapper roleMapper;
    private final RoleCustomScopeRepository customScopeRepository;

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

    // ==================== V2 API Methods ====================

    @Override
    public RoleDataPermissionDTO.RolePermissionConfig getRolePermissionConfigV2(Long roleId) {
        // 获取角色信息
        Role role = roleMapper.selectById(roleId);
        String roleName = role != null ? role.getName() : "";

        // 获取所有模块权限
        List<RoleDataPermission> existingPermissions = roleDataPermissionMapper.selectByRoleId(roleId);
        Map<String, RoleDataPermission> permissionMap = existingPermissions.stream()
                .collect(Collectors.toMap(RoleDataPermission::getModuleCode, p -> p, (a, b) -> a));

        // 获取自定义范围
        List<RoleCustomScope> customScopes = customScopeRepository.findByRoleId(roleId);
        Map<String, List<Long>> customScopeMap = customScopes.stream()
                .collect(Collectors.groupingBy(
                        RoleCustomScope::getModuleCode,
                        Collectors.mapping(RoleCustomScope::getOrgUnitId, Collectors.toList())
                ));

        // 构建模块权限列表
        List<RoleDataPermissionDTO.ModulePermission> modulePermissions = new ArrayList<>();
        for (DataModule module : DataModule.values()) {
            RoleDataPermission existing = permissionMap.get(module.getCode());
            String scopeCode;
            if (existing != null && existing.getDataScope() != null) {
                scopeCode = DataScope.fromCode(existing.getDataScope()).getCode();
            } else {
                scopeCode = DataScope.SELF.getCode(); // 默认仅本人
            }

            RoleDataPermissionDTO.ModulePermission mp = new RoleDataPermissionDTO.ModulePermission();
            mp.setModuleCode(module.getCode());
            mp.setScopeCode(scopeCode);
            mp.setCustomOrgUnitIds(customScopeMap.get(module.getCode()));

            modulePermissions.add(mp);
        }

        RoleDataPermissionDTO.RolePermissionConfig config = new RoleDataPermissionDTO.RolePermissionConfig();
        config.setRoleId(roleId);
        config.setRoleName(roleName);
        config.setModulePermissions(modulePermissions);

        return config;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRolePermissionConfigV2(Long roleId, RoleDataPermissionDTO.RolePermissionConfig config) {
        log.info("V2: 保存角色数据权限配置: roleId={}", roleId);

        if (config.getModulePermissions() == null) {
            return;
        }

        // 删除原有配置
        LambdaQueryWrapper<RoleDataPermission> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(RoleDataPermission::getRoleId, roleId);
        roleDataPermissionMapper.delete(deleteWrapper);

        // 删除原有自定义范围
        customScopeRepository.deleteByRoleId(roleId);

        // 保存新配置
        for (RoleDataPermissionDTO.ModulePermission mp : config.getModulePermissions()) {
            if (mp.getScopeCode() == null) {
                continue;
            }

            DataScope scope = DataScope.fromCode(mp.getScopeCode());

            // 保存基础权限
            RoleDataPermission entity = new RoleDataPermission();
            entity.setRoleId(roleId);
            entity.setModuleCode(mp.getModuleCode());
            entity.setDataScope(scope.getIntCode());
            roleDataPermissionMapper.insert(entity);

            // 如果是自定义范围，保存组织单元列表
            if (scope == DataScope.CUSTOM && mp.getCustomOrgUnitIds() != null && !mp.getCustomOrgUnitIds().isEmpty()) {
                customScopeRepository.saveAll(roleId, mp.getModuleCode(), mp.getCustomOrgUnitIds());
            }
        }

        log.info("V2: 角色数据权限配置保存成功: roleId={}", roleId);
    }

    @Override
    public Map<String, List<Map<String, String>>> getAllModulesV2() {
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();

        // 按领域分组
        Map<String, String> domainLabels = new LinkedHashMap<>();
        domainLabels.put("organization", "组织管理");
        domainLabels.put("inspection", "量化检查");
        domainLabels.put("evaluation", "评价管理");
        domainLabels.put("task", "任务管理");

        for (String domain : domainLabels.keySet()) {
            List<Map<String, String>> modules = Arrays.stream(DataModule.values())
                    .filter(m -> m.getDomain().equals(domain))
                    .map(m -> {
                        Map<String, String> map = new LinkedHashMap<>();
                        map.put("code", m.getCode());
                        map.put("name", m.getName());
                        map.put("domain", m.getDomain());
                        return map;
                    })
                    .collect(Collectors.toList());

            if (!modules.isEmpty()) {
                result.put(domain, modules);
            }
        }

        return result;
    }

    @Override
    public List<Map<String, String>> getAllScopesV2() {
        return Arrays.stream(DataScope.values())
                .map(s -> {
                    Map<String, String> map = new LinkedHashMap<>();
                    map.put("code", s.getCode());
                    map.put("name", s.getDisplayName());
                    map.put("intCode", String.valueOf(s.getIntCode()));
                    return map;
                })
                .collect(Collectors.toList());
    }
}
