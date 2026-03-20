package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.DataScope;
import com.school.management.domain.access.model.Permission;
import com.school.management.domain.access.model.Role;
import com.school.management.domain.access.model.UserRole;
import com.school.management.domain.access.repository.PermissionRepository;
import com.school.management.domain.access.repository.RoleRepository;
import com.school.management.domain.access.repository.UserRoleRepository;
import com.school.management.domain.access.service.AuthorizationService;
import com.school.management.domain.organization.repository.OrgUnitRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Casbin-based implementation of AuthorizationService.
 * Uses Spring @Cacheable for performance with cache invalidation on changes.
 */
@Slf4j
@Service
public class CasbinAuthorizationService implements AuthorizationService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrgUnitRepository orgUnitRepository;

    public CasbinAuthorizationService(
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            OrgUnitRepository orgUnitRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.orgUnitRepository = orgUnitRepository;
    }

    @Override
    @Cacheable(value = "userPermissions", key = "#userId + ':' + #permission")
    public boolean hasPermission(Long userId, String permission) {
        Set<String> permissions = getPermissions(userId);
        return permissions.contains(permission) || permissions.contains("*");
    }

    @Override
    public boolean hasAnyPermission(Long userId, String... permissions) {
        Set<String> userPerms = getPermissions(userId);
        if (userPerms.contains("*")) {
            return true;
        }
        for (String permission : permissions) {
            if (userPerms.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasAllPermissions(Long userId, String... permissions) {
        Set<String> userPerms = getPermissions(userId);
        if (userPerms.contains("*")) {
            return true;
        }
        for (String permission : permissions) {
            if (!userPerms.contains(permission)) {
                return false;
            }
        }
        return true;
    }

    @Override
    @Cacheable(value = "userRoles", key = "#userId + ':' + #roleCode")
    public boolean hasRole(Long userId, String roleCode) {
        Set<String> roles = getRoles(userId);
        return roles.contains(roleCode);
    }

    @Override
    public boolean hasAnyRole(Long userId, String... roleCodes) {
        Set<String> userRoles = getRoles(userId);
        for (String roleCode : roleCodes) {
            if (userRoles.contains(roleCode)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Cacheable(value = "userPermissions", key = "#userId")
    public Set<String> getPermissions(Long userId) {
        // Load from database
        Set<String> permissions = new HashSet<>();

        // Get user's active roles
        List<UserRole> userRoles = userRoleRepository.findActiveByUserId(userId);
        List<Long> roleIds = userRoles.stream()
            .map(UserRole::getRoleId)
            .collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return permissions;
        }

        // Get roles with their permissions
        List<Role> roles = roleRepository.findByIds(roleIds);
        Set<Long> allPermissionIds = new HashSet<>();

        for (Role role : roles) {
            if (role.getIsEnabled()) {
                allPermissionIds.addAll(role.getPermissionIds());
            }
        }

        // Get permission codes
        if (!allPermissionIds.isEmpty()) {
            List<Permission> permissionList = permissionRepository.findByIds(new ArrayList<>(allPermissionIds));
            for (Permission perm : permissionList) {
                if (perm.getIsEnabled()) {
                    permissions.add(perm.getPermissionCode());
                }
            }
        }

        // Check for super admin role (string comparison since roleType is now free-form)
        boolean isSuperAdmin = roles.stream()
            .anyMatch(r -> "SUPER_ADMIN".equals(r.getRoleType()));
        if (isSuperAdmin) {
            permissions.add("*");
        }

        return permissions;
    }

    @Override
    @Cacheable(value = "userRoles", key = "#userId")
    public Set<String> getRoles(Long userId) {
        // Load from database
        List<UserRole> userRoles = userRoleRepository.findActiveByUserId(userId);
        List<Long> roleIds = userRoles.stream()
            .map(UserRole::getRoleId)
            .collect(Collectors.toList());

        Set<String> roleCodes = new HashSet<>();
        if (!roleIds.isEmpty()) {
            List<Role> roles = roleRepository.findByIds(roleIds);
            for (Role role : roles) {
                if (role.getIsEnabled()) {
                    roleCodes.add(role.getRoleCode());
                }
            }
        }

        return roleCodes;
    }

    @Override
    @Cacheable(value = "userDataScope", key = "#userId")
    public DataScope getDataScope(Long userId) {
        // Get user's roles and find the broadest data scope
        List<UserRole> userRoles = userRoleRepository.findActiveByUserId(userId);
        List<Long> roleIds = userRoles.stream()
            .map(UserRole::getRoleId)
            .collect(Collectors.toList());

        DataScope broadestScope = DataScope.SELF;

        if (!roleIds.isEmpty()) {
            List<Role> roles = roleRepository.findByIds(roleIds);
            for (Role role : roles) {
                if (role.getIsEnabled() && isBroaderScope(role.getDataScope(), broadestScope)) {
                    broadestScope = role.getDataScope();
                }
            }
        }

        return broadestScope;
    }

    @Override
    public List<Long> getAccessibleOrgUnitIds(Long userId) {
        DataScope scope = getDataScope(userId);

        switch (scope) {
            case ALL:
                // Return all org unit IDs
                return orgUnitRepository.findAll().stream()
                    .map(org -> org.getId())
                    .collect(Collectors.toList());

            case DEPARTMENT_AND_BELOW:
                // Get user's department and all descendants
                // This would require user's org unit from user service
                // For now, return empty - to be implemented with user context
                return Collections.emptyList();

            case DEPARTMENT:
                // Get only user's department
                return Collections.emptyList();

            case CUSTOM:
                // Get custom-defined org units
                // This would use data_permissions table
                return Collections.emptyList();

            case SELF:
            default:
                return Collections.emptyList();
        }
    }

    @Override
    public boolean canAccessOrgUnit(Long userId, Long orgUnitId) {
        DataScope scope = getDataScope(userId);

        if (scope == DataScope.ALL) {
            return true;
        }

        List<Long> accessibleIds = getAccessibleOrgUnitIds(userId);
        return accessibleIds.contains(orgUnitId);
    }

    @Override
    @CacheEvict(value = {"userPermissions", "userRoles", "userDataScope"}, key = "#userId")
    public void refreshCache(Long userId) {
        log.info("Refreshed permission cache for user: {}", userId);
    }

    @Override
    @CacheEvict(value = {"userPermissions", "userRoles", "userDataScope"}, allEntries = true)
    public void refreshAllCache() {
        log.info("Refreshed all permission caches");
    }

    /**
     * Determines if scope1 is broader than scope2.
     */
    private boolean isBroaderScope(DataScope scope1, DataScope scope2) {
        int rank1 = getScopeRank(scope1);
        int rank2 = getScopeRank(scope2);
        return rank1 > rank2;
    }

    private int getScopeRank(DataScope scope) {
        switch (scope) {
            case ALL:
                return 5;
            case DEPARTMENT_AND_BELOW:
                return 4;
            case DEPARTMENT:
                return 3;
            case CUSTOM:
                return 2;
            case SELF:
            default:
                return 1;
        }
    }
}
