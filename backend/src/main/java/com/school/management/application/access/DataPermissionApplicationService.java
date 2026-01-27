package com.school.management.application.access;

import com.school.management.domain.access.model.DataModule;
import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.Role;
import com.school.management.domain.access.repository.RoleRepository;
import com.school.management.infrastructure.persistence.access.RoleDataPermissionDomainMapper;
import com.school.management.infrastructure.persistence.access.RoleDataPermissionPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DDD Application Service for role data permission management.
 *
 * Provides data permission configuration using domain types (DataScope, DataModule).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPermissionApplicationService {

    private final RoleRepository roleRepository;
    private final RoleDataPermissionDomainMapper roleDataPermissionMapper;

    // ==================== Query Methods ====================

    /**
     * Get role data permission configuration.
     */
    @Transactional(readOnly = true)
    public RolePermissionConfig getRolePermissionConfig(Long roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));

        List<RoleDataPermissionPO> existing = roleDataPermissionMapper.selectByRoleId(roleId);
        Map<String, RoleDataPermissionPO> permissionMap = existing.stream()
                .collect(Collectors.toMap(
                        RoleDataPermissionPO::getModuleCode,
                        p -> p, (a, b) -> a));

        List<ModulePermission> modulePermissions = new ArrayList<>();
        for (DataModule module : DataModule.values()) {
            RoleDataPermissionPO perm = permissionMap.get(module.getCode());
            String scopeCode;
            if (perm != null && perm.getDataScope() != null) {
                scopeCode = DataScope.fromCode(perm.getDataScope()).getCode();
            } else {
                scopeCode = DataScope.SELF.getCode();
            }

            ModulePermission mp = new ModulePermission();
            mp.setModuleCode(module.getCode());
            mp.setScopeCode(scopeCode);
            modulePermissions.add(mp);
        }

        RolePermissionConfig config = new RolePermissionConfig();
        config.setRoleId(roleId);
        config.setRoleName(role.getRoleName());
        config.setModulePermissions(modulePermissions);
        return config;
    }

    /**
     * Save role data permission configuration.
     */
    @Transactional
    public void saveRolePermissionConfig(Long roleId, RolePermissionConfig config) {
        log.info("Saving role data permission config: roleId={}", roleId);

        if (config.getModulePermissions() == null) {
            return;
        }

        roleDataPermissionMapper.physicalDeleteByRoleId(roleId);

        for (ModulePermission mp : config.getModulePermissions()) {
            if (mp.getScopeCode() == null) {
                continue;
            }

            DataScope scope = DataScope.fromCode(mp.getScopeCode());

            RoleDataPermissionPO po = new RoleDataPermissionPO();
            po.setRoleId(roleId);
            po.setModuleCode(mp.getModuleCode());
            po.setDataScope(scope.getIntCode());
            roleDataPermissionMapper.insert(po);

            if (scope == DataScope.CUSTOM) {
                log.info("Custom scope: use Casbin API to configure: roleId={}, module={}",
                        roleId, mp.getModuleCode());
            }
        }

        log.info("Role data permission config saved: roleId={}", roleId);
    }

    /**
     * Get all data modules grouped by domain.
     */
    public Map<String, List<Map<String, String>>> getAllModules() {
        Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();

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

    /**
     * Get all data scope options.
     */
    public List<Map<String, String>> getAllScopes() {
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

    // ==================== DDD DTOs ====================

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ModulePermission {
        private String moduleCode;
        private String scopeCode;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RolePermissionConfig {
        private Long roleId;
        private String roleName;
        private List<ModulePermission> modulePermissions;
    }
}
