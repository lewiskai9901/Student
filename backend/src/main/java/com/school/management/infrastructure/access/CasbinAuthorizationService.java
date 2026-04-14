package com.school.management.infrastructure.access;

import com.school.management.domain.access.model.Permission;
import com.school.management.domain.access.model.Role;
import com.school.management.domain.access.model.UserRole;
import com.school.management.domain.access.repository.PermissionRepository;
import com.school.management.domain.access.repository.RoleRepository;
import com.school.management.domain.access.repository.UserRoleRepository;
import com.school.management.domain.access.service.AuthorizationService;
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

    public CasbinAuthorizationService(
            UserRoleRepository userRoleRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository) {
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
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
    @CacheEvict(value = {"userPermissions", "userRoles"}, key = "#userId")
    public void refreshCache(Long userId) {
        log.info("Refreshed permission cache for user: {}", userId);
    }

    @Override
    @CacheEvict(value = {"userPermissions", "userRoles"}, allEntries = true)
    public void refreshAllCache() {
        log.info("Refreshed all permission caches");
    }
}
